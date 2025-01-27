package top.lings.updater

import android.util.Log
import top.laoxin.modmanager.BuildConfig
import top.lings.updater.util.GithubApi

object Updater {
    suspend fun checkUpdate(): Triple<List<String>, String, String>? {
        kotlin.runCatching {
            GithubApi.retrofitService.getLatestRelease()
        }.onFailure { exception ->
            Log.e("ConsoleViewModel", "checkUpdate failed: ${exception.message}", exception)
        }.onSuccess { release ->
            // 当前版本号
            val currentVersion = BuildConfig.VERSION_NAME
            // version 是版本号，这里进行比较
            val releaseVersion = release.version

            if (releaseVersion != currentVersion) {
                Log.d("Updater", "Update available: $release")
                return Triple(
                    listOf(release.getDownloadLink(), release.getDownloadLinkUniversal()),
                    release.info,
                    releaseVersion
                )
            } else {
                Log.d("Updater", "No update available")
            }
        }
        return null
    }
}