package top.laoxin.modmanager.ui.viewmodel

data class ConsoleUiState (
    var antiHarmony: Boolean = false,
    var scanQQDirectory: Boolean = false,
    var selectedDirectory: String = "未选择",
    val scanDownload: Boolean = false,
    val openPermissionRequestDialog: Boolean = false,
    // mod数量
    val modCount: Int = 0,
    // 已开启mod数量
    val enableModCount: Int = 0,
    // 权限路径


)