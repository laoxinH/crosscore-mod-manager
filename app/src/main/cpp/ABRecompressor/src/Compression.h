//
// Created by AXiX-official on 2024/7/18.
//

#ifndef ABRECOMPRESSOR_COMPRESSION_H
#define ABRECOMPRESSOR_COMPRESSION_H

#include "Type.h"
#include "CompressionType.h"

uint Decompress(CompressionType compressionType, byte* compressedData, uint compressedSize, byte* decompressedData, uint decompressedSize);
ByteArr* Compress(CompressionType compressionType, byte* tocompressData, uint tocompressSize);

#endif // ABRECOMPRESSOR_COMPRESSION_H