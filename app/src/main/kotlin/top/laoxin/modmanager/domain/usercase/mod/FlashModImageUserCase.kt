package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashModImageUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager
) {
    // 读取readme文件
    suspend operator fun invoke(modBean: ModBean): Boolean = withContext(Dispatchers.IO) {
        if (modBean.isEncrypted && modBean.password == null) {
            return@withContext true
        }
        if (modBean.isZipFile) {
            return@withContext try {
                ArchiveUtil.extractSpecificFile(
                    modBean.path!!,
                    modBean.images.orEmpty().map {
                        it.replace(appPathsManager.getModsImagePath() + File(modBean.path).nameWithoutExtension + File.separator,"")
                    },
                    appPathsManager.getModsImagePath() + File(modBean.path).nameWithoutExtension,
                    modBean.password,
                    false
                )
                true
            } catch (_: Exception) {
                false
            }
        }
        return@withContext false
    }
}