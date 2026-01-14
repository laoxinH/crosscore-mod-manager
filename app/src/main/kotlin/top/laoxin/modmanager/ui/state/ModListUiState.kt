package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean

/**
 * Mod 列表 UI 状态
 */
data class ModListUiState(
    val modList: List<ModBean> = emptyList(),
    val enableModList: List<ModBean> = emptyList(),
    val disableModList: List<ModBean> = emptyList(),
    val isMultiSelect: Boolean = false,
    val modsSelected: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    // 开关状态是否可点击
    val modSwitchEnable : Boolean = true
)
