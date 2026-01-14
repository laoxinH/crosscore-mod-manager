package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean

/**
 * Mod 搜索 UI 状态
 */
data class ModSearchUiState(
    val searchBoxVisible: Boolean = false,
    val searchContent: String = "",
    val searchModList: List<ModBean> = emptyList(),
    val isSearching: Boolean = false,
)
