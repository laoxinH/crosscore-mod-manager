package top.laoxin.modmanager.domain.usercase.app

import kotlinx.coroutines.flow.first
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.repository.UpdateInfo
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.repository.VersionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpdateUserCase @Inject constructor(
    private val versionRepository: VersionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * Checks for an update and returns the update info if available.
     * This UseCase delegates all data handling logic to the repository.
     */
    suspend operator fun invoke(currentVersion: String, autoCheck: Boolean = true): UpdateInfo? {
        val updateInfo = versionRepository.getNewUpdateInfo().getOrNull()

         return if (updateInfo != null) {
             if (updateInfo.versionName != currentVersion) {
                 userPreferencesRepository.saveCachedVersionName(updateInfo.versionName)
                 userPreferencesRepository.saveCachedChangelog(updateInfo.changelog)
                 userPreferencesRepository.saveCachedDownloadUrl(updateInfo.downloadUrl)
                 userPreferencesRepository.saveCachedUniversalUrl(updateInfo.universalUrl)
                 //versionRepository.saveUpdateInfo(updateInfo)
                 updateInfo
             }else{
                 if (!autoCheck) {
                     // 从缓存中获取
                     updateInfo
                 } else {
                     null
                 }
             }
        } else {
            // 从缓存中获取
            val cachedVersion = userPreferencesRepository.cachedVersionName.first()
            if (cachedVersion.isNotEmpty() && cachedVersion != currentVersion) {
                val updateInfo = UpdateInfo(
                    downloadUrl = userPreferencesRepository.cachedDownloadUrl.first(),
                    universalUrl = userPreferencesRepository.cachedUniversalUrl.first(),
                    changelog = userPreferencesRepository.cachedChangelog.first(),
                    versionName = cachedVersion
                )
                //versionRepository.saveUpdateInfo(updateInfo)
                updateInfo
            } else {
                //versionRepository.saveUpdateInfo(null)
                null
            }
        }
    }
}
