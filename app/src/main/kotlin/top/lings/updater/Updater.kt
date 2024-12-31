package top.lings.updater

import android.util.Log
import top.laoxin.modmanager.App
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import top.laoxin.modmanager.tools.ToastUtils
import top.lings.updater.util.GithubApi

object Updater {
    suspend fun checkUpdate(): Pair<String, String>? {
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
                return Pair(release.getDownloadLink(), release.info)
            } else {
                ToastUtils.longCall(App.get().getString(R.string.toast_no_update))
            }
        }
        return null
    }
}