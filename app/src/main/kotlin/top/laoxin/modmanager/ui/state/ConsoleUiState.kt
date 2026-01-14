package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.repository.UpdateInfo

/**
 * 更新信息的数据载体
 */


/**
 * 控制台（主页）UI 状态
 */
data class ConsoleUiState(
    // App/Update Info
    val infoBean: InfoBean? = null,
    val updateInfo: UpdateInfo? = null, // Grouped update info
    val showInfoDialog: Boolean = false,
    val showUpgradeDialog: Boolean = false,

    // Game Info
    val gameInfo: GameInfoBean = GameInfoConstant.NO_GAME, // Non-nullable
    val antiHarmony: Boolean = false,
    val canInstallMod: Boolean = false,
    val modCount: Int = 0,
    val enableModCount: Int = 0,

    // Dialogs & Permissions
    val showScanDirectoryModsDialog: Boolean = false,
    val openPermissionRequestDialog: Boolean = false,
    val showDeleteUnzipDialog: Boolean = false,
    val requestPermissionPath: String = "",

    // User Preferences (mirrored from settings)
    val scanQQDirectory: Boolean = false,
    val selectedDirectory: String = "",
    val scanDownload: Boolean = false,
    val scanDirectoryMods: Boolean = true,
    val delUnzipDictionary: Boolean = false,
    val showCategoryView: Boolean = true,

    // Loading state
    val isLoading: Boolean = true
)
