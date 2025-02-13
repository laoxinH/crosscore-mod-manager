package top.laoxin.modmanager.domain.usercase.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.repository.VersionRepository
import top.lings.updater.util.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpdateUserCase @Inject constructor(
    private val versionRepository: VersionRepository
) {

    suspend operator fun invoke(currentVersion: String): Triple<List<String>, List<String>, Boolean> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            GithubApi.retrofitService.getLatestRelease()
        }.onFailure { exception ->
            if (versionRepository.getVersion() != currentVersion) {
                return@withContext Triple(
                    listOf(versionRepository.getVersionUrl(), versionRepository.getUniversalUrl()),
                    listOf(versionRepository.getVersionInfo(),versionRepository.getVersion()),
                    true
                )
            }
            return@withContext Triple(
                listOf(),
                listOf(),
                false
            )
        }.onSuccess { release ->
            // version 是版本号，这里进行比较
            val releaseVersion = release.version

            if (releaseVersion != currentVersion) {
                Log.d("Updater", "Update available: $release")
                versionRepository.saveVersionInfo(releaseVersion)
                versionRepository.saveVersionUrl(release.getDownloadLink())
                versionRepository.saveUniversalUrl(release.getDownloadLinkUniversal())
                return@withContext Triple(
                    listOf(release.getDownloadLink(), release.getDownloadLinkUniversal()),
                    listOf(release.info,releaseVersion),
                    true
                )
            } else {
                Log.d("Updater", "No update available")
            }
        }
        return@withContext Triple(
            listOf(),
            listOf(),
            false
        )
    }
}