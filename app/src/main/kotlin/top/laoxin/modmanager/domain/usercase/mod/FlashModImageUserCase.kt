package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.VersionRepository
import top.laoxin.modmanager.domain.usercase.app.CheckPermissionUserCase
import top.laoxin.modmanager.exception.NoSelectedGameException
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.lings.updater.util.GithubApi
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