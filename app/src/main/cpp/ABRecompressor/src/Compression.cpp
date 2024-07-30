//
// Created by AXiX-official on 2024/7/18.
//

#include "Compression.h"
#include "lz4.h"
#include "lz4hc.h"
#include "lzma.h"
#include <stdexcept>

uint Decompress(CompressionType compressionType, byte* compressedData, uint compressedSize, byte* decompressedData, uint decompressedSize){
    switch (compressionType){
    case CompressionType::None:{
        if (compressedSize != decompressedSize){
            throw std::runtime_error("Decompressed size does not match the expected size, expected " + std::to_string(decompressedSize) + " but got " + std::to_string(compressedSize));
        }   
        memcpy(decompressedData, compressedData, compressedSize);
        return compressedSize;
        break;
    }
    case CompressionType::Lzma:{
        lzma_stream strm = LZMA_STREAM_INIT;
        lzma_ret ret = lzma_alone_decoder(&strm, UINT64_MAX);
        if (ret != LZMA_OK) {
            throw std::runtime_error("lzma_stream_buffer_decode error: " + std::to_string(static_cast<int>(ret)));
        }
        
        byte* fixedData = new byte[compressedSize + 8];
        byte* sizeLE = new byte[8];
        for (int i = 0; i < 4; i++) {
            sizeLE[i] = (decompressedSize >> (i * 8)) & 0xFF;
            sizeLE[i + 4] = 0;
        }
        memcpy(fixedData, compressedData, 5);
        memcpy(fixedData + 5, sizeLE, 8);
        memcpy(fixedData + 13, compressedData + 5, compressedSize - 5);
        strm.next_in = fixedData;
        strm.avail_in = compressedSize + 8;
        strm.next_out = decompressedData;
        strm.avail_out = decompressedSize;

        ret = lzma_code(&strm, LZMA_RUN);
        if (ret != LZMA_STREAM_END) {
            lzma_end(&strm);
            delete[] fixedData;
            delete[] sizeLE;
            throw std::runtime_error("Lzma decompression failed: " + std::to_string(ret));
        }

        lzma_end(&strm);
        delete[] fixedData;
        delete[] sizeLE;
        return strm.total_out;
        break;
    }
    case CompressionType::Lz4:
    case CompressionType::Lz4HC:{
        int size = LZ4_decompress_safe((const char*)compressedData, (char*)decompressedData, compressedSize, decompressedSize);
        if (size != decompressedSize){
            throw std::runtime_error("Decompressed size does not match the expected size, expected " + std::to_string(decompressedSize) + " but got " + std::to_string(size));
        }
        return size;
        break;  
    }
    default:
        throw std::runtime_error("Unknown compression type");
        break;
    }
};

ByteArr* Compress(CompressionType compressionType, byte* tocompressData, uint tocompressSize){
    switch (compressionType){
    case CompressionType::None:{
		ByteArr* result = new ByteArr(tocompressSize);
		memcpy(result->GetData(), tocompressData, tocompressSize);
        return result;
    }
    case CompressionType::Lzma:{
        lzma_stream strm = LZMA_STREAM_INIT;
        lzma_ret ret;
        lzma_options_lzma opt;
        opt.dict_size = 0x200000;
        lzma_lzma_preset(&opt, LZMA_PRESET_EXTREME);
        ret = lzma_alone_encoder(&strm, &opt);
        if (ret != LZMA_OK) {
            throw std::runtime_error("lzma_stream_buffer_decode error: " + std::to_string(ret));
        }
        
        strm.next_in = tocompressData;
        strm.avail_in = tocompressSize;
        size_t out_size = tocompressSize + tocompressSize / 3 + 128;
        uint8_t* out_buf = new uint8_t[out_size];
        strm.next_out = out_buf;
        strm.avail_out = out_size;

        ret = lzma_code(&strm, LZMA_FINISH);
        if (ret != LZMA_STREAM_END){
            delete[] out_buf;
            lzma_end(&strm);
            throw std::runtime_error("Lzma decompression failed: " + std::to_string(ret));
        }
        lzma_end(&strm);
        memcpy(out_buf + 8, out_buf, 5);
        ByteArr* result = new ByteArr(out_buf, strm.total_out, 8);
        return result;
        break;
    }
        break;
    case CompressionType::Lz4:{
        int size = LZ4_compressBound(tocompressSize);
        ByteArr* result = new ByteArr(size);
        result->SetSize(LZ4_compress_default((const char*)tocompressData, (char*)result->GetData(), tocompressSize, size));
        return result;
        break;
    }
    case CompressionType::Lz4HC:{
        int size = LZ4_compressBound(tocompressSize);
        ByteArr* result = new ByteArr(size);
        result->SetSize(LZ4_compress_HC((const char*)tocompressData, (char*)result->GetData(), tocompressSize, size, 12));
        return result;
        break;  
    }
    default:
        throw std::runtime_error("Unknown compression type");
        break;
    }
};