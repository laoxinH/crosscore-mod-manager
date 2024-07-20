//
// Created by AXiX-official on 2024/7/17.
//

#ifndef ABRECOMPRESSOR_BUNDLEFILE_H
#define ABRECOMPRESSOR_BUNDLEFILE_H

#include "Type.h"
#include "Reader.h"
#include "Writer.h"
#include "Header.h"
#include "BlocksAndDirectoryInfo.h"

class BundleFile{
public:
    BundleFile(ByteArr* data, std::string key);
    ~BundleFile();

    ByteArr* ToBytes(CompressionType compressionType);

    char GetCompressionType();

private:
    Header* mHeader;
    std::string mUnityCNKey;
    bool HasBlockInfoNeedPaddingAtStart;
    ArchiveFlags mask;
    bool HeaderAligned;
    bool BlocksInfoAtTheEnd;
    BlocksAndDirectoryInfo* mDataInfo ;
    ByteArr* data;
    char mCompressionType;

    void ReadBundleWithHeader(Reader* reader);
    void ReadBlocks(Reader* reader);
};

#endif // ABRECOMPRESSOR_BUNDLEFILE_H