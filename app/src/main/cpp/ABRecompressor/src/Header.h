//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_HEADER_H
#define ABRECOMPRESSOR_HEADER_H

#include <stdexcept>

#include "Reader.h"
#include "Writer.h"

enum ArchiveFlags{
    CompressionTypeMask = 0x3f,
    BlocksAndDirectoryInfoCombined = 0x40,
    BlocksInfoAtTheEnd = 0x80,
    OldWebPluginCompatibility = 0x100,
    BlockInfoNeedPaddingAtStart = 0x200,
    UnityCNEncryption = 0x400
};

class Header{
public:
    Header(Reader* reader);
    ~Header();

    uint64_t GetSize();
    void Write(Writer* writer);

    int* ParseVersion();

    std::string mSignature;
    uint mVersion;
    std::string mUnityVersion;
    std::string mUnityRevision;
    uint64_t mSize;
    uint mCompressedBlocksInfoSize;
    uint mUncompressedBlocksInfoSize;
    ArchiveFlags mFlags;
};

#endif // ABRECOMPRESSOR_HEADER_H