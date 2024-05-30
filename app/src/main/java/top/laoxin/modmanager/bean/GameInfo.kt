package top.laoxin.modmanager.bean

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap

data class GameInfo(
    val name: String,
    val packageName: String,
    val versionCode: String,
    val serviceName: String,
    val icon: ImageBitmap,
)
