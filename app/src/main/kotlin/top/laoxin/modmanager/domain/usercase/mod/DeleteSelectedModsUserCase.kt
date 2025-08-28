package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import javax.inject.Inject
import javax.inject.Singleton

data class DeleteSelectedModsResult(
    val code: Int,
    // 删除的已开启的mods
    val delEnableMods: List<ModBean>,
)

@Singleton
class DeleteSelectedModUserCase @Inject constructor(
    private val fileObserverManager: FlashModsObserverManager,
    private val fileToolsManager: FileToolsManager,
    private val modRepository: ModRepository,
) {
    // 读取readme文件
    suspend operator fun invoke(modIds: List<Int>): DeleteSelectedModsResult =
        withContext(Dispatchers.IO) {
            if (modIds.isEmpty()) {
                return@withContext DeleteSelectedModsResult(ResultCode.NO_EXECUTE, emptyList())
            }
            fileObserverManager.stopWatching()
            val delMods = modRepository.getModsByIds(modIds).first()
            // 排除包含多个mod文件的压缩包
            val singleFileMods =
                delMods.filter { modRepository.getModsCountByPath(it.path!!).first() == 1 }
            val singleFileDisableMods = singleFileMods.filter { !it.isEnable }
            val enableMods = singleFileMods.filter { it.isEnable }
            deleteMods(singleFileMods)
            modRepository.deleteAll(singleFileDisableMods)
            fileObserverManager.startWatching()
            return@withContext DeleteSelectedModsResult(ResultCode.SUCCESS, enableMods)
        }


    private fun deleteMods(mods: List<ModBean>): Boolean {
        return try {
            val fileTools = fileToolsManager.getFileTools()
            mods.forEach {
                fileTools.deleteFile(it.path!!) == true
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}