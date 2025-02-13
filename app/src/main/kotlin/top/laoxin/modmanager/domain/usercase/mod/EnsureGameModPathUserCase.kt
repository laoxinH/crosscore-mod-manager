package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.UserPreferencesKeys
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.repository.UserPreferencesRepository
import top.laoxin.modmanager.tools.AppInfoTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnsureGameModPathUserCase @Inject constructor(
    private val fileToolsManager: FileToolsManager,
) {
    // 读取readme文件
    suspend operator fun invoke(path: String) = withContext(Dispatchers.IO) {
       val fileTools =  fileToolsManager.getFileTools()
        fileTools.createDictionary(path)
    }

}