package top.laoxin.modmanager.data.service

import android.util.Log
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.service.EnableFileEvent
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.TraditionalModEnableService

/** 传统形式 MOD 开启服务实现 处理压缩包和文件夹形式的 MOD 文件操作 在复制/提取时计算 MD5 */
@Singleton
class TraditionalModEnableServiceImpl
@Inject
constructor(private val fileService: FileService, private val archiveService: ArchiveService) :
    TraditionalModEnableService {

    companion object {
        private const val TAG = "TraditionalModEnable"
    }

    override fun enableZipMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent> =
        flow {
            val modFiles = mod.modFiles
            val gameFilePaths = mod.gameFilesPath

            if (modFiles.isEmpty() || gameFilePaths.isEmpty()) {
                emit(
                    EnableFileEvent.Complete(
                        false,
                        AppError.ModError.InvalidModData(mod.name)
                    )
                )
                return@flow
            }

            if (modFiles.size != gameFilePaths.size) {
                emit(
                    EnableFileEvent.Complete(
                        false,
                        AppError.ModError.InvalidModData(mod.name)
                    )
                )
                return@flow
            }

            val archivePath = mod.path
            if (archivePath.isEmpty()) {
                emit(
                    EnableFileEvent.Complete(
                        false,
                        AppError.ModError.FileMissing(mod.name)
                    )
                )
                return@flow
            }

            val total = modFiles.size
            val fileMd5Map = mutableMapOf<String, String>()

            for (i in modFiles.indices) {
                val modFilePath = modFiles[i]
                val gameFilePath = gameFilePaths[i]
                val fileName = File(gameFilePath).name

                // 发射文件进度
                emit(EnableFileEvent.FileProgress(fileName, i + 1, total))

                // 确保目标目录存在
                val targetDir = File(gameFilePath).parent ?: ""

                // 使用流式提取并计算 MD5
                val streamResult =
                    archiveService.getFileInputStream(
                        archivePath = archivePath,
                        itemName = modFilePath,
                        password = mod.password.ifEmpty { null }
                    )

                when (streamResult) {
                    is Result.Success -> {
                        val inputStream = streamResult.data

                        // 计算 MD5 并写入文件
                        val md5Result =
                            writeFileWithMd5(inputStream, targetDir, fileName)
                        inputStream.close()

                        when (md5Result) {
                            is Result.Success -> {
                                val md5 = md5Result.data
                                if (md5.isEmpty()) {
                                    emit(
                                        EnableFileEvent.Complete(
                                            false,
                                            AppError.ModError.Md5CalculationFailed(gameFilePath)
                                        )
                                    )
                                    return@flow
                                }
                                fileMd5Map[gameFilePath] = md5
                            }

                            is Result.Error -> {
                                Log.e(TAG, "Failed to write file: $gameFilePath")
                                emit(
                                    EnableFileEvent.Complete(
                                        false,
                                        AppError.ModError.WriteFailed(
                                            gameFilePath
                                        )
                                    )
                                )
                                return@flow
                            }
                        }
                    }

                    is Result.Error -> {
                        Log.e(TAG, "Failed to extract file: $modFilePath")
                        emit(
                            EnableFileEvent.Complete(
                                false,
                                AppError.ModError.ReadFailed(modFilePath)
                            )
                        )
                        return@flow
                    }
                }
            }

            emit(EnableFileEvent.Complete(true, fileMd5Map = fileMd5Map))
        }
            .flowOn(Dispatchers.IO)

    override fun disableZipMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent> =
        flow {
            // 关闭操作由 BackupService 的 restoreBackups 处理
            emit(EnableFileEvent.Complete(true))
        }
            .flowOn(Dispatchers.IO)

    override fun enableFolderMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent> =
        flow {
            val modFiles = mod.modFiles
            val gameFilePaths = mod.gameFilesPath

            if (modFiles.isEmpty() || gameFilePaths.isEmpty()) {
                emit(
                    EnableFileEvent.Complete(
                        false,
                        AppError.ModError.InvalidModData(mod.name)
                    )
                )
                return@flow
            }

            if (modFiles.size != gameFilePaths.size) {
                emit(
                    EnableFileEvent.Complete(
                        false,
                        AppError.ModError.InvalidModData(mod.name)
                    )
                )
                return@flow
            }

            val total = modFiles.size
            val fileMd5Map = mutableMapOf<String, String>()

            for (i in modFiles.indices) {
                val modFilePath = modFiles[i]
                val gameFilePath = gameFilePaths[i]
                val fileName = File(gameFilePath).name

                // 发射文件进度
                emit(EnableFileEvent.FileProgress(fileName, i + 1, total))

                // 确保目标目录存在
                val targetDir = File(gameFilePath).parent
                if (targetDir != null) {
                    fileService.createDirectory(targetDir)
                }

                // 先计算 MOD 文件 MD5
                val md5Result = fileService.calculateFileMd5(modFilePath)
                val md5 =
                    when (md5Result) {
                        is Result.Success -> md5Result.data
                        is Result.Error -> ""
                    }

                if (md5.isEmpty()) {
                    Log.e(TAG, "MD5 calculation failed for: $modFilePath")
                    emit(
                        EnableFileEvent.Complete(
                            false,
                            AppError.ModError.Md5CalculationFailed(modFilePath)
                        )
                    )
                    return@flow
                }

                // 复制文件
                val copyResult = fileService.copyFile(modFilePath, gameFilePath)
                if (copyResult is Result.Error) {
                    Log.e(TAG, "Failed to copy file: $modFilePath -> $gameFilePath")
                    emit(
                        EnableFileEvent.Complete(
                            false,
                            AppError.ModError.CopyFailed(gameFilePath)
                        )
                    )
                    return@flow
                }

                fileMd5Map[gameFilePath] = md5
            }

            emit(EnableFileEvent.Complete(true, fileMd5Map = fileMd5Map))
        }
            .flowOn(Dispatchers.IO)

    override fun disableFolderMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent> =
        flow {
            // 关闭操作由 BackupService 的 restoreBackups 处理
            emit(EnableFileEvent.Complete(true))
        }
            .flowOn(Dispatchers.IO)

    /**
     * 写入文件并计算 MD5（流式处理，避免 OOM）
     * @return Result<String> MD5 值
     */
    private suspend fun writeFileWithMd5(
        inputStream: InputStream,
        targetDir: String,
        fileName: String
    ): Result<String> {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            // 使用 DigestInputStream 包装原始流，在读取时自动计算 MD5
            val digestInputStream = java.security.DigestInputStream(inputStream, digest)
            
            // 直接使用 DigestInputStream 写入文件
            val writeResult = fileService.createFileByStream(
                path = targetDir,
                filename = fileName,
                inputStream = digestInputStream
            )
            
            when (writeResult) {
                is Result.Success -> {
                    // 写入完成后获取 MD5
                    val md5 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (md5.isEmpty()) {
                        Result.Error(AppError.ModError.Md5CalculationFailed(""))
                    } else {
                        Result.Success(md5)
                    }
                }
                is Result.Error -> writeResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating MD5 and writing file", e)
            Result.Error(AppError.FileError.Unknown(e.message ?: "Unknown error"))
        }
    }
}
