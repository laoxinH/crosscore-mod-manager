package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.tools.filetools.FileToolsManager
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