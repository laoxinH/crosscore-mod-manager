package top.laoxin.modmanager.service.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileInfoBean(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
) : Parcelable