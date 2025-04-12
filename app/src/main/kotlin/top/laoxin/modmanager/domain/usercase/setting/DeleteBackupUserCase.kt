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
    suspend operator fun invoke(): Pair<Int, String?> = withContext(Dispatchers.IO) {
        val gameInfo = gameInfoManager.getGameInfo()
        if (gameInfo.packageName.isEmpty()) {
            return@withContext Pair(ResultCode.NO_SELECTED_GAME, null)
        }
        modRepository.getEnableMods(gameInfo.packageName).first().let { enableMods ->
            if (enableMods.isNotEmpty()) {
                return@withContext Pair(ResultCode.HAVE_ENABLE_MODS, null)
            } else {
                val delBackupFile: Boolean = deleteBackupFiles(gameInfo)
                if (delBackupFile) {
                    backupRepository.deleteByGamePackageName(gameInfo.packageName)
                    return@withContext Pair(ResultCode.SUCCESS, gameInfo.packageName)
                } else {
                    return@withContext Pair(ResultCode.FAIL, gameInfo.packageName)
                }
            }
        }
    }

    private fun deleteBackupFiles(gameInfo: GameInfoBean) =
        if (File(appPathsManager.getBackupPath() + gameInfo.packageName).exists()) {
            val fileTools = fileToolsManager.getFileTools()
            kotlin.runCatching { fileTools.deleteFile(appPathsManager.getBackupPath() + gameInfo.packageName) }.isSuccess
        } else {
            true
        }
}