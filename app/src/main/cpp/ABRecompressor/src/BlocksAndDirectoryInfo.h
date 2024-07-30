//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_BLOCKSANDDIRECTORYINFO_H
#define ABRECOMPRESSOR_BLOCKSANDDIRECTORYINFO_H

#include "Type.h"
#include "Reader.h"
#include "Writer.h"
#include "Header.h"
#include "StorageBlockInfo.h"
#include "DirectoryInfo.h"
#include "CompressionType.h"

class BlocksAndDirectoryInfo{
public:
    ByteArr* UncompressedDataHash;
    uint BlocksInfoCount;
    StorageBlockInfo* BlocksInfos;
    uint DirectoryInfoCount;
    DirectoryInfo* DirectoryInfos;

    BlocksAndDirectoryInfo(Reader* reader, Header* header, bool* blocksInfoAtTheEnd);
    ~BlocksAndDirectoryInfo();

    ByteArr* ToBytes();
    ByteArr* ToBytes(CompressionType compressionType);
    uint64_t DataSize();
    uint64_t GetRawSize();
};

#endif // ABRECOMPRESSOR_BLOCKSANDDIRECTORYINFO_H