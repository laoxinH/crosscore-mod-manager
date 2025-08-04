package top.laoxin.modmanager.domain.usercase.console

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.UserPreferencesKeys
import top.laoxin.modmanager.domain.usercase.userpreference.SaveUserPreferenceUseCase
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveSelectModDirectoryUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val gameInfoManager: GameInfoManager,
    private val saveUserPreferenceUseCase: SaveUserPreferenceUseCase
) {
    suspend operator fun invoke(selectedDirectoryPath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val gameConfigFile = File(
                    (appPathsManager.getRootPath() + "/$selectedDirectoryPath/" + appPathsManager.getGameConfig()).replace(
                        "tree", ""
                    ).replace("//", "/")
                )
                val gameModsFile = File(
                    (appPathsManager.getRootPath() + "/$selectedDirectoryPath/" + gameInfoManager.getGameInfo().packageName).replace(
                        "tree", ""
                    ).replace("//", "/")
                )
                if (!gameConfigFile.absolutePath.contains("${appPathsManager.getRootPath()}/Android")) {
                    gameConfigFile.mkdirs()
                    if (gameInfoManager.getGameInfo().packageName != "") {
                        gameModsFile.mkdirs()
                    }
                    saveUserPreferenceUseCase(
                        UserPreferencesKeys.SELECTED_DIRECTORY,
                        "/$selectedDirectoryPath/".replace("tree", "").replace("//", "/")
                    )
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("ConsoleViewModel", "setSelectedDirectory: $e")
                return@withContext false
            }
        }
}