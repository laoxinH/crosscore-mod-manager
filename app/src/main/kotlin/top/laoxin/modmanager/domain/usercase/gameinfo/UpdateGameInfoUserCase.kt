package top.laoxin.modmanager.domain.usercase.gameinfo

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.tools.AppInfoTools
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager


import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateGameInfoUserCase @Inject constructor(
    private val appInfoTools: AppInfoTools,
    private val gameInfoManager: GameInfoManager,
    private val specialGameToolsManager: SpecialGameToolsManager,
    @FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools
) {
    suspend operator fun invoke(
        selectedGameIndex: Int,
        modPath: String,
    ) = withContext(Dispatchers.IO) {
        var gameInfo = gameInfoManager.getGameInfoByIndex(selectedGameIndex)
        createModsDirectory(gameInfo, modPath)
        Log.d("ConsoleViewModel", "getGameInfo: $gameInfo")
        if (gameInfo.packageName.isNotEmpty()) {

            gameInfo = if (appInfoTools.isAppInstalled(gameInfo.packageName)) {
                val modifyGameInfo =  gameInfo.copy(
                    version = appInfoTools.getVersionName(gameInfo.packageName),
                    modSavePath = modPath + gameInfo.packageName + File.separator
                )
                val specialGameTools = specialGameToolsManager.getSpecialGameTools(gameInfo.packageName)
                specialGameTools?.specialOperationUpdateGameInfo(modifyGameInfo) ?: modifyGameInfo
            } else {
                 gameInfoManager.getGameInfoByIndex(0)
            }
        }
        gameInfoManager.setGameInfo(gameInfo)
        return@withContext gameInfo
    }

    private suspend fun createModsDirectory(gameInfo: GameInfoBean, path: String) {
        try {
            File(path + gameInfo.packageName).mkdirs()
        } catch (e: Exception) {
            Log.e("UpdateGameInfoUserCase", "创建文件夹失败: $e")
        }

    }
}