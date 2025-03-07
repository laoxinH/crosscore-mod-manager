package top.laoxin.modmanager.domain.usercase.setting

import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.bean.DownloadGameConfigBean
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.domain.usercase.gameinfo.LoadGameConfigUserCase
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadGameConfigUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val loadGameConfigUserCase: LoadGameConfigUserCase

) {

    suspend operator fun invoke(downloadGameConfigBean: DownloadGameConfigBean): Boolean =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val downloadGameConfig =
                    ModManagerApi.retrofitService.downloadGameConfig(downloadGameConfigBean.packageName)
                writeGameConfigFile(downloadGameConfig)
            }.onFailure {
                Log.e("SettingViewModel", "downloadGameConfig: $it")
                return@withContext false
            }.onSuccess {

                loadGameConfigUserCase(
                    Paths.get(
                        appPathsManager.getMyAppPath(),
                        appPathsManager.getGameConfig()
                    ).toString(), appPathsManager.getRootPath()
                )
                return@withContext true


            }
            return@withContext true

        }

    private fun writeGameConfigFile(downloadGameConfig: GameInfoBean) {
        try {
            val file =
                File(appPathsManager.getMyAppPath() + appPathsManager.getGameConfig() + downloadGameConfig.packageName + ".json")
            if (file.exists()) {
                file.delete()
            }
            if (file.parentFile?.exists() == false) {
                file.parentFile?.mkdirs()
            }
            file.createNewFile()
            val gson = GsonBuilder()
                .disableHtmlEscaping()
                .create()
            file.writeText(gson.toJson(downloadGameConfig, GameInfoBean::class.java))
        } catch (e: Exception) {
            Log.e("DownloadGameConfigUserCase", "写入游戏配置失败: $e")
        }
    }

}