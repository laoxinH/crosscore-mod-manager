//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_STORAGEBLOCKINFO_H
#define ABRECOMPRESSOR_STORAGEBLOCKINFO_H

#include "Type.h"
#include "Reader.h"
#include "Writer.h"

enum StorageBlockFlags{
    BlockCompressionTypeMask = 0x3f,
    Streamed = 0x40,
};

class StorageBlockInfo{
public:
    uint compressedSize;
    uint uncompressedSize;
    StorageBlockFlags flags;

    StorageBlockInfo(){
        compressedSize = 0;
        uncompressedSize = 0;
        flags = StorageBlockFlags::BlockCompressionTypeMask;
    }

    StorageBlockInfo(uint compressedSize, uint uncompressedSize, StorageBlockFlags flags){
        this->compressedSize = compressedSize;
        this->uncompressedSize = uncompressedSize;
        this->flags = flags;
    }

    ~StorageBlockInfo(){

    }

    void Read(Reader* reader){
        uncompressedSize = reader->ReadUInt32();
        compressedSize = reader->ReadUInt32();
        flags = (StorageBlockFlags)reader->ReadInt16();
    }

    void Write(Writer* writer){
        writer->WriteUInt32(uncompressedSize);
        writer->WriteUInt32(compressedSize);
        writer->WriteInt16((short)flags);
    }
};

#endif // ABRECOMPRESSOR_STORAGEBLOCKINFO_H