//
// Created by AXiX-official on 2024/7/17.
//

#include "BlocksAndDirectoryInfo.h"
#include "Compression.h"

BlocksAndDirectoryInfo::BlocksAndDirectoryInfo(Reader* reader, Header* header, bool* blocksInfoAtTheEnd){
    byte* BlocksInfoBytes;
    if ((header->mFlags & ArchiveFlags::BlocksInfoAtTheEnd) != 0){ //kArchiveBlocksInfoAtTheEnd
        *blocksInfoAtTheEnd = true;
        BlocksInfoBytes = reader->Data + header->mSize - header->mCompressedBlocksInfoSize;
    }else{ //0x40 BlocksAndDirectoryInfoCombined
        *blocksInfoAtTheEnd = false;
        BlocksInfoBytes = reader->Data + reader->Position;
        reader->Position += header->mCompressedBlocksInfoSize;
    }

    ByteArr* DecompressedBlocksInfo = new ByteArr(header->mUncompressedBlocksInfoSize);
    CompressionType compressionType = (CompressionType)(header->mFlags & ArchiveFlags::CompressionTypeMask);
    try {
        Decompress(compressionType, BlocksInfoBytes, header->mCompressedBlocksInfoSize, DecompressedBlocksInfo->GetData(), DecompressedBlocksInfo->GetSize());
	}
	catch (std::exception e) {
		throw std::runtime_error("Failed to decompress blocks info:\n" + std::string(e.what()));
	}
	
    UncompressedDataHash = new ByteArr(16);
    memcpy(UncompressedDataHash->GetData(), DecompressedBlocksInfo->GetData(), 16);
    
    Reader nreader = Reader(DecompressedBlocksInfo);
    nreader.Position = 16;
    BlocksInfoCount = nreader.ReadUInt32();
    BlocksInfos = new StorageBlockInfo[BlocksInfoCount];
    for (uint i = 0; i < BlocksInfoCount; i++)
    {
        BlocksInfos[i].Read(&nreader);
    }
    DirectoryInfoCount = nreader.ReadUInt32();
    DirectoryInfos = new DirectoryInfo[DirectoryInfoCount];
    for (uint i = 0; i < DirectoryInfoCount; i++)
    {
        DirectoryInfos[i].Read(&nreader);
    }
}

ByteArr* BlocksAndDirectoryInfo::ToBytes(){
    ByteArr* result = new ByteArr(GetRawSize());
    memcpy(result->GetData(), UncompressedDataHash->GetData(), 16);
    Writer writer = Writer(result, 16);
    writer.WriteUInt32(BlocksInfoCount);
    for (uint i = 0; i < BlocksInfoCount; i++){
        BlocksInfos[i].Write(&writer);
    }
    writer.WriteUInt32(DirectoryInfoCount);
    for (uint i = 0; i < DirectoryInfoCount; i++){
        DirectoryInfos[i].Write(&writer);
    }
    return result;
}

ByteArr* BlocksAndDirectoryInfo::ToBytes(CompressionType compressionType){
    switch (compressionType){
    case CompressionType::None:{
        return ToBytes();
        break;
    }  
    case CompressionType::Lz4:
    case CompressionType::Lz4HC:
    case CompressionType::Lzma:{
        ByteArr* raw = ToBytes();
        return Compress(compressionType, raw->GetData(), raw->GetSize());
        break;
    } 
    default:
        throw std::runtime_error("Unexpected CompressionType: " + std::to_string(compressionType));
        break;
    }
}

uint64_t BlocksAndDirectoryInfo::DataSize(){
    uint64_t size1 = 0;
    uint64_t size2 = 0;
    for (uint i = 0; i < BlocksInfoCount; i++){
        size1 += BlocksInfos[i].uncompressedSize;
    }
    for (uint i = 0; i < DirectoryInfoCount; i++){
        size2 += DirectoryInfos[i].size;
    }
    if (size1 == size2){
        return size1;
    }
    throw std::runtime_error("Data size mismatch, for blocks is " + std::to_string(size1) + " while for cab is " + std::to_string(size2));
}

uint64_t BlocksAndDirectoryInfo::GetRawSize(){
    uint64_t size = 16 + 4 + 4 + 10 * BlocksInfoCount;
    for (uint i = 0; i < DirectoryInfoCount; i++){
        size += DirectoryInfos[i].GetSize();
    }
    return size;
}