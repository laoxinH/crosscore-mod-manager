package top.laoxin.modmanager.domain.usercase.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.network.GithubApi
import top.laoxin.modmanager.data.repository.VersionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpdateUserCase @Inject constructor(
    private val versionRepository: VersionRepository
) {
    suspend operator fun invoke(currentVersion: String): Triple<List<String>, List<String>, Boolean> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                GithubApi.retrofitService.getLatestRelease()
            }.onFailure { exception ->
                if (versionRepository.getVersion() != currentVersion) {
                    return@withContext Triple(
                        listOf(
                            versionRepository.getVersionUrl(),
                            versionRepository.getUniversalUrl()
                        ),
                        listOf(versionRepository.getVersionInfo(), versionRepository.getVersion()),
                        true
                    )
                }
                return@withContext Triple(
                    listOf(),
                    listOf(),
                    false
                )
            }.onSuccess { release ->
                if (release.version != currentVersion) {
                    Log.d("Updater", "Update available: $release")
                    versionRepository.saveVersion(release.version)
                    versionRepository.saveVersionInfo(release.info)
                    versionRepository.saveVersionUrl(release.getDownloadLink())
                    versionRepository.saveUniversalUrl(release.getDownloadLinkUniversal())
                    return@withContext Triple(
                        listOf(release.getDownloadLink(), release.getDownloadLinkUniversal()),
                        listOf(release.info, release.version),
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