package top.laoxin.modmanager.data.service

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.FindModEvent
import top.laoxin.modmanager.domain.service.FindModResult
import top.laoxin.modmanager.domain.service.ModScanService
import top.laoxin.modmanager.domain.service.ModSourcePrepareService
import top.laoxin.modmanager.domain.service.TransferEvent
import top.laoxin.modmanager.domain.service.TransferResult

/** MOD 源文件准备服务实现 从外部目录转移 MOD 文件到 MOD 目录 */
class ModSourcePrepareServiceImpl
@Inject
constructor(
    private val fileService: FileService,
    private val archiveService: ArchiveService,
    private val modScanService: ModScanService
) : ModSourcePrepareService {

    companion object {
        private const val TAG = "ModSourcePrepareService"

        // 支持的压缩包扩展名
        private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z")
    }

    override fun transferModSources(
        externalPaths: List<String>,
        targetPath: String,
        gameInfo: GameInfoBean
    ): Flow<TransferEvent> =
        flow {
            var transferredCount = 0
            var skippedCount = 0
            val errors = mutableListOf<String>()

            // 使用 FileService 确保目标目录存在
            fileService.createDirectory(targetPath)

            // 收集所有需要转移的文件
            val allModFiles = mutableListOf<String>()

            for ((index, externalPath) in externalPaths.withIndex()) {
                // 发射扫描进度
                emit(TransferEvent.Scanning(externalPath, index + 1, externalPaths.size))

                // 查找外部目录中的 MOD 文件
                val findResult = findModFiles(externalPath, gameInfo).collect {
                    when (it) {
                        is FindModEvent.Scanning -> {}
                        is FindModEvent.ScanningFile -> {
                            emit(
                                TransferEvent.ScanningFile(
                                    directory = externalPath,
                                    fileName = it.fileName,
                                    current = it.current,
                                    total = it.total
                                )
                            )
                        }

                        is FindModEvent.FoundOne -> {}
                        is FindModEvent.Complete -> {
                            allModFiles.addAll(it.result.modFiles)
                            Log.d(
                                TAG,
                                "Found ${it.result.modFiles.size} MOD files in $externalPath"
                            )
                        }

                        is FindModEvent.Error -> {
                            errors.add("扫描目录失败: $externalPath - ${it.err}")
                        }
                    }
                }

            }

            val total = allModFiles.size

            for ((index, modFilePath) in allModFiles.withIndex()) {
                // 获取文件名
                val fileName =
                    modFilePath.substringAfterLast("/").substringAfterLast("\\")
                val targetFilePath = "$targetPath/$fileName"

                // 发射文件进度
                emit(TransferEvent.FileProgress(fileName, index + 1, total))

                try {
                    // 移动并覆盖
                    val moveResult =
                        fileService.moveFile(
                            modFilePath,
                            targetFilePath,
                            overwrite = true
                        )

                    when (moveResult) {
                        is Result.Success -> {
                            transferredCount++
                            Log.d(TAG, "Transferred: $fileName")
                        }

                        is Result.Error -> {
                            errors.add("移动失败: $fileName - ${moveResult.error}")
                        }
                    }
                } catch (e: Exception) {
                    errors.add("移动失败: $fileName - ${e.message}")
                    skippedCount++
                    Log.e(TAG, "Failed to transfer $fileName", e)
                }
            }

            emit(
                TransferEvent.Complete(
                    TransferResult(
                        transferredCount = transferredCount,
                        skippedCount = skippedCount,
                        errors = errors
                    )
                )
            )
        }
            .flowOn(Dispatchers.IO)

    override suspend fun findModFiles(
        externalPath: String,
        gameInfo: GameInfoBean,
    ): Flow<FindModEvent> = flow {
        try {
            // 使用 FileService 检查目录是否存在
            val existsResult = fileService.isFileExist(externalPath)
            if (existsResult is Result.Error ||
                (existsResult is Result.Success && !existsResult.data)
            ) {
                return@flow emit(FindModEvent.Error(AppError.FileError.FileNotFound(externalPath)))
                //return Result.Success(emptyList())
            }

            // 使用 FileService 检查是否为文件（非目录）
            val isFileResult = fileService.isFile(externalPath)
            if (isFileResult is Result.Success && isFileResult.data) {
                return@flow emit(FindModEvent.Error(AppError.FileError.Unknown("不是目录")))
                //return Result.Success(emptyList()) // 不是目录
            }

            val modFiles = mutableListOf<String>()

            // 使用 FileService 遍历目录（不递归，只查找一层）
            val filesResult = fileService.listFiles(externalPath)
            if (filesResult is Result.Error) {
                return@flow emit(FindModEvent.Error(filesResult.error))

            }

            val files = (filesResult as Result.Success).data
            var skippedCount = files.size

            loop@ for (file in files) {
                emit(
                    FindModEvent.ScanningFile(
                        file.absolutePath,
                        files.indexOf(file) + 1,
                        files.size
                    )
                )
                // 只处理压缩包文件
                fileService.isFile(file.absolutePath).onSuccess {
                    if (it) {
                        val ext = file.extension.lowercase()
                        if (ext !in ARCHIVE_EXTENSIONS) continue@loop

                        // 验证是否为有效 MOD
                        val isModResult = modScanService.isModSource(file.absolutePath, gameInfo)
                        if (isModResult is Result.Success && isModResult.data) {
                            skippedCount--
                            modFiles.add(file.absolutePath)
                            emit(
                                FindModEvent.FoundOne(
                                    file.name,
                                    file.absolutePath,
                                )
                            )
                        }
                    }
                }.onError {

                    Log.e(TAG, "Failed to check file type: ${file.absolutePath}")
                }

            }

            emit(
                FindModEvent.Complete(
                    FindModResult(
                        modFiles = modFiles,
                        skippedCount = skippedCount,
                        errors = emptyList()
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find MOD files in $externalPath", e)
            emit(FindModEvent.Error(AppError.FileError.Unknown(e.message ?: "")))
        }
    }


}
