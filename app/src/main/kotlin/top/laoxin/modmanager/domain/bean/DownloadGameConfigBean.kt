package top.laoxin.modmanager.domain.bean

import kotlinx.serialization.Serializable

@Serializable
data class DownloadGameConfigBean(
    val gameName: String,
    val packageName: String,
    val serviceName: String,
    val downloadUrl: String,
)
