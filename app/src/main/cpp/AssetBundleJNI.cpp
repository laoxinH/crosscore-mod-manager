#include "AssetBundleJNI.h"
#include <stdexcept>

JNIEXPORT jlong JNICALL
Java_top_laoxin_modmanager_tools_AssetBundleJNI_loadFromBytes(JNIEnv* env, jclass clazz, jbyteArray data) {
    try {
        jbyte* nativeData = env->GetByteArrayElements(data, nullptr);
        jsize size = env->GetArrayLength(data);

        AssetBundle* bundle = LoadFromBytes(reinterpret_cast<const char*>(nativeData), static_cast<size_t>(size));

        env->ReleaseByteArrayElements(data, nativeData, JNI_ABORT);

        return reinterpret_cast<jlong>(bundle);
    } catch (const std::runtime_error& e) {
        jclass runtimeExceptionClass = env->FindClass("java/lang/RuntimeException");
        if (runtimeExceptionClass != nullptr) {
            env->ThrowNew(runtimeExceptionClass, e.what());
        }
        return 0;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_top_laoxin_modmanager_tools_assetbundle_AssetBundleJNI_compressToBytes(JNIEnv* env, jclass clazz, jlong bundlePtr, jchar compressionType) {
    try {
        AssetBundle* bundle = reinterpret_cast<AssetBundle*>(bundlePtr);

        size_t size;
        const char* compressedData = CompressToBytes(bundle, static_cast<char>(compressionType), &size);

        jbyteArray result = env->NewByteArray(size);
        env->SetByteArrayRegion(result, 0, size, reinterpret_cast<const jbyte*>(compressedData));

        return result;
    } catch (const std::runtime_error& e) {
        jclass runtimeExceptionClass = env->FindClass("java/lang/RuntimeException");
        if (runtimeExceptionClass != nullptr) {
            env->ThrowNew(runtimeExceptionClass, e.what());
        }
        return nullptr;
    }
}

JNIEXPORT jchar JNICALL
Java_top_laoxin_modmanager_tools_assetbundle_AssetBundleJNI_getCompressionType(JNIEnv* env, jclass clazz, jlong bundlePtr) {
    try {
        AssetBundle* bundle = reinterpret_cast<AssetBundle*>(bundlePtr);
        return static_cast<jchar>(bundle->CompressionType);
    } catch (const std::runtime_error& e) {
        jclass runtimeExceptionClass = env->FindClass("java/lang/RuntimeException");
        if (runtimeExceptionClass != nullptr) {
            env->ThrowNew(runtimeExceptionClass, e.what());
        }
        return 0;
    }
}
