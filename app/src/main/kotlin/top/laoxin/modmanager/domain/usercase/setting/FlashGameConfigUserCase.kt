package top.laoxin.modmanager.domain.usercase.setting

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.UserPreferencesKeys
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.usercase.gameinfo.CheckGameConfigUserCase
import top.laoxin.modmanager.domain.usercase.gameinfo.LoadGameConfigUserCase
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashGameConfigUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val checkGameConfigUserCase: CheckGameConfigUserCase,
    private val loadGameConfigUserCase: LoadGameConfigUserCase

) {
    // 读取readme文件
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val first = userPreferencesRepository.getPreferenceFlow(
            UserPreferencesKeys.SELECTED_DIRECTORY,
            appPathsManager.getDownloadModPath()
        ).first()
        readGameConfig(appPathsManager.getRootPath() + first)
        loadGameConfigUserCase(Paths.get(appPathsManager.getMyAppPath(), appPathsManager.getGameConfig()).toString(),appPathsManager.getRootPath())


        return@withContext true

    }

    suspend fun readGameConfig(path: String): List<GameInfoBean> {
        val gameInfoList = mutableListOf<GameInfoBean>()
        try {
            val listFiles = File(path + appPathsManager.getGameConfig()).listFiles()
            File(appPathsManager.getMyAppPath() + appPathsManager.getGameConfig()).mkdirs()
            for (listFile in listFiles!!) {
                if (listFile.name.endsWith(".json")) {
                    try {
                        withContext(Dispatchers.IO) {

                            val fromJson =
                                Gson().fromJson<GameInfoBean>(
                                    listFile.readText(),
                                    GameInfoBean::class.java
                                )

                            //val fromJson = Gson().fromJson(listFile.readText(), GameInfo::class.java)
                            val checkGameInfo = checkGameConfigUserCase(fromJson,appPathsManager.getRootPath())
                            if (File(appPathsManager.getMyAppPath() + appPathsManager.getGameConfig() + checkGameInfo.packageName + ".json").exists()) {
                                Files.delete(
                                    Paths.get(appPathsManager.getMyAppPath() + appPathsManager.getGameConfig() + checkGameInfo.packageName + ".json")
                                )
                            }
                            Files.copy(
                                Paths.get(listFile.absolutePath),
                                Paths.get(appPathsManager.getMyAppPath() + appPathsManager.getGameConfig() + checkGameInfo.packageName + ".json"),
                                StandardCopyOption.REPLACE_EXISTING
                            )

                        }
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall("已读取 : " + listFile.name)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(listFile.name + ":" + e.message)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                ToastUtils.longCall(
                    App.get().getString(R.string.toast_load_game_config_err, e.message)
                )
            }

        }
        return gameInfoList
    }

}