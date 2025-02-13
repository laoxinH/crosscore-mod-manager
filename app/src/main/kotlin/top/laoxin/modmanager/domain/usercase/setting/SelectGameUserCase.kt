package top.laoxin.modmanager.domain.usercase.setting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.UserPreferencesKeys
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.usercase.mod.EnsureGameModPathUserCase
import top.laoxin.modmanager.tools.AppInfoTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectGameUserCase @Inject constructor(
    private val fileToolsManager: FileToolsManager,
    private val appPathsManager: AppPathsManager,
    private val appInfoTools: AppInfoTools,
    private val specialGameToolsManager: SpecialGameToolsManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gameInfoManager: GameInfoManager,
    private val ensureGameModPathUserCase: EnsureGameModPathUserCase
) {
    // 读取readme文件
    suspend operator fun invoke(gameInfo: GameInfoBean): Boolean = withContext(Dispatchers.IO) {
        val result = appInfoTools.isAppInstalled(gameInfo.packageName)
        if (result) {
            ensureGameModPathUserCase(appPathsManager.getRootPath() + userPreferencesRepository.getPreferenceFlow(UserPreferencesKeys.SELECTED_DIRECTORY, "").first() + gameInfo.packageName + File.separator)
            userPreferencesRepository.savePreference(UserPreferencesKeys.SELECTED_GAME, gameInfoManager.getGameInfoList().indexOf(gameInfo))
            val modifyGameInfo = gameInfo.copy(
                version = appInfoTools.getVersionName(gameInfo.packageName),
            )
            specialGameToolsManager.getSpecialGameTools(gameInfo.packageName)
                ?.specialOperationSelectGame(modifyGameInfo)
            return@withContext true
        } else {
            return@withContext false
        }

    }

}