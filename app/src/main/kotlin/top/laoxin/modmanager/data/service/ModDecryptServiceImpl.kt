package top.laoxin.modmanager.data.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.service.DecryptEvent
import top.laoxin.modmanager.domain.service.DecryptResult
import top.laoxin.modmanager.domain.service.DecryptStep
import top.laoxin.modmanager.domain.service.ModDecryptService
import kotlin.io.path.Path

/** MOD 解密服务实现 */
class ModDecryptServiceImpl @Inject constructor(private val archiveService: ArchiveService) :
        ModDecryptService {

    companion object {
        private const val TAG = "ModDecryptService"
    }

    override fun decryptMods(
            archivePath: String,
            password: String,
            mods: List<ModBean>
    ): Flow<DecryptEvent> =
            flow {
                        if (mods.isEmpty()) {
                            emit(DecryptEvent.Complete(DecryptResult(decryptedCount = 0)))
                            return@flow
                        }

                        // 1. 验证密码
                        emit(DecryptEvent.Validating(archivePath))
                        emit(
                                DecryptEvent.Progress(
                                        modName = "",
                                        current = 0,
                                        total = mods.size,
                                        step = DecryptStep.VALIDATING_PASSWORD
                                )
                        )

                        val passwordValid = validatePasswordWithFallback(archivePath, password)
                        if (!passwordValid) {
                            emit(DecryptEvent.Error(AppError.ModError.DecryptFailed("密码错误")))
                            return@flow
                        }

                        Log.d(TAG, "Password validated for $archivePath")

                        // 2. 遍历更新每个 MOD
                        val failedMods = mutableListOf<ModBean>()
                        var decryptedCount = 0
                        val total = mods.size

                        for ((index, mod) in mods.withIndex()) {
                            val current = index + 1

                            try {
                                // 2.1 提取 icon
                                emit(
                                        DecryptEvent.Progress(
                                                modName = mod.name,
                                                current = current,
                                                total = total,
                                                step = DecryptStep.EXTRACTING_ICON
                                        )
                                )

                                val extractedIcon =
                                        if (!mod.icon.isNullOrEmpty()) {
                                            extractImageToCache(
                                                    archivePath,
                                                    mod.icon,
                                                    isIcon = true,
                                                    mod.name,
                                                    password
                                            )
                                        } else null

                                // 2.2 提取 images
                                emit(
                                        DecryptEvent.Progress(
                                                modName = mod.name,
                                                current = current,
                                                total = total,
                                                step = DecryptStep.EXTRACTING_IMAGES
                                        )
                                )

                                val extractedImages =
                                        mod.images?.mapNotNull { imagePath ->
                                            extractImageToCache(
                                                    archivePath,
                                                    imagePath,
                                                    isIcon = false,
                                                    mod.name,
                                                    password
                                            )
                                        }
                                                ?: emptyList()

                                // 2.3 读取 readme 内容
                                emit(
                                        DecryptEvent.Progress(
                                                modName = mod.name,
                                                current = current,
                                                total = total,
                                                step = DecryptStep.READING_README
                                        )
                                )

                                val description =
                                        when {
                                            !mod.readmePath.isNullOrEmpty() -> {
                                                extractReadmeContent(
                                                        archivePath,
                                                        mod.readmePath,
                                                        password
                                                )
                                                        ?: "MOD已解密"
                                            }
                                            !mod.fileReadmePath.isNullOrEmpty() -> {
                                                extractReadmeContent(
                                                        archivePath,
                                                        mod.fileReadmePath,
                                                        password
                                                )
                                                        ?: "MOD已解密"
                                            }
                                            else -> "MOD已解密"
                                        }

                                // 2.4 更新 ModBean
                                val updatedMod =
                                        mod.copy(
                                               // isEncrypted = false,
                                                password = password,
                                                description = description,
                                                icon = extractedIcon ?: mod.icon,
                                                images =
                                                    extractedImages.ifEmpty { mod.images }
                                        )

                                emit(DecryptEvent.ModUpdated(updatedMod))
                                decryptedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to decrypt mod: ${mod.name}", e)
                                failedMods.add(mod)
                            }
                        }

                        // 3. 完成
                        emit(
                                DecryptEvent.Complete(
                                        DecryptResult(
                                                decryptedCount = decryptedCount,
                                                failedMods = failedMods
                                        )
                                )
                        )
                    }
                    .flowOn(Dispatchers.IO)

    /**
     * 验证密码（双重验证）
     * 1. 先尝试 validatePassword
     * 2. 失败时尝试提取文件测试
     */
    private suspend fun validatePasswordWithFallback(
            archivePath: String,
            password: String
    ): Boolean {
        // 方式1: 使用 ArchiveService.validatePassword
      /*  val validateResult = archiveService.validatePassword(archivePath, password)
        if (validateResult is Result.Success && validateResult.data) {
            return true
        }*/

        // 方式2: 提取测试文件到缓存目录
        Log.d(TAG, "validatePassword failed, trying extraction test")
        val listResult = archiveService.listFiles(archivePath)
        if (listResult is Result.Error) return false

        val files = (listResult as Result.Success).data
        // 找一个非目录的小文件进行测试
        val testFile = files.firstOrNull { !it.endsWith("/") } ?: return false
        Log.i(TAG, "Test file: $testFile")

        val cacheDir = PathConstants.MODS_UNZIP_PATH + "/decrypt_test"
        File(cacheDir).mkdirs()

        val extractResult =
                archiveService.extractSpecificFiles(
                        srcPath = archivePath,
                        files = listOf(testFile),
                        destPath = cacheDir,
                        password = password,
                        overwrite = true
                )

        if (extractResult is Result.Error) {
            Log.d(TAG, "Extraction test failed: ${extractResult.error}")
            return false
        }

        // 检查提取的文件大小
        val extractedFile = File(cacheDir, testFile)
        val valid = extractedFile.exists() && extractedFile.length() > 0

        // 清理测试文件
        File(cacheDir).deleteRecursively()

        return valid
    }

    /** 从压缩包提取图片到缓存目录（流式提取 + WebP 压缩） */
    private suspend fun extractImageToCache(
            archivePath: String,
            entryPath: String,
            isIcon: Boolean,
            modName: String,
            password: String
    ): String? {
        val cacheDir = if (isIcon) PathConstants.MODS_ICON_PATH else PathConstants.MODS_IMAGE_PATH
        val fileName = generateImageFileName(archivePath, entryPath)
        val targetPath = "$cacheDir$fileName.webp"
        val targetFile = File(targetPath)

        // 确保目录存在
        targetFile.parentFile?.mkdirs()

        // 如果已存在则直接返回
        if (targetFile.exists()) return targetPath

        // 1. 获取图片流
        val inputStreamResult = archiveService.getFileInputStream(archivePath, entryPath, password)
        if (inputStreamResult is Result.Error) {
            Log.w(TAG, "读取流失败 $entryPath: ${inputStreamResult.error}")
            return null
        }


        val inputStream = (inputStreamResult as Result.Success).data

        return try {
            inputStream.use { stream ->
                // 2. 解码图片
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap == null) {
                    Log.w(TAG, "Failed to decode image: $entryPath")
                    return@use null
                }

                // 3. 压缩为 WebP 并写入文件
                FileOutputStream(targetFile).use { fos ->
                    val compressed =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, fos)
                            } else {
                                @Suppress("DEPRECATION")
                                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, fos)
                            }

                    bitmap.recycle()

                    if (compressed) targetPath else null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Failed to extract and compress image: $entryPath", e)
            null
        }
    }

    /** 从压缩包提取 readme 文本内容 */
    private suspend fun extractReadmeContent(
            archivePath: String,
            entryPath: String,
            password: String
    ): String? {
        val inputStreamResult = archiveService.getFileInputStream(archivePath, entryPath, password)
        if (inputStreamResult is Result.Error) {
            Log.w(
                    TAG,
                    "Failed to get readme input stream for $entryPath: ${inputStreamResult.error}"
            )
            return null
        }

        val inputStream = (inputStreamResult as Result.Success).data

        return try {
            inputStream.bufferedReader().use { reader -> reader.readText().take(2000) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract readme content: $entryPath", e)
            null
        }
    }

    /**
     * 生成可还原的图片缓存文件名
     * 
     * 格式：{modName}_{base64EncodedPath}
     * 使用 URL-safe Base64 编码，可通过 decodeImageFileName 还原原始路径
     * 
     * @param modName MOD 名称
     * @param imagePath 压缩包内的相对路径
     * @return 缓存文件名（不含扩展名）
     */
    private fun generateImageFileName(archivePath: String, imagePath: String): String {
        val safeModName = File(archivePath).nameWithoutExtension +"/" + imagePath
        return safeModName
    }

    /**
     * 从缓存文件名还原压缩包内的相对路径
     * 
     * @param cacheFileName 缓存文件名（可包含 .webp 扩展名）
     * @param modName MOD 名称
     * @return 压缩包内的相对路径，解码失败返回 null
     */
    private fun decodeImageFileName(cacheFilePath: String, archivePath: String): String {
        if (!cacheFilePath.contains(PathConstants.ROOT_PATH)) return cacheFilePath
        val cacheDir = if (cacheFilePath.contains(PathConstants.MODS_ICON_PATH)) PathConstants.MODS_ICON_PATH else PathConstants.MODS_IMAGE_PATH
        val nameWithoutExt = cacheFilePath.substringBeforeLast(".")
        return nameWithoutExt.removePrefix(cacheDir + File(archivePath).nameWithoutExtension + "/")
    }

    /**
     * 刷新 MOD 详情（重新提取/扫描图片、图标和 README）
     * 
     * 压缩包 MOD：从压缩包提取图片和 README 到缓存
     * 文件夹 MOD：扫描文件夹中的图片和 README 文件
     * 
     * @param mod 需要刷新的 MOD
     * @return Result<ModBean> 刷新后的 MOD（包含更新后的图片路径和描述）
     */
    override suspend fun refreshModDetail(mod: ModBean): Result<ModBean> = withContext(Dispatchers.IO) {
        return@withContext if (mod.isZipFile) {
            refreshArchiveModDetail(mod)
        } else {
            refreshFolderModDetail(mod)
        }
    }

    /**
     * 刷新压缩包 MOD 详情
     */
    private suspend fun refreshArchiveModDetail(mod: ModBean): Result<ModBean> {
        // 跳过已加密但没有密码的 MOD（无法提取内容）
        if (mod.isEncrypted && mod.password.isEmpty()) {
            Log.d(TAG, "Skipping encrypted mod without password: ${mod.name}")
            return Result.Success(mod)
        }

        val archivePath = mod.path
        if (!File(archivePath).exists()) {
            Log.w(TAG, "Archive not found: $archivePath")
            return Result.Error(AppError.FileError.FileNotFound(archivePath))
        }

        try {
            // 1. 刷新图标（强制删除缓存后重新提取）
            val extractedIcon = if (!mod.icon.isNullOrEmpty()) {
                //val iconFileName = mod.icon
               // val iconCachePath = "${PathConstants.MODS_ICON_PATH}${iconFileName.substringBeforeLast(".")}.webp"
                File(mod.icon).delete()

                Log.d(TAG, "Extracting icon: ${decodeImageFileName(File(mod.icon).path, archivePath)}")
                extractImageToCache(
                    archivePath = archivePath,
                    entryPath = decodeImageFileName(File(mod.icon).path, archivePath),
                    isIcon = true,
                    modName = mod.name,
                    password = mod.password
                )
            } else null

            // 2. 刷新所有图片
            val extractedImages = mod.images?.mapNotNull { imagePath ->
                //val imageFileName = imagePath
                //val imageCachePath = "${PathConstants.MODS_IMAGE_PATH}${imageFileName.substringBeforeLast(".")}.webp"
                File(imagePath).delete()
                Log.d(TAG, "Extracting image: ${decodeImageFileName(File(imagePath).path, archivePath)}")

                extractImageToCache(
                    archivePath = archivePath,
                    entryPath = decodeImageFileName(File(imagePath).path, archivePath),
                    isIcon = false,
                    modName = mod.name,
                    password = mod.password
                )
            } ?: emptyList()

            // 3. 刷新 README 内容
            val description = when {
                !mod.readmePath.isNullOrEmpty() -> {
                    extractReadmeContent(archivePath, mod.readmePath, mod.password) ?: mod.description
                }
                !mod.fileReadmePath.isNullOrEmpty() -> {
                    extractReadmeContent(archivePath, mod.fileReadmePath, mod.password) ?: mod.description
                }
                else -> mod.description
            }

            val updatedMod = mod.copy(
                icon = extractedIcon ?: mod.icon,
                images = extractedImages.ifEmpty { mod.images },
                description = description,
                updateAt = System.currentTimeMillis()
            )

            Log.d(TAG, "Refreshed archive mod: ${mod.name}")
            return Result.Success(updatedMod)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh archive mod: ${mod.name}", e)
            return Result.Error(AppError.ArchiveError.ExtractFailed)
        }
    }

    /**
     * 刷新文件夹 MOD 详情
     * 
     * 扫描文件夹中的图片文件和 README 文件
     */
    private fun refreshFolderModDetail(mod: ModBean): Result<ModBean> {
        val modDir = File(mod.path)
        if (!modDir.exists() || !modDir.isDirectory) {
            Log.w(TAG, "Mod folder not found: ${mod.path}")
            return Result.Error(AppError.FileError.FileNotFound(mod.path))
        }

        try {
            // 图片扩展名列表
            val imageExtensions = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
            // README 文件名列表（不区分大小写）
            val readmeNames = listOf("readme.txt", "readme.md", "readme", "说明.txt", "介绍.txt")

            // 1. 扫描文件夹中的图片文件
            val imageFiles = modDir.listFiles { file ->
                file.isFile && imageExtensions.any { ext ->
                    file.extension.lowercase() == ext
                }
            }?.sortedBy { it.name } ?: emptyList()

            // 第一张图片作为 icon，所有图片作为 images
            val icon = imageFiles.firstOrNull()?.absolutePath
            val images = imageFiles.map { it.absolutePath }

            // 2. 扫描文件夹中的 README 文件
            val readmeFile = modDir.listFiles { file ->
                file.isFile && readmeNames.any { name ->
                    file.name.equals(name, ignoreCase = true)
                }
            }?.firstOrNull()

            // 3. 读取 README 内容并更新 readmePath
            var readmePath: String? = mod.readmePath
            var description = mod.description

            if (readmeFile != null && readmeFile.exists()) {
                // 找到 README 文件，读取内容
                try {
                    val content = readmeFile.readText().take(2000)
                    description = content
                    readmePath = readmeFile.absolutePath
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read readme file: ${readmeFile.absolutePath}", e)
                    // 读取失败，保持原值不变
                }
            } else {
                // 没有找到 README 文件，设置 readmePath 为 null
                readmePath = null
                
                // 检查 fileReadmePath 是否有效
                if (!mod.fileReadmePath.isNullOrEmpty()) {
                    val fileReadme = File(mod.fileReadmePath)
                    if (fileReadme.exists() && fileReadme.isFile) {
                        try {
                            description = fileReadme.readText().take(2000)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to read fileReadmePath: ${mod.fileReadmePath}", e)
                            // 读取失败，保持原描述不变
                        }
                    }
                }
            }

            val updatedMod = mod.copy(
                icon = icon ?: mod.icon,
                images = images.ifEmpty { mod.images },
                readmePath = readmePath,
                description = description,
                updateAt = System.currentTimeMillis()
            )

            Log.d(TAG, "Refreshed folder mod: ${mod.name}, icon: $icon, images: ${images.size}")
            return Result.Success(updatedMod)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh folder mod: ${mod.name}", e)
            return Result.Error(AppError.FileError.ReadFailed)
        }
    }
}
