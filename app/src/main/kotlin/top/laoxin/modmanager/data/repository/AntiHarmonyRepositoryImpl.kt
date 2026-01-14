package top.laoxin.modmanager.data.repository

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.AntiHarmonyBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.repository.AntiHarmonyRepository
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.PermissionService

@Singleton
class AntiHarmonyRepositoryImpl
@Inject
constructor(
        database: ModManagerDatabase,
        private val fileService: FileService,
        private val permissionService: PermissionService
) : AntiHarmonyRepository {
    val antiHarmonyDao = database.antiHarmonyDao()
    override fun getAntiHarmony(gamePackageName: String): Flow<AntiHarmonyBean?> =
            antiHarmonyDao.getByGamePackageName(gamePackageName)

    override suspend fun addGameToAntiHarmony(game: AntiHarmonyBean) {
        antiHarmonyDao.insert(game)
    }

    override suspend fun switchAntiHarmony(gameInfo: GameInfoBean, enable: Boolean): Result<Unit> =
            withContext(Dispatchers.IO) {
                if (gameInfo.antiHarmonyFile.isEmpty()) {
                    return@withContext Result.Error(AppError.AntiHarmonyError.NotSupported)
                }

//                val myAppPath = PathConstants.APP_PATH
//                if (permissionService.getFileAccessType(myAppPath) == FileAccessType.NONE) {
//                    return@withContext Result.Error(
//                            AppError.PermissionError.StoragePermissionDenied
//                    )
//                }
//
                val gamePath = gameInfo.gamePath
                if (permissionService.getFileAccessType(gamePath) == FileAccessType.NONE) {
                    return@withContext Result.Error(
                            AppError.PermissionError.UriPermissionNotGranted
                    )
                }

                val antiHarmonyFile = File(gameInfo.antiHarmonyFile)
                val backupFile = File("${PathConstants.BACKUP_PATH}${antiHarmonyFile.name}")

                try {
                    if (enable) {
                        // Enable: copy original to backup, then write empty file
                        if (!backupFile.exists()) {
                            if (antiHarmonyFile.exists()) {
                                fileService.copyFile(
                                        antiHarmonyFile.absolutePath,
                                        backupFile.absolutePath
                                ).onError { return@withContext Result.Error(it) }
                            } else {
                                // If original file doesn't exist, create an empty backup to
                                // indicate it was originally missing
                                backupFile.createNewFile()
                            }
                        }
                        fileService.writeFile(
                                antiHarmonyFile.parent!!,
                                antiHarmonyFile.name,
                                gameInfo.antiHarmonyContent
                        ).onError { return@withContext Result.Error(it) }
                    } else {
                        // Disable: restore from backup
                        if (backupFile.exists()) {
                            fileService.copyFile(
                                    backupFile.absolutePath,
                                    antiHarmonyFile.absolutePath
                            ).onError { return@withContext Result.Error(it) }
                            backupFile.delete()
                        } else {
                            // If no backup, just delete the file to restore original state (which
                            // was likely non-existent)
                            antiHarmonyFile.delete()
                        }
                    }
                    val antiHarmony = antiHarmonyDao.getByGamePackageName(gameInfo.packageName).first()
                    if (antiHarmony == null) {
                        //Log.d("AntiHarmonyRepositoryImpl", "Inserting anti-harmony for ${gameInfo.packageName}")
                        antiHarmonyDao.insert(
                                AntiHarmonyBean(
                                        gamePackageName = gameInfo.packageName,
                                        isEnable = enable
                                )
                        )
                    } else {
                        //Log.d("AntiHarmonyRepositoryImpl", "Updating anti-harmony for ${gameInfo.packageName}")
                        antiHarmonyDao.updateByGamePackageName(gameInfo.packageName, enable)
                    }
                   // antiHarmonyDao.updateByGamePackageName(gameInfo.packageName, enable)
                    return@withContext Result.Success(Unit)
                } catch (e: Exception) {
                    return@withContext Result.Error(
                            AppError.AntiHarmonyError.OperationFailed(e.message ?: "Unknown error")
                    )
                }
            }
}
