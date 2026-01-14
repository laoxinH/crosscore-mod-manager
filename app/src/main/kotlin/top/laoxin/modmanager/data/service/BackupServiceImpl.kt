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
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ReplacedFileBean
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

    override fun backupGameFiles(
        mod: ModBean,
        gameInfo: GameInfoBean,
        replacedFilesMap: Map<String, ReplacedFileBean>
    ): Flow<BackupEvent> =
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

                // ========== 智能备份检测 ==========
                val replacedFile = replacedFilesMap[gameFilePath]
                if (replacedFile != null) {
                    // 文件有替换记录
                    if (currentMd5 == replacedFile.md5) {
                        // MD5 一致：文件仍为 MOD 实验室替换的状态
                        val backupExists =
                            fileService.isFileExist(backupPath).let {
                                it is Result.Success && it.data
                            }

                        if (backupExists) {
                            // 备份文件存在：跳过物理备份，直接创建 BackupBean
                            Log.d(TAG, "智能跳过备份: $gameFilePath (已有备份且MD5一致)")
                            val existingBackupMd5 =
                                fileService.calculateFileMd5(backupPath).let {
                                    if (it is Result.Success) it.data else ""
                                }
                            backups.add(
                                BackupBean(
                                    modId = mod.id,
                                    filename = fileName,
                                    gamePath = gameInfo.gamePath,
                                    gameFilePath = gameFilePath,
                                    backupPath = backupPath,
                                    gamePackageName = gameInfo.packageName,
                                    backupTime = currentTime,
                                    copyTime = System.currentTimeMillis(),
                                    originalMd5 = existingBackupMd5
                                )
                            )
                            continue // 跳过物理备份
                        }
                        // 备份文件不存在：继续完整备份流程
                        Log.d(TAG, "备份文件不存在，执行完整备份: $gameFilePath")
                    } else {
                        // MD5 不一致：文件被其他方式修改（游戏更新），执行完整备份
                        Log.d(TAG, "MD5不一致（可能游戏更新），执行完整备份: $gameFilePath")
                    }
                }
                // 无替换记录：首次替换，继续完整备份流程

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
                backups.add(newBackup)
            }

            emit(BackupEvent.Success(backups))
        }
            .flowOn(Dispatchers.IO)

    override fun restoreBackups(
        backups: List<BackupBean>,
        replacedFilesMap: Map<String, ReplacedFileBean>,
        mod: ModBean,
        gameInfo: GameInfoBean
    ): Flow<BackupEvent> =
        flow {
            if (backups.isEmpty()) {
                emit(BackupEvent.Success(emptyList()))
                return@flow
            }

            val total = backups.size
            val restoredBackups = mutableListOf<BackupBean>()

            for ((index, backup) in backups.withIndex()) {
                val fileName = backup.filename

                // 发射进度
                emit(BackupEvent.FileProgress(fileName, index + 1, total))

                // ========== 智能还原检测 ==========
                val replacedFile = replacedFilesMap[backup.gameFilePath]

                if (replacedFile != null) {
                    // 有替换记录：检查当前文件 MD5 是否与替换时记录一致
                    val currentMd5Result =
                        fileService.calculateFileMd5(backup.gameFilePath)
                    val currentMd5 =
                        if (currentMd5Result is Result.Success)
                            currentMd5Result.data
                        else ""

                    if (currentMd5.isNotEmpty() && currentMd5 != replacedFile.md5) {
                        // MD5 不一致：文件已被其他方式修改（游戏更新），跳过物理还原
                        Log.d(
                            TAG,
                            "智能跳过还原: ${backup.gameFilePath} (文件已被其他方式修改, recorded=${replacedFile.md5}, current=$currentMd5)"
                        )
                        restoredBackups.add(backup) // 直接视为成功
                        continue
                    }
                    // MD5 一致：文件仍为实验室替换的状态，执行完整还原
                    Log.d(TAG, "MD5一致，执行完整还原: ${backup.gameFilePath}")
                } else {
                    // 无替换记录：可能为老版本实验室生成的记录，执行完整还原
                    Log.d(TAG, "无替换记录（老版本），执行完整还原: ${backup.gameFilePath}")
                }

                // 检查备份文件是否存在
                val existResult = fileService.isFileExist(backup.backupPath)
                if (existResult is Result.Success && existResult.data) {
                    // 备份文件存在，执行还原
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
                    restoredBackups.add(backup)
                } else {
                    // 备份文件不存在：记录日志，视为成功
                    Log.w(TAG, "备份文件缺失，跳过还原（视为成功）: ${backup.backupPath}")
                    restoredBackups.add(backup)
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
