package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.bean.DownloadGameConfigBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.InfoBean
import top.laoxin.modmanager.bean.ThinksBean

data class SettingUiState(
    // 删除备份对话框
    val deleteBackupDialog: Boolean = false,
    val showAcknowledgments: Boolean = false,
    val showSwitchGame: Boolean = false,
    val gameInfoList: List<GameInfoBean> = emptyList(),
    // 更新弹窗
    val showUpdateDialog: Boolean = false,
    // 当前的versionName
    val versionName: String = "",
    // 显示下载游戏配置弹窗
    val showDownloadGameConfigDialog: Boolean = false,
    // 下载游戏配置列表
    val downloadGameConfigList: List<DownloadGameConfigBean> = emptyList(),
    // 感谢名单
    val thinksList: List<ThinksBean> = emptyList(),
    // 游戏提示弹窗
    val showGameTipsDialog: Boolean = false,

    //权限提示窗
    val openPermissionRequestDialog: Boolean = false,
    // 显示通知弹窗
    val showNotificationDialog: Boolean = false,
    // 通知
    val infoBean: InfoBean = InfoBean(0.0, "")
)
