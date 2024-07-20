//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_DIRECTORYINFO_H
#define ABRECOMPRESSOR_DIRECTORYINFO_H

#include "Type.h"
#include "Reader.h"
#include "Writer.h"

class DirectoryInfo{
public: 
    uint64_t offset;
    uint64_t size;
    uint flags;
    std::string path;
    
    DirectoryInfo(){
        offset = 0;
        size = 0;
        flags = 0;
        path = "";
    }

    DirectoryInfo(uint64_t offset, uint64_t size, uint flags, std::string path){
        this->offset = offset;
        this->size = size;
        this->flags = flags;
        this->path = path;
    }

    ~DirectoryInfo(){

    }

    uint64_t GetSize(){
        return 8 + 8 + 4 + path.size() + 1;
    }

    void Read(Reader* reader){
        offset = reader->ReadUInt64();
        size = reader->ReadUInt64();
        flags = reader->ReadUInt32();
        path = reader->ReadString();
    }

    void Write(Writer* writer){
        writer->WriteUInt64(offset);
        writer->WriteUInt64(size);
        writer->WriteUInt32(flags);
        writer->WriteString(path);
    }
};

#endif // ABRECOMPRESSOR_DIRECTORYINFO_H