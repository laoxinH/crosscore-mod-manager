package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.domain.bean.DownloadGameConfigBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.bean.ThanksBean
import top.laoxin.modmanager.domain.repository.UpdateInfo

/** 设置页面的 UI 状态 */
data class SettingUiState(
        // Dialogs
        val showDeleteBackupDialog: Boolean = false,
        val showDeleteCacheDialog: Boolean = false,
        val showAcknowledgmentsDialog: Boolean = false,
        val showSwitchGameDialog: Boolean = false,
        val showGameTipsDialog: Boolean = false,
        val showUpdateDialog: Boolean = false,
        val showDownloadGameConfigDialog: Boolean = false,
        val showNotificationDialog: Boolean = false,
        val openPermissionRequestDialog: Boolean = false,
        val showAboutDialog: Boolean = false,

        // Data
        val thanksList: List<ThanksBean> = emptyList(),
        val gameInfoList: List<GameInfoBean> = emptyList(),
        val currentGame: GameInfoBean = GameInfoConstant.NO_GAME,
        val targetGame: GameInfoBean? = null, // Game selected in dialog
        val downloadGameConfigList: List<DownloadGameConfigBean> = emptyList(),
        val infoBean: InfoBean? = null,
        val updateInfo: UpdateInfo? = null,
        val versionName: String = "",

        // Permission
        val requestPermissionPath: String = "",

        // Loading State
        val isLoading: Boolean = true,

        // 下载状态
        val isDownloading: Boolean = false,

        // 是否在关于页面
        val isAboutPage: Boolean = false,
)
