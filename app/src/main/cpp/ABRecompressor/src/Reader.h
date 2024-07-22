//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_READER_H
#define ABRECOMPRESSOR_READER_H

#include <stdexcept>

#include "Type.h"

class Reader{
public:
    byte* Data;
    uint64_t Size;
    uint64_t Position;

    Reader(ByteArr* data){
        Data = data->GetData();
        Size = data->GetSize();
        Position = 0;
    }

    ~Reader(){

    }

    void Align(int alignment = 16){
        Position += alignment - (Position % alignment);
    }

    byte ReadByte(){
        if (Position >= Size)
        {
            throw std::runtime_error("EOF");
        }
        return Data[Position++];
    }
    
    std::string ReadString(uint64_t maxLength = 32767){
        std::string result = "";
        for (uint64_t i = 0; i < maxLength; i++)
        {
            byte b = ReadByte();
            if (b == 0)
            {
                break;
            }
            result += b;
        }
        return result;
    }

    short ReadInt16(){
        short ret = (short)((Data[Position] << 8) | Data[Position + 1]);
        Position += 2;
        return ret;
    }

    uint ReadUInt32(){
        uint ret = (uint)((Data[Position] << 24) | (Data[Position + 1] << 16) | (Data[Position + 2] << 8) | Data[Position + 3]);
        Position += 4;
        return ret;
    }
    
    uint64_t ReadUInt64(){
        uint64_t ret = ((uint64_t)Data[Position] << 56) | ((uint64_t)Data[Position + 1] << 48) | ((uint64_t)Data[Position + 2] << 40) | ((uint64_t)Data[Position + 3] << 32) | ((uint64_t)Data[Position + 4] << 24) | ((uint64_t)Data[Position + 5] << 16) | ((uint64_t)Data[Position + 6] << 8) | (uint64_t)Data[Position + 7];
        Position += 8;
        return ret;
    }    
};

#endif //BUNDLEFILE_READER_H