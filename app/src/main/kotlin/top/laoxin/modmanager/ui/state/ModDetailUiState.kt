package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean

/**
 * Mod详情和预览UiState
 */
data class ModDetailUiState(
    val mod: ModBean? = null,
    val isShown: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isLoadingImage: Boolean = false,
)
