package top.laoxin.modmanager.bean

import com.google.gson.annotations.SerializedName
import top.lings.updater.util.AppConfig

data class GithubBean(
    @SerializedName("tag_name") val version: String,
    @SerializedName("body") val info: String,
    @SerializedName("assets") val assets: List<GitHubAssets>
) {
    fun getDownloadLink(): String {
        val asset = assets.find { AppConfig.matchVariant(it.downloadLink) } ?: assets[0]
        return asset.downloadLink
    }
}

data class GitHubAssets(@SerializedName("browser_download_url") val downloadLink: String)
