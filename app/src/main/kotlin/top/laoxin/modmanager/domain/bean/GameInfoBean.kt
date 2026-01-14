package top.laoxin.modmanager.domain.bean


import kotlinx.serialization.Serializable

@Serializable
data class GameInfoBean(
    val gameName: String,
    val serviceName: String,
    val packageName: String,
    val gamePath: String,
    val modSavePath: String = "",
    val antiHarmonyFile: String = "",
    val antiHarmonyContent: String = "",
    val gameFilePath: List<String>,
    val version: String,
    val modType: List<String>,
    val isGameFileRepeat: Boolean = true,
    val enableBackup: Boolean = true,
    val tips: String = ""

)
