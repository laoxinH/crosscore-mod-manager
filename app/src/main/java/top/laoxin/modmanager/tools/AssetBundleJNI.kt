package top.laoxin.modmanager.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JNI interface for interacting with native AssetBundle library.
 */
object AssetBundleJNI {
    init {
        System.loadLibrary("AssetBundleJNI")
    }

    // Compression type constants
    const val AB_COMPRESSION_NONE: Char = 0.toChar()
    const val AB_COMPRESSION_LZMA: Char = 1.toChar()
    const val AB_COMPRESSION_LZ4: Char = 2.toChar()
    const val AB_COMPRESSION_LZ4HC: Char = 3.toChar()

    @Throws(RuntimeException::class)
    private external fun loadFromBytes(data: ByteArray?): Long

    @Throws(RuntimeException::class)
    private external fun compressToBytes(bundlePtr: Long, compressionType: Char): ByteArray?

    /**
     * Gets the compression type of the AssetBundle.
     *
     * @param bundlePtr Pointer to the native AssetBundle object.
     * @return The compression type of the AssetBundle, which can be one of the following:
     * - [AssetBundleJNI.AB_COMPRESSION_NONE]
     * - [AssetBundleJNI.AB_COMPRESSION_LZMA]
     * - [AssetBundleJNI.AB_COMPRESSION_LZ4]
     * - [AssetBundleJNI.AB_COMPRESSION_LZ4HC]
     * @throws RuntimeException if there is an error during retrieval.
     */
    @Throws(RuntimeException::class)
    external fun getCompressionType(bundlePtr: Long): Char

    /**
     * Asynchronously loads an AssetBundle from byte array data.
     *
     * @param data The byte array containing the AssetBundle data.
     * @return A pointer to the native AssetBundle object.
     * @throws RuntimeException if there is an error during loading.
     */
    suspend fun loadFromBytesAsync(data: ByteArray?): Long = withContext(Dispatchers.IO) {
        loadFromBytes(data)
    }

    /**
     * Asynchronously compresses an AssetBundle to a byte array.
     *
     * @param bundlePtr Pointer to the native AssetBundle object.
     * @param compressionType The compression type to use, which can be one of the following:
     * - [AssetBundleJNI.AB_COMPRESSION_NONE]
     * - [AssetBundleJNI.AB_COMPRESSION_LZMA]
     * - [AssetBundleJNI.AB_COMPRESSION_LZ4]
     * - [AssetBundleJNI.AB_COMPRESSION_LZ4HC]
     * @return A byte array containing the compressed AssetBundle data.
     * @throws RuntimeException if there is an error during compression.
     */
    suspend fun compressToBytesAsync(bundlePtr: Long, compressionType: Char): ByteArray? = withContext(Dispatchers.IO) {
        compressToBytes(bundlePtr, compressionType)
    }
}