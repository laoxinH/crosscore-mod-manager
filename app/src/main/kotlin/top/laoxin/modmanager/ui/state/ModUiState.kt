package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.view.modView.NavigationIndex
import java.io.File

/**
 * 简化的ModUiState - 只保留必要的全局状态
 * 其他状态已分散到各个专门的ViewModel中
 */
data class ModUiState(
    // 核心数据状态
    val modList: List<ModBean> = emptyList(),
    val enableModList: List<ModBean> = emptyList(),
    val disableModList: List<ModBean> = emptyList(),
    val searchModList: List<ModBean> = emptyList(),
    
    // 全局加载状态
    val isLoading: Boolean = false,
    val loadingPath: String = "",
    val isReady: Boolean = false,
    val isInitializing: Boolean = true,
    
    // 视图状态
    val modsView: NavigationIndex = NavigationIndex.MODS_BROWSER,
    val currentGameModPath: String = "",
    val currentFiles: List<File> = emptyList(),
    val currentMods: List<ModBean> = emptyList(),
    val currentPath: String = "",
    val isBackPathExist: Boolean = false,
    
    // 多选状态
    val isMultiSelect: Boolean = false,
    val modsSelected: List<Int> = emptyList(),
    
    // 全局对话框状态
    val showUserTipsDialog: Boolean = false,
    
    // 向后兼容的字段（逐步迁移到专门的ViewModel）
    @Deprecated("Use ModDetailViewModel instead")
    val modDetail: ModBean? = null,
    @Deprecated("Use ModDetailViewModel instead")
    val showModDetail: Boolean = false,
    @Deprecated("Use ModSearchViewModel instead")
    val searchBoxVisible: Boolean = false,
    @Deprecated("Use ModSearchViewModel instead")
    val searchContent: String = "",
    @Deprecated("Use ModOperationViewModel instead")
    val showPasswordDialog: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val tipsText: String = "",
    @Deprecated("Use ModOperationViewModel instead")
    val showTips: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val modSwitchEnable: Boolean = true,
    @Deprecated("Use ModOperationViewModel instead")
    val unzipProgress: String = "",
    @Deprecated("Use ModOperationViewModel instead")
    val multitaskingProgress: String = "",
    @Deprecated("Use ModOperationViewModel instead")
    val isSnackbarHidden: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val openPermissionRequestDialog: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val showDelSelectModsDialog: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val showDelModDialog: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val showOpenFailedDialog: Boolean = false,
    @Deprecated("Use ModOperationViewModel instead")
    val openFailedMods: List<ModBean> = emptyList(),
    @Deprecated("Use ModScanViewModel instead")
    val delEnableModsList: List<ModBean> = emptyList(),
    @Deprecated("Use ModScanViewModel instead")
    val showDisEnableModsDialog: Boolean = false,
    @Deprecated("Use ModScanViewModel instead")
    val showForceScanDialog: Boolean = false,
    @Deprecated("Use ModBrowserViewModel instead")
    val doBackFunction: Boolean = false,
    @Deprecated("Legacy field")
    val selectedMenuItem: Int = 0
)
