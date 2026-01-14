package top.laoxin.modmanager.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import top.laoxin.modmanager.App
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.data.service.filetools.FileToolsManager
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.PasswordErrorException

/** ArchiveService 实现 封装 ArchiveUtil，提供统一的错误处理 支持 Shizuku 权限：当文件需要 Shizuku 权限时，先复制到临时目录再操作 */
@Singleton
class ArchiveServiceImpl @Inject constructor(
    private val fileToolsManager: FileToolsManager,
    @param:ApplicationContext private val context: Context,
    ) :
    ArchiveService {

    private val tempDir: File
        get() = File(PathConstants.MODS_TEMP_PATH, "temp_archive").also { if (!it.exists()) it.mkdirs() }

    override suspend fun extract(
        srcPath: String,
        destPath: String,
        password: String?,
        overwrite: Boolean,
        onProgress: (Int) -> Unit
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            executeWithShizukuSupport(srcPath) { actualPath ->
                try {
                    val success =
                        ArchiveUtil.decompression(
                            srcFile = actualPath,
                            destDir = destPath,
                            password = password,
                            overwrite = overwrite,
                            onProgress = onProgress
                        )
                    if (success) {
                        Result.Success(Unit)
                    } else {
                        Result.Error(AppError.ArchiveError.ExtractFailed)
                    }
                } catch (e: PasswordErrorException) {
                    Result.Error(AppError.ArchiveError.WrongPassword)
                } catch (e: FileNotFoundException) {
                    Result.Error(AppError.FileError.FileNotFound(srcPath))
                } catch (e: SecurityException) {
                    Result.Error(AppError.FileError.PermissionDenied)
                } catch (e: Exception) {
                    when {
                        e.message?.contains("password", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.WrongPassword)

                        e.message?.contains("encrypted", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.EncryptedNeedPassword)

                        e.message?.contains("corrupt", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.CorruptedArchive)

                        else -> Result.Error(AppError.ArchiveError.Unknown(e.message ?: "解压失败"))
                    }
                }
            }
        }

    override suspend fun extractSpecificFiles(
        srcPath: String,
        files: List<String>,
        destPath: String,
        password: String?,
        overwrite: Boolean,
        onProgress: (Int) -> Unit
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            executeWithShizukuSupport(srcPath) { actualPath ->
                try {
                    val success =
                        ArchiveUtil.extractSpecificFile(
                            srcFile = actualPath,
                            files = files,
                            destDir = destPath,
                            password = password,
                            overwrite = overwrite,
                            onProgress = onProgress
                        )
                    if (success) {
                        Result.Success(Unit)
                    } else {
                        Result.Error(AppError.ArchiveError.ExtractFailed)
                    }
                } catch (e: PasswordErrorException) {
                    Result.Error(AppError.ArchiveError.WrongPassword)
                } catch (e: FileNotFoundException) {
                    Result.Error(AppError.FileError.FileNotFound(srcPath))
                } catch (e: SecurityException) {
                    Result.Error(AppError.FileError.PermissionDenied)
                } catch (e: Exception) {
                    when {
                        e.message?.contains("password", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.WrongPassword)

                        e.message?.contains("encrypted", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.EncryptedNeedPassword)

                        else ->
                            Result.Error(
                                AppError.ArchiveError.Unknown(e.message ?: "解压指定文件失败")
                            )
                    }
                }
            }
        }

    override suspend fun listFiles(archivePath: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            executeWithShizukuSupport(archivePath) { actualPath ->
                try {
                    val files = ArchiveUtil.listInArchiveFiles(actualPath)
                    if (files.isEmpty()) {
                        Result.Error(AppError.ArchiveError.EmptyArchive)
                    } else {
                        Result.Success(files)
                    }
                } catch (e: FileNotFoundException) {
                    Result.Error(AppError.FileError.FileNotFound(archivePath))
                } catch (e: SecurityException) {
                    Result.Error(AppError.FileError.PermissionDenied)
                } catch (e: Exception) {
                    when {
                        e.message?.contains("corrupt", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.CorruptedArchive)

                        else -> Result.Error(AppError.ArchiveError.ReadContentFailed)
                    }
                }
            }
        }

    override suspend fun getFileInputStream(
        archivePath: String,
        itemName: String,
        password: String?
    ): Result<InputStream> =
        withContext(Dispatchers.IO) {
            // 注意：对于 InputStream，Shizuku 模式下临时文件不能立即删除
            // 需要调用者自行管理或使用包装流
            executeWithShizukuSupportKeepTemp(archivePath) { actualPath ->
                try {
                    val inputStream =
                        ArchiveUtil.getArchiveItemInputStream(
                            archivePath = actualPath,
                            itemName = itemName,
                            password = password
                        )
                    if (inputStream != null) {
                        Result.Success(inputStream)
                    } else {
                        Result.Error(AppError.ArchiveError.ItemNotFound(itemName))
                    }
                } catch (e: PasswordErrorException) {
                    Result.Error(AppError.ArchiveError.WrongPassword)
                } catch (e: FileNotFoundException) {
                    Result.Error(AppError.FileError.FileNotFound(archivePath))
                } catch (e: SecurityException) {
                    Result.Error(AppError.FileError.PermissionDenied)
                } catch (e: Exception) {
                    when {
                        e.message?.contains("password", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.WrongPassword)

                        e.message?.contains("not found", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.ItemNotFound(itemName))

                        else ->
                            Result.Error(
                                AppError.ArchiveError.Unknown(e.message ?: "读取压缩包文件流失败")
                            )
                    }
                }
            }
        }

    override suspend fun isArchive(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            executeWithShizukuSupport(path) { actualPath ->
                try {
                    val result = ArchiveUtil.isArchive(actualPath)
                    Result.Success(result)
                } catch (e: FileNotFoundException) {
                    Result.Error(AppError.FileError.FileNotFound(path))
                } catch (e: SecurityException) {
                    Result.Error(AppError.FileError.PermissionDenied)
                } catch (e: Exception) {
                    Result.Error(AppError.FileError.Unknown(e.message ?: "判断压缩包类型失败"))
                }
            }
        }

    override suspend fun isEncrypted(archivePath: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            executeWithShizukuSupport(archivePath) { actualPath ->
                try {
                    val result = ArchiveUtil.isArchiveEncrypted(actualPath)
                    Result.Success(result)
                } catch (e: FileNotFoundException) {
                    Result.Error(AppError.FileError.FileNotFound(archivePath))
                } catch (e: Exception) {
                    when {
                        e.message?.contains("corrupt", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.CorruptedArchive)

                        else ->
                            Result.Error(
                                AppError.ArchiveError.Unknown(e.message ?: "检查加密状态失败")
                            )
                    }
                }
            }
        }

    override suspend fun validatePassword(archivePath: String, password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            executeWithShizukuSupport(archivePath) { actualPath ->
                try {
                    val result = ArchiveUtil.validate7zPassword(actualPath, password)
                    Result.Success(result)
                } catch (e: Exception) {
                    when {
                        e.message?.contains("corrupt", ignoreCase = true) == true ->
                            Result.Error(AppError.ArchiveError.CorruptedArchive)

                        else ->
                            Result.Error(
                                AppError.ArchiveError.Unknown(e.message ?: "验证密码失败")
                            )
                    }
                }
            }
        }

    // ==================== Shizuku 支持辅助函数 ====================

    /** 根据文件访问类型执行操作 如果需要 Shizuku 权限，则先复制到临时目录，操作完成后删除 */
    private suspend fun <T> executeWithShizukuSupport(
        originalPath: String,
        operation: suspend (actualPath: String) -> Result<T>
    ): Result<T> {
        val accessType = fileToolsManager.getFileAccessType(originalPath)

        return when (accessType) {
            FileAccessType.SHIZUKU -> {
                val tempFile = File(tempDir, File(originalPath).name)
                try {
                    val shizukuTools = fileToolsManager.getShizukuFileTools()
                    val copySuccess = shizukuTools.copyFile(originalPath, tempFile.absolutePath)

                    if (!copySuccess) {
                        return Result.Error(AppError.FileError.CopyFailed)
                    }

                    val result = operation(tempFile.absolutePath)
                   tempFile.delete()
                    result
                } catch (e: Exception) {
                   tempFile.delete()
                    Result.Error(AppError.FileError.Unknown(e.message ?: "Shizuku 操作失败"))
                }
            }

            FileAccessType.DOCUMENT_FILE-> {
                val tempFile = File(tempDir, File(originalPath).name)
                try {
                    val fileTools = fileToolsManager.getDocumentFileTools()
                    val copySuccess = fileTools.copyFileByDF(originalPath, tempFile.absolutePath)

                    if (!copySuccess) {
                        return Result.Error(AppError.FileError.CopyFailed)
                    }

                    val result = operation(tempFile.absolutePath)
                    tempFile.delete()
                    result
                } catch (e: Exception) {
                    tempFile.delete()
                    Result.Error(AppError.FileError.Unknown(e.message ?: "D0ucumentFile 操作失败"))
                }
            }

            FileAccessType.NONE -> {
                Result.Error(AppError.FileError.PermissionDenied)
            }

            else -> {
                operation(originalPath)
            }
        }
    }

    /** Shizuku 支持版本，但不删除临时文件 用于返回 InputStream 等需要保留临时文件的场景 */
    private suspend fun <T> executeWithShizukuSupportKeepTemp(
        originalPath: String,
        operation: suspend (actualPath: String) -> Result<T>
    ): Result<T> {
        val accessType = fileToolsManager.getFileAccessType(originalPath)

        return when (accessType) {
            FileAccessType.SHIZUKU -> {
                val tempFile = File(tempDir, File(originalPath).name)
                try {
                    val shizukuTools = fileToolsManager.getShizukuFileTools()
                    val copySuccess = shizukuTools.copyFileByDF(originalPath, tempFile.absolutePath)

                    if (!copySuccess) {
                        return Result.Error(AppError.FileError.CopyFailed)
                    }

                    // 不删除临时文件，由调用者管理
                    operation(tempFile.absolutePath)
                } catch (e: Exception) {
                    tempFile.delete()
                    Result.Error(AppError.FileError.Unknown(e.message ?: "Shizuku 操作失败"))
                }
            }

            FileAccessType.DOCUMENT_FILE-> {
                val tempFile = File(tempDir, File(originalPath).name)
                try {
                    val fileTools = fileToolsManager.getDocumentFileTools()
                    val copySuccess = fileTools.copyFileByDF(originalPath, tempFile.absolutePath)

                    if (!copySuccess) {
                        return Result.Error(AppError.FileError.CopyFailed)
                    }

                    val result = operation(tempFile.absolutePath)
                    tempFile.delete()
                    result
                } catch (e: Exception) {
                    tempFile.delete()
                    Result.Error(AppError.FileError.Unknown(e.message ?: "D0ucumentFile 操作失败"))
                }
            }

            FileAccessType.NONE -> {
                Result.Error(AppError.FileError.PermissionDenied)
            }

            else -> {
                operation(originalPath)
            }
        }
    }

    /** 清理临时目录 可在应用退出或适当时机调用 */
    override suspend fun clearTempDirectory() {
        withContext(Dispatchers.IO) {
            tempDir.listFiles()?.forEach { it.delete() }
        }
    }
}
