package top.laoxin.modmanager.data.bean

import com.google.gson.annotations.SerializedName
import top.lings.updater.util.AppConfig

data class GithubBean(
    @SerializedName("tag_name") val version: String,
    @SerializedName("body") val info: String,
    @SerializedName("assets") val assets: List<GitHubAssets>
) {
    fun getDownloadLink(): String {
        return assets.find { AppConfig.matchVariant(it.downloadLink) }?.downloadLink
            ?: assets[0].downloadLink
    }

    fun getDownloadLinkUniversal(): String {
        return assets.find { it.downloadLink.contains("universal") }?.downloadLink
            ?: assets[0].downloadLink
    }
}

data class GitHubAssets(@SerializedName("browser_download_url") val downloadLink: String)
