package top.laoxin.modmanager.bean

data class ModBeanTemp(
    val name: String,
    val modFiles: MutableList<String>,
    var readmePath: String?,
    var fileReadmePath: String?,
    var iconPath: String?,
    var images: MutableList<String>,
    val isEncrypted: Boolean,
    val gamePackageName: String,
    val modType: String,
    val gameModPath: String,
    val modPath: String,
    val isZip: Boolean = true,
)
