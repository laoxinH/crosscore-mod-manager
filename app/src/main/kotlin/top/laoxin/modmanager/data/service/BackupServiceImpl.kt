package top.laoxin.modmanager.data.service

import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ReplacedFileBean
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.BackupEvent
import top.laoxin.modmanager.domain.service.BackupService
import top.laoxin.modmanager.domain.service.FileService

/** 备份服务实现 负责游戏文件的备份和还原，支持流式状态返回 */
@Singleton
class BackupServiceImpl
@Inject
constructor(database: ModManagerDatabase, private val fileService: FileService) : BackupService {

    companion object {
        private const val TAG = "BackupService"
    }

    // private val backupDao = database.backupDao()

    override fun backupGameFiles(mod: ModBean, gameInfo: GameInfoBean): Flow<BackupEvent> =
            flow {
                        val gameFilePaths = mod.gameFilesPath
                        if (gameFilePaths.isEmpty()) {
                            emit(BackupEvent.Success(emptyList()))
                            return@flow
                        }

                        val backups = mutableListOf<BackupBean>()
                        val currentTime = System.currentTimeMillis()
                        val total = gameFilePaths.size

                        for ((index, gameFilePath) in gameFilePaths.withIndex()) {
                            val fileName = File(gameFilePath).name
                            val backupDir = getBackupDir(gameFilePath, gameInfo)
                            val backupPath = "$backupDir/$fileName"

                            // 发射进度
                            emit(BackupEvent.FileProgress(fileName, index + 1, total))

                            // 检查游戏文件是否存在
                            val gameFileExists =
                                    fileService.isFileExist(gameFilePath).let {
                                        it is Result.Success && it.data
                                    }
                            if (!gameFileExists) {
                                Log.e(TAG, "Game file not found: $gameFilePath")
                                emit(
                                        BackupEvent.Failed(
                                                AppError.ModError.FileMissing(gameFilePath)
                                        )
                                )
                                return@flow
                            }

                            // 计算当前游戏文件 MD5
                            val currentMd5 =
                                    fileService.calculateFileMd5(gameFilePath).let {
                                        if (it is Result.Success) it.data else ""
                                    }

                            // 创建备份目录
                            val dirResult = fileService.createDirectory(backupDir)
                            if (dirResult is Result.Error) {
                                Log.e(TAG, "Failed to create backup dir: $backupDir")
                                emit(
                                        BackupEvent.Failed(
                                                AppError.ModError.CreateDirectoryFailed(backupDir)
                                        )
                                )
                                return@flow
                            }

                            // 备份物理文件
                            val copyResult = fileService.copyFile(gameFilePath, backupPath)
                            if (copyResult is Result.Error) {
                                Log.e(TAG, "Failed to backup file: $gameFilePath")
                                emit(BackupEvent.Failed(AppError.ModError.CopyFailed(gameFilePath)))
                                return@flow
                            }
                            Log.d(TAG, "Backup created: $gameFilePath -> $backupPath")

                            // 检查是否已有此 MOD 的备份记录
                            /* val existingBackup =
                                                     backupDao.getByModIdAndGameFilePath(mod.id, gameFilePath)
                            */
                            /* if (existingBackup != null) {
                                // 更新现有记录
                                val updatedBackup =
                                        existingBackup.copy(
                                                originalMd5 = currentMd5,
                                                backupTime = currentTime,
                                                copyTime = System.currentTimeMillis()
                                        )
                                backupDao.update(updatedBackup)
                                backups.add(updatedBackup)
                            } else {*/
                            // 新建备份记录
                            val newBackup =
                                    BackupBean(
                                            modId = mod.id,
                                            filename = fileName,
                                            gamePath = gameInfo.gamePath,
                                            gameFilePath = gameFilePath,
                                            backupPath = backupPath,
                                            gamePackageName = gameInfo.packageName,
                                            backupTime = currentTime,
                                            copyTime = System.currentTimeMillis(),
                                            originalMd5 = currentMd5
                                    )
                            // backupDao.insert(newBackup)
                            backups.add(newBackup)
                            // }
                        }

                        emit(BackupEvent.Success(backups))
                    }
                    .flowOn(Dispatchers.IO)

    override fun restoreBackups(
            backups: List<BackupBean>,
            replacedFiles: List<ReplacedFileBean>,
            mod: ModBean,
            gameInfo: GameInfoBean
    ): Flow<BackupEvent> =
            flow {
                        if (backups.isEmpty()) {
                            emit(BackupEvent.Success(emptyList()))
                            return@flow
                        }

                        // 创建 gameFilePath -> md5 的映射表
                        val replacedMd5Map = replacedFiles.associate { it.gameFilePath to it.md5 }

                        val total = backups.size
                        val restoredBackups = mutableListOf<BackupBean>()

                        for ((index, backup) in backups.withIndex()) {
                            val fileName = backup.filename

                            // 发射进度
                            emit(BackupEvent.FileProgress(fileName, index + 1, total))

                            // 检查当前游戏文件的 MD5 是否与替换时记录的一致
                            val recordedMd5 = replacedMd5Map[backup.gameFilePath]
                            if (recordedMd5 != null) {
                                val currentMd5Result =
                                        fileService.calculateFileMd5(backup.gameFilePath)
                                val currentMd5 =
                                        if (currentMd5Result is Result.Success)
                                                currentMd5Result.data
                                        else ""

                                if (currentMd5.isNotEmpty() && currentMd5 != recordedMd5) {
                                    // MD5 不一致，说明游戏更新已覆盖，无需还原
                                    Log.d(
                                            TAG,
                                            "游戏已更新, 跳过还原: ${backup.gameFilePath} (recorded=$recordedMd5, current=$currentMd5)"
                                    )
                                    continue
                                }
                            }

                            // 检查备份文件是否存在
                            val existResult = fileService.isFileExist(backup.backupPath)
                            if (existResult is Result.Success && existResult.data) {
                                // 还原到原位置
                                val copyResult =
                                        fileService.copyFile(backup.backupPath, backup.gameFilePath)
                                if (copyResult is Result.Error) {
                                    Log.e(TAG, "Failed to restore file: ${backup.backupPath}")
                                    emit(
                                            BackupEvent.Failed(
                                                    AppError.ModError.CopyFailed(backup.backupPath)
                                            )
                                    )
                                    return@flow
                                }
                                Log.d(
                                        TAG,
                                        "Restored: ${backup.backupPath} -> ${backup.gameFilePath}"
                                )
                                // 删除备份文件
                                fileService.deleteFile(backup.backupPath)
                                restoredBackups.add(backup)
                            } else {
                                Log.e(TAG, "Backup file not found: ${backup.backupPath}")
                                emit(
                                        BackupEvent.Failed(
                                                AppError.ModError.FileMissing(backup.backupPath)
                                        )
                                )
                                return@flow
                            }
                        }

                        emit(BackupEvent.Success(restoredBackups))
                    }
                    .flowOn(Dispatchers.IO)

    /** 获取备份目录路径 */
    private fun getBackupDir(gameFilePath: String, gameInfo: GameInfoBean): String {
        val dataPrefix = "/Android/data/${gameInfo.packageName}/"
        val relativePath =
                if (gameFilePath.contains(dataPrefix)) {
                    val startIndex = gameFilePath.indexOf(dataPrefix) + dataPrefix.length
                    val filePathAfterData = gameFilePath.substring(startIndex)
                    File(filePathAfterData).parent ?: ""
                } else {
                    ""
                }

        return if (relativePath.isNotEmpty()) {
            "${PathConstants.BACKUP_PATH}/${gameInfo.packageName}/$relativePath"
        } else {
            "${PathConstants.BACKUP_PATH}/${gameInfo.packageName}/"
        }
    }
}
