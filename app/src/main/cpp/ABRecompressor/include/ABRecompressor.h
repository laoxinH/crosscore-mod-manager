//
// Created by AXiX-official on 2024/7/19.
//

#include <cstddef>

#ifndef ABRECOMPRESSOR_H
#define ABRECOMPRESSOR_H

#if defined (__cplusplus)
extern "C" {
#endif

#ifdef _WIN32
    #ifdef ABRECOMPRESSOR_EXPORTS
        #define ABRECOMPRESSOR_API __declspec(dllexport)
    #else
        #define ABRECOMPRESSOR_API __declspec(dllimport)
    #endif
#else
    #define ABRECOMPRESSOR_API
#endif

#define AB_COMPRESSION_NONE 0
#define AB_COMPRESSION_LZMA 1
#define AB_COMPRESSION_LZ4 2
#define AB_COMPRESSION_LZ4HC 3

struct ABRECOMPRESSOR_API AssetBundle
{
    void* BundleFile;
    char CompressionType;
};

ABRECOMPRESSOR_API AssetBundle* LoadFromBytes(const char* data, size_t size) noexcept(false);
ABRECOMPRESSOR_API const char* CompressToBytes(AssetBundle* bundle, char compressionType, size_t* size) noexcept(false);

#endif // ABRECOMPRESSOR_H

#if defined (__cplusplus)
}
#endif