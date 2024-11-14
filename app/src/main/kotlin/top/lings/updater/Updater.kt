package top.lings.updater

import android.util.Log
import top.laoxin.modmanager.BuildConfig

object Updater {
    suspend fun checkUpdate(): Pair<String, String>? {
        kotlin.runCatching {
            GithubApi.retrofitService.getLatestRelease()
        }.onFailure { exception ->
            Log.e("ConsoleViewModel", "checkUpdate failed: ${exception.message}", exception)
        }.onSuccess { release ->
            Log.d("ConsoleViewModel", "Update available: $release")
            // 当前版本号
            val currentVersion = BuildConfig.VERSION_NAME
            // version 是版本号，这里进行比较
            val releaseVersion = release.version

            if (releaseVersion != currentVersion) {
                Log.d("ConsoleViewModel", "Update available: $release")
                return Pair(release.getDownloadLink(), release.info)
            }
        }
        return null
    }
}