package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.view.modView.NavigationIndex
import java.io.File

/**
 * Mod 文件浏览器 UI 状态
 */
data class ModBrowserUiState(
    val modsView: NavigationIndex = NavigationIndex.MODS_BROWSER,
    val currentGameModPath: String = "",
    val currentPath: String = "",
    val currentFiles: List<File> = emptyList(),
    val currentMods: List<ModBean> = emptyList(),
    val isBackPathExist: Boolean = false,
    val doBackFunction: Boolean = false,
    val allMods: List<ModBean> = emptyList(),
    val showCategory: Boolean = true,
    /** 是否为大图网格视图 (false=列表视图, true=大图网格视图) */
    val isGridView: Boolean = false,
)
