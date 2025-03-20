package top.laoxin.modmanager.domain.usercase.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.bean.InfoBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.di.FileToolsModule.FileToolsImpl
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetInformationUserCase @Inject constructor(
    @param:FileToolsImpl private val fileTools: BaseFileTools,
    private val appPathsManager: AppPathsManager
) {

    suspend operator fun invoke(): InfoBean? = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            ModManagerApi.retrofitService.getInfo()
        }.onFailure {
            Log.e("ConsoleViewModel", "信息提示: $it")
        }.onSuccess { info ->
            if (info.version > getInfoVersion()) {
                val path =
                    Paths.get(appPathsManager.getMyAppPath(), "informationVersion").toString()
                if (fileTools.isFileExist(path)) {
                    fileTools.deleteFile(path)
                }
                fileTools.writeFile(
                    appPathsManager.getMyAppPath(),
                    "informationVersion",
                    info.version.toString()
                )
                return@withContext info
            }
        }
        return@withContext null
    }


    private fun getInfoVersion(): Double {
        val readFile = fileTools.readFile(
            Paths.get(appPathsManager.getMyAppPath(), "informationVersion").toString()
        )
        return if (readFile.isEmpty()) {
            0.0
        } else {
            readFile.toDouble()
        }
    }
}