package top.laoxin.modmanager.ui.view.modView

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// 图片缓存管理器
object ImageCacheManager {
    private const val MAX_CACHE_SIZE = 50 * 1024 * 1024 // 50MB

    private val memoryCache = object : LruCache<String, ImageBitmap>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return value.width * value.height * 4
        }
    }

    fun get(key: String): ImageBitmap? = memoryCache[key]

    fun put(key: String, bitmap: ImageBitmap) {
        memoryCache.put(key, bitmap)
    }

}

// 异步加载图片，使用缓存
suspend fun loadImageBitmapWithCache(
    context: Context,
    path: String,
    reqWidth: Int = 256,
    reqHeight: Int = 256
): ImageBitmap? {
    // 先检查缓存
    val cacheKey = "$path-$reqWidth-$reqHeight"
    ImageCacheManager.get(cacheKey)?.let { return it }

    return withContext(Dispatchers.IO) {
        try {
            val bitmap: Bitmap = Glide.with(context)
                .asBitmap()
                .load(path)
                .apply(
                    RequestOptions()
                        .override(reqWidth, reqHeight)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                )
                .submit()
                .get()

            val imageBitmap = bitmap.asImageBitmap()
            ImageCacheManager.put(cacheKey, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            Log.e("ImageCache", "加载图片失败: $path", e)
            null
        }
    }
}

// Compose 友好的图片加载器
// key 参数用于强制刷新，例如传入 mod.updateAt 在刷新后重新加载图片
@Composable
fun rememberImageBitmap(
    path: String?,
    reqWidth: Int = 256,
    reqHeight: Int = 256,
    key: Any? = null
): State<ImageBitmap?> {
    val context = LocalContext.current

    return produceState(initialValue = null, path, reqWidth, reqHeight, key) {
        if (path.isNullOrEmpty()) {
            value = null
            return@produceState
        }

        if (!File(path).exists()) {
            Log.d("ImageCache", "图片文件不存在: $path")
            value = null
            return@produceState
        }

        // 缓存 key 包含刷新 key，确保刷新后使用新缓存
        val cacheKey = "$path-$reqWidth-$reqHeight-${key ?: ""}"
        val cached = ImageCacheManager.get(cacheKey)
        if (cached != null) {
            Log.d("ImageCache", "从缓存加载图片: $path")
            value = cached
            return@produceState
        }

        // 异步加载
        Log.d("ImageCache", "开始加载图片: $path")
        val bitmap = loadImageBitmapWithCache(context, path, reqWidth, reqHeight)
        if (bitmap != null) {
            // 使用新的 cacheKey 存储
            ImageCacheManager.put(cacheKey, bitmap)
            Log.d("ImageCache", "图片加载成功: $path")
        }
        value = bitmap
    }
}

