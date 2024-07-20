//
// Created by AXiX-official on 2024/7/17.
//

#include "Header.h"

Header::Header(Reader* reader){
    mSignature = reader->ReadString(20);
    if (mSignature != "UnityFS"){
        throw std::runtime_error("Invalid signature, expected UnityFS but got " + mSignature);
    }
    mVersion = reader->ReadUInt32();
    mUnityVersion = reader->ReadString(16);
    mUnityRevision = reader->ReadString(20);
    mSize = reader->ReadUInt64();
    mCompressedBlocksInfoSize = reader->ReadUInt32();
    mUncompressedBlocksInfoSize = reader->ReadUInt32();
    mFlags = (ArchiveFlags)reader->ReadUInt32();
}

Header::~Header(){
    
}

uint64_t Header::GetSize(){
    return mSignature.size() + 1 + 4 + mUnityVersion.size() + 1 + mUnityRevision.size() + 1 + 8 + 4 + 4 + 4;
}

void Header::Write(Writer* writer){
    writer->WriteString(mSignature);
    writer->WriteUInt32(mVersion);
    writer->WriteString(mUnityVersion);
    writer->WriteString(mUnityRevision);
    writer->WriteUInt64(mSize);
    writer->WriteUInt32(mCompressedBlocksInfoSize);
    writer->WriteUInt32(mUncompressedBlocksInfoSize);
    writer->WriteUInt32((uint)mFlags);
}

int* Header::ParseVersion(){
    int* result = new int[3]();
    int i = 0;
    for (int j = 0; j < mUnityRevision.size() && i < 3; j++)
    {
        if (mUnityRevision[j] < '0' || mUnityRevision[j] > '9')
        {
            i++;
            continue;
        }
        result[i] = result[i] * 10 + (mUnityRevision[j] - '0');
    }
    return result;
}