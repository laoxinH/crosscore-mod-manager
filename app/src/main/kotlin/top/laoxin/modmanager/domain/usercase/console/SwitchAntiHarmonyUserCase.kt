package top.laoxin.modmanager.domain.usercase.console


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode

import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.repository.antiharmony.AntiHarmonyRepository
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchAntiHarmonyUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val antiHarmonyRepository: AntiHarmonyRepository,
    private val permissionTools: PermissionTools,
    private val gameInfoManager: GameInfoManager,
    private val fileToolsManager: FileToolsManager
) {
    private var fileTools: BaseFileTools? = null

    suspend operator fun invoke(b: Boolean): Int = withContext(Dispatchers.IO) {
        val gameInfo = gameInfoManager.getGameInfo()
        if (gameInfo.antiHarmonyFile.isEmpty() || gameInfo.antiHarmonyContent.isEmpty()) {
            return@withContext ResultCode.NOT_SUPPORT
        }
        val pathType = permissionTools.checkPermission(gameInfo.gamePath)
        if (pathType == PathType.NULL) {
            return@withContext ResultCode.NO_PERMISSION
        } else if (pathType == PathType.DOCUMENT) {
            val myPathType = permissionTools.checkPermission(appPathsManager.getMyAppPath())
            if (myPathType == PathType.NULL) {
                return@withContext ResultCode.NO_MY_APP_PERMISSION
            }
        }
        fileTools = fileToolsManager.getFileTools(pathType)
        antiHarmony(gameInfo, true)
        antiHarmonyRepository.updateByGamePackageName(gameInfo.packageName, b)
        return@withContext ResultCode.SUCCESS
    }

    // 反和谐
    fun antiHarmony(gameInfo: GameInfoBean, b: Boolean): Boolean {
        return try {
            if (b) {
                if (fileTools?.isFileExist(
                        appPathsManager.getBackupPath() + gameInfo.modSavePath + File(
                            gameInfo.antiHarmonyFile
                        ).name
                    ) != true
                ) {
                    fileTools?.copyFile(
                        gameInfo.antiHarmonyFile,
                        appPathsManager.getBackupPath() + gameInfo.packageName + "/" + File(gameInfo.antiHarmonyFile).name
                    )
                }

                fileTools?.writeFile(
                    File(gameInfo.antiHarmonyFile).parent!!,
                    File(gameInfo.antiHarmonyFile).name,
                    gameInfo.antiHarmonyContent
                )
            } else {
                fileTools?.copyFile(
                    appPathsManager.getBackupPath() + gameInfo.packageName + "/" + File(gameInfo.antiHarmonyFile).name,
                    gameInfo.antiHarmonyFile
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}