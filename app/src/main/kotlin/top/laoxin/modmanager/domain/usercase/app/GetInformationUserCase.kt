package top.laoxin.modmanager.domain.usercase.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.bean.InfoBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.di.FileToolsModule.FileToolsImpl

import top.laoxin.modmanager.tools.filetools.BaseFileTools

import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.nio.file.Paths
@Singleton
class GetInformationUserCase @Inject constructor(
    @FileToolsImpl private val fileTools: BaseFileTools,
    private val  appPathsManager: AppPathsManager
) {

    suspend operator fun invoke(myAppPath: String) : InfoBean? = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            ModManagerApi.retrofitService.getInfo()
        }.onFailure {
            Log.e("ConsoleViewModel", "信息提示: $it")
        }.onSuccess { info ->
            if (info.version > getInfoVersion()) {
                val path = Paths.get(appPathsManager.getMyAppPath(), "informationVersion").toString()
                if (fileTools.isFileExist(path)) {
                    fileTools.deleteFile(path)
                }
                fileTools.writeFile(appPathsManager.getMyAppPath(), "informationVersion", info.version.toString())
                return@withContext info
            }
        }
        return@withContext null
    }


    private fun getInfoVersion(): Double {
        val readFile = fileTools.readFile(Paths.get(appPathsManager.getMyAppPath(), "informationVersion").toString())
        if (readFile.isEmpty()) {
            return 0.0
        } else {
            return readFile.toDouble()
        }
    }
}