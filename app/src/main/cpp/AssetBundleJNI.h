#include <jni.h>
#include "ABRecompressor/include/ABRecompressor.h"

extern "C" {
    JNIEXPORT jlong JNICALL
        Java_top_laoxin_modmanager_tools_AssetBundleJNI_loadFromBytes(JNIEnv* env, jclass clazz, jbyteArray data);

    JNIEXPORT jbyteArray JNICALL
        Java_top_laoxin_modmanager_tools_assetbundle_AssetBundleJNI_compressToBytes(JNIEnv* env, jclass clazz, jlong bundlePtr, jchar compressionType);

    JNIEXPORT jchar JNICALL
        Java_top_laoxin_modmanager_tools_assetbundle_AssetBundleJNI_getCompressionType(JNIEnv* env, jclass clazz, jlong bundlePtr);
}