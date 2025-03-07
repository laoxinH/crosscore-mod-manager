package top.laoxin.modmanager.domain.usercase.setting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.repository.backup.BackupRepository
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteBackupUserCase @Inject constructor(
   private val gameInfoManager: GameInfoManager,
    private val backupRepository: BackupRepository,
    private val modRepository: ModRepository,
    private val fileToolsManager: FileToolsManager,
    private val appPathsManager: AppPathsManager,
) {
    // 读取readme文件
    suspend operator fun invoke(): Int = withContext(Dispatchers.IO) {
        val gameInfo = gameInfoManager.getGameInfo()
        if (gameInfo.packageName.isEmpty()) {
            return@withContext ResultCode.NO_SELECTED_GAME
        }
        modRepository.getEnableMods(gameInfo.packageName).first().let { enableMods ->
            if (enableMods.isNotEmpty()) {
                return@withContext ResultCode.HAVE_ENABLE_MODS
            } else {
                val delBackupFile: Boolean = deleteBackupFiles(gameInfo)
                if (delBackupFile) {
                    backupRepository.deleteByGamePackageName(gameInfo.packageName)
                    return@withContext ResultCode.SUCCESS
                } else {
                    return@withContext ResultCode.FAIL
                }
            }
        }
    }

    private suspend fun deleteBackupFiles(
        gameInfo: GameInfoBean
    ): Boolean {

        return if (File(appPathsManager.getBackupPath() + gameInfo.packageName).exists()) {
            val fileTools = fileToolsManager.getFileTools()
            kotlin.runCatching { fileTools.deleteFile(appPathsManager.getBackupPath() + gameInfo.packageName) }.isSuccess
        } else {
            true
        }
    }
}