package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.filetools.FileToolsManager

import top.laoxin.modmanager.tools.manager.AppPathsManager
import javax.inject.Inject
import javax.inject.Singleton

data class DeleteModResult(
    val code: Int,
    // 删除的已开启的mods
    val delEnableMods: List<ModBean>,
    // 删除的全部mods
    val delMods: List<ModBean>,
)
@Singleton
class DeleteModUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val fileObserverManager: FlashModsObserverManager,
    private val fileToolsManager: FileToolsManager,
    private val modRepository: ModRepository,
) {
    // 读取readme文件
    suspend operator fun invoke(delMod: ModBean): DeleteModResult = withContext(Dispatchers.IO) {
        fileObserverManager.stopWatching()
        val delMods = modRepository.getModsByPathAndGamePackageName(
            delMod.path!!, delMod.gamePackageName!!
        ).first()
        // 排除包含多个mod文件的压缩包
        val disableMods = delMods.filter { !it.isEnable }
        val enableMods = delMods.filter { it.isEnable }
        deleteMods(delMods)
        modRepository.deleteAll(disableMods)

        return@withContext DeleteModResult(ResultCode.SUCCESS, enableMods, delMods)
    }

    private fun deleteMods(mods: List<ModBean>): Boolean {
        return try {
            val fileTools = fileToolsManager.getFileTools()
            mods.forEach {
                fileTools.deleteFile(it.path!!)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}