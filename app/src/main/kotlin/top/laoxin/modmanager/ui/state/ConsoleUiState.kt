package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.InfoBean
import top.laoxin.modmanager.constant.GameInfoConstant

data class ConsoleUiState(
    var antiHarmony: Boolean = false,
    var scanQQDirectory: Boolean = false,
    var selectedDirectory: String = "未选择",
    val scanDownload: Boolean = false,
    val openPermissionRequestDialog: Boolean = false,
    // mod数量
    val modCount: Int = 0,
    // 已开启mod数量
    val enableModCount: Int = 0,
    // 扫描文件夹中的Mods
    val scanDirectoryMods: Boolean = false,
    // 游戏信息
    val gameInfo: GameInfoBean = GameInfoConstant.gameInfoList[0],
    // 是否可以安装mod
    val canInstallMod: Boolean = false,
    // 是否显示扫描文件夹中的Mods对话框
    val showScanDirectoryModsDialog: Boolean = false,
    // 显示升级弹窗
    val showUpgradeDialog: Boolean = false,
    // 显示信息弹窗
    val showInfoDialog: Boolean = false,
    val infoBean: InfoBean = InfoBean(0.0, ""),
    // 显示删除解压目录弹窗
    val showDeleteUnzipDialog: Boolean = false,
    val delUnzipDictionary: Boolean = false,
    // 展示分类视图
    val showCategoryView: Boolean = false,
)