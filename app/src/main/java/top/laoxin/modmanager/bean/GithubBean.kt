package top.laoxin.modmanager.bean

import kotlinx.serialization.Serializable

@Serializable
data class GithubBean(
    val tag_name: String,
    val name: String,
    val body: String,
    val published_at: String,
    val assets: List<Asset>
)

@Serializable
data class Asset(
    val name: String,
    val browser_download_url: String
)
