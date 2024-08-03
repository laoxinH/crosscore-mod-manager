//
// Created by AXiX-official on 2024/7/17.
//

#include "BundleFile.h"
#include "Compression.h"

BundleFile::BundleFile(ByteArr* data, std::string key = ""){
    auto reader = Reader(data);

    mUnityCNKey = key;

    mHeader = new Header(&reader);
    try {
        ReadBundleWithHeader(&reader);
	}
    catch (std::exception e) {
		throw std::runtime_error("Failed to read bundle with header:\n" + std::string(e.what()));
    }
}

BundleFile::~BundleFile(){
    delete mHeader;
}

void BundleFile::ReadBundleWithHeader(Reader* reader){
    auto version = mHeader->ParseVersion();
    if (version[0] < 2020 || //2020 and earlier
        (version[0] == 2020 && version[1] == 3 && version[2] <= 34) || //2020.3.34 and earlier
        (version[0] == 2021 && version[1] == 3 && version[2] <= 2) || //2021.3.2 and earlier
        (version[0] == 2022 && version[1] == 3 && version[2] <= 1)){ //2022.3.1 and earlier
        mask = ArchiveFlags::BlockInfoNeedPaddingAtStart;
        HasBlockInfoNeedPaddingAtStart = false;
    }else{
        mask = ArchiveFlags::UnityCNEncryption;
        HasBlockInfoNeedPaddingAtStart = true;
    }

    if ((mHeader->mFlags & mask) != 0){
        if (mUnityCNKey.size() != 32)
        {
            throw std::runtime_error("File is encrypted but no valid key was provided");
        }
        //UnityCNInfo = new UnityCN(reader, UnityCNKey);
        //Header.flags &= (ArchiveFlags)~mask;
    }

    HeaderAligned = false;
    if (mHeader->mVersion >= 7){
        reader->Align();
        HeaderAligned = true;
    }else if (version[0] == 2019 && version[1] == 4){ // temp fix for 2019.4.x
        HeaderAligned = true;
        uint64_t pos = reader->Position;
        uint64_t len = 16 - pos % 16;
        for (uint64_t i = 0; i < len; i++)
        {
            byte b = reader->ReadByte();
            if (b != 0)
            {
                reader->Position = pos;
                HeaderAligned = false;
                break;
            }
        }
    }

    delete version;

    try {
        mDataInfo = new BlocksAndDirectoryInfo(reader, mHeader, &BlocksInfoAtTheEnd);
    } catch (std::exception e) {
		throw std::runtime_error("Failed to read blocks and directory info:\n" + std::string(e.what()));
	}

    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        mDataInfo->BlocksInfos[i].flags = (StorageBlockFlags)(mDataInfo->BlocksInfos[i].flags & ~0x100);
    }

    if (HasBlockInfoNeedPaddingAtStart && (mHeader->mFlags & ArchiveFlags::BlockInfoNeedPaddingAtStart) != 0){
        reader->Align();
    }

    ReadBlocks(reader);

    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        mDataInfo->BlocksInfos[i].flags = (StorageBlockFlags)(mDataInfo->BlocksInfos[i].flags & 0x0);
    }
    mHeader->mFlags = (ArchiveFlags)(mHeader->mFlags & ~StorageBlockFlags::BlockCompressionTypeMask);

}

void BundleFile::ReadBlocks(Reader* reader){
    data = new ByteArr(mDataInfo->DataSize());
    byte* p = data->GetData();
    mCompressionType = 0;
    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        CompressionType compressionType = (CompressionType)(mDataInfo->BlocksInfos[i].flags & StorageBlockFlags::BlockCompressionTypeMask);
        if ((char)compressionType > mCompressionType){
            mCompressionType = (char)compressionType;
        }
        try {
            Decompress(compressionType, reader->Data + reader->Position, mDataInfo->BlocksInfos[i].compressedSize, p, mDataInfo->BlocksInfos[i].uncompressedSize);
        }
        catch (std::exception e) {
            throw std::runtime_error("Failed to decompress blocks:\n" + std::string(e.what()));
        }
        reader->Position += mDataInfo->BlocksInfos[i].compressedSize;
        p += mDataInfo->BlocksInfos[i].uncompressedSize;
    }
}

char BundleFile::GetCompressionType(){
    return mCompressionType;
}

ByteArr* BundleFile::ToBytes(CompressionType compressionType){
    if (compressionType == CompressionType::Lzma){
        uint64_t totalSize = data->GetSize();
        mDataInfo->BlocksInfoCount = (totalSize / UINT32_MAX) + ((totalSize % UINT32_MAX) ? 1 : 0);
        for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
            if (i == mDataInfo->BlocksInfoCount - 1){
                mDataInfo->BlocksInfos[i].uncompressedSize = totalSize % UINT32_MAX;
            }else{
                mDataInfo->BlocksInfos[i].uncompressedSize = UINT32_MAX;
            }
        }    
    }

    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        mDataInfo->BlocksInfos[i].flags = (StorageBlockFlags)(mDataInfo->BlocksInfos[i].flags | compressionType);
    }
    ByteArr** compressedData = new ByteArr*[mDataInfo->BlocksInfoCount];
    uint64_t p = 0;
    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        compressedData[i] = Compress(compressionType, data->GetData() + p, mDataInfo->BlocksInfos[i].uncompressedSize);
        mDataInfo->BlocksInfos[i].compressedSize = compressedData[i]->GetSize();
        p += mDataInfo->BlocksInfos[i].uncompressedSize;
    }
    ByteArr* bDataInfo = mDataInfo->ToBytes(CompressionType::Lz4HC);
    mHeader->mUncompressedBlocksInfoSize = mDataInfo->GetRawSize();
    mHeader->mCompressedBlocksInfoSize = bDataInfo->GetSize();
    uint64_t size = mHeader->GetSize();
    if (HeaderAligned){
        size += (16 - (size % 16) % 16);
    }
    size += bDataInfo->GetSize();
    if (HasBlockInfoNeedPaddingAtStart && (mHeader->mFlags & ArchiveFlags::BlockInfoNeedPaddingAtStart) != 0){
        size += (16 - (size % 16) % 16);
    }
    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        size += mDataInfo->BlocksInfos[i].compressedSize;
    }

    ByteArr* result = new ByteArr(size);
    Writer writer(result);
    mHeader->mSize = size;
    mHeader->mFlags = (ArchiveFlags)(mHeader->mFlags | CompressionType::Lz4HC);
    mHeader->Write(&writer);
    if (HeaderAligned) {
        writer.Align();
    }
    if (!BlocksInfoAtTheEnd){
        writer.WriteBytes(bDataInfo);
    }
    if (HasBlockInfoNeedPaddingAtStart && (mHeader->mFlags & ArchiveFlags::BlockInfoNeedPaddingAtStart) != 0) {
        writer.Align();
    }
    for (int i = 0; i < mDataInfo->BlocksInfoCount; i++){
        writer.WriteBytes(compressedData[i]);
        delete compressedData[i];
    }
    if (BlocksInfoAtTheEnd){
        writer.WriteBytes(bDataInfo);
    }
    return result;
}