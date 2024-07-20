//
// Created by AXiX-official on 2024/7/19.
//

#include "../include/ABRecompressor.h"

#include "BundleFile.h"

ABRECOMPRESSOR_API AssetBundle* LoadFromBytes(const char* data, size_t size){
    AssetBundle* bundle = new AssetBundle();
    ByteArr* bData = new ByteArr((byte*)data, size);
    try {
        bundle->BundleFile = new BundleFile(bData, "");
    }
	catch (std::exception e) {
		throw std::runtime_error("Failed to LoadFromBytes:\n" + std::string(e.what()));
	}
    bundle->CompressionType = ((BundleFile*)bundle->BundleFile)->GetCompressionType();
    return bundle;
}

ABRECOMPRESSOR_API const char* CompressToBytes(AssetBundle* bundle, char compressionType, size_t* size){
    if (compressionType < 0 || compressionType > 3){
        throw std::runtime_error("Invalid compression type");
    }
    ByteArr* ret = ((BundleFile*)(bundle->BundleFile))->ToBytes((CompressionType)compressionType);
    *size = ret->GetSize();
    return (const char*)ret->GetData();
}