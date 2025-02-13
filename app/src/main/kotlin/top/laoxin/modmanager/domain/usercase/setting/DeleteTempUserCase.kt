package top.laoxin.modmanager.domain.usercase.setting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteTempUserCase @Inject constructor(
    private val fileToolsManager: FileToolsManager,
    private val appPathsManager: AppPathsManager,
) {
    // 读取readme文件
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (File(appPathsManager.getModsTempPath()).exists()) {
            val fileTools = fileToolsManager.getFileTools()
            fileTools.deleteFile(appPathsManager.getModsTempPath())
        } else {
            true
        }
    }

}