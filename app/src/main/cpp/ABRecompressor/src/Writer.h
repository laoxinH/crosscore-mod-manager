//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_WRITER_H
#define ABRECOMPRESSOR_WRITER_H

#include <stdexcept>

#include "Type.h"

class Writer{
public:
    Writer(ByteArr* data){
        Data = data->GetData();
        Size = data->GetSize();
        Position = 0;
    }

    Writer(ByteArr* data, uint64_t offset){
        Data = data->GetData();
        Size = data->GetSize();
        Position = offset;
    }

    ~Writer(){
        
    }

    void Align(int alignment = 16){
		while (Position % alignment != 0){
			WriteByte(0);
		}
    }

    void WriteByte(byte b){
        if (Position >= Size)
        {
            throw std::runtime_error("EOF");
        }
        Data[Position++] = b;
    }

    void WriteBytes(ByteArr* b){
        WriteBytes(b->GetData(), b->GetSize());
    }

    void WriteBytes(byte* b, uint64_t count){
        if (Position + count > Size)
        {
            throw std::runtime_error("EOF");
        }
        memcpy(Data + Position, b, count);
		Position += count;
    }

    void WriteString(std::string str){
        for (uint64_t i = 0; i < str.size(); i++)
        {
            WriteByte(str[i]);
        }
        WriteByte(0);
    }

    void WriteInt16(short i){
        WriteByte((byte)(i >> 8));
        WriteByte((byte)i);
    }

    void WriteUInt32(uint i){
        WriteByte((byte)(i >> 24));
        WriteByte((byte)(i >> 16));
        WriteByte((byte)(i >> 8));
        WriteByte((byte)i);
    }

    void WriteUInt64(uint64_t i){
        WriteByte((byte)(i >> 56));
        WriteByte((byte)(i >> 48));
        WriteByte((byte)(i >> 40));
        WriteByte((byte)(i >> 32));
        WriteByte((byte)(i >> 24));
        WriteByte((byte)(i >> 16));
        WriteByte((byte)(i >> 8));
        WriteByte((byte)i);
    }

private:
    byte* Data;
    uint64_t Size;
    uint64_t Position;
};







#endif // ABRECOMPRESSOR_WRITER_H