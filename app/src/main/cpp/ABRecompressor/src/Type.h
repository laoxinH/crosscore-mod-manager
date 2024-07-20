//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_TYPE_H
#define ABRECOMPRESSOR_TYPE_H

#include <string>
#include <cstdint>
#include <stdexcept>

#define byte unsigned char
#define uint uint32_t

struct ByteArr{
private:
    byte* data;
    uint64_t size;
    uint64_t offset;

public:
    ByteArr(){
        data = nullptr;
        size = 0;
        offset = 0;
    }

    ByteArr(byte* data, uint64_t size, uint64_t offset = 0){
        if (offset > size){
            throw std::runtime_error("Offset is greater than size");
        }
        this->data = data;
        this->size = size;
        this->offset = offset;
    }

    ByteArr(uint64_t size){
        this->size = size;
        data = new byte[size];
        offset = 0;
    }

    ~ByteArr(){
        delete[] data;
    }

    byte* GetData(){
        return data + offset;
    }

    uint64_t GetSize(){
        return size - offset;
    }

    void SetSize(uint64_t size){
        if (size > this->GetSize()){
            throw std::runtime_error("Size is greater than available space");
        }
        this->size -= this->GetSize() - size;
    }
};

#endif