package top.laoxin.modmanager.bean

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBean(
    val code: Int,
    val name: String,
    val filename: String,
    val url: String,
    val time: Long,
    val des: String,
    val size: Long,
    val md5: String
)