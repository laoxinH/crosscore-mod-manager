package top.laoxin.modmanager.ui.view.components.mod

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.view.components.common.fuzzyContains
import top.laoxin.modmanager.ui.viewmodel.ModernModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModernModListViewModel
import top.laoxin.modmanager.ui.state.ModListFilter
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel

@Composable
fun ModernAllModPage(
        modListViewModel: ModernModListViewModel,
        modDetailViewModel: ModDetailViewModel,
        modOperationViewModel: ModOperationViewModel,
        modSearchViewModel: ModSearchViewModel,
        modBrowserViewModel: ModernModBrowserViewModel
) {
    // 收集列表状态
    val modListUiState by modListViewModel.uiState.collectAsState()
    val modList = modListUiState.modList
    val enableModList = modListUiState.enableModList
    val disableModList = modListUiState.disableModList
    val isMultiSelect = modListUiState.isMultiSelect
    val modsSelected = modListUiState.modsSelected
    val filter = modListUiState.filter

    // 收集搜索状态
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val searchQuery = modSearchUiState.searchContent

    // 收集浏览器状态 (主要为了网格视图设置)
    val modBrowserState by modBrowserViewModel.uiState.collectAsState()
    
    // 收集操作状态
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modSwitchEnable = modOperationUiState.modSwitchEnable

    if (modList.isEmpty()) {
        NoMod()
        return
    }

    val currentMods =
            when (filter) {
                ModListFilter.ALL -> modList
                ModListFilter.ENABLE -> enableModList
                ModListFilter.DISABLE -> disableModList
            }
            
    val currentModList =
            remember(currentMods, searchQuery) {
                currentMods.filter { searchQuery.isBlank() || it.name.fuzzyContains(searchQuery) }
            }

    if (currentModList.isEmpty()) {
        NoMod()
        return
    }

    // Reuse existing ModList component but passing new ViewModel where needed effectively?
    // ModList component expects the OLD ModListViewModel in its signature:
    // fun ModList(..., modListViewModel: ModListViewModel, ...)
    // So we can't directly use ModList component with ModernModListViewModel.
    // We need to either refactor ModList to take lambdas or duplicate ModList as well.
    // Given the depth, duplicating/refactoring ModList is needed.
    // Let's assume for now we use a compatible ModernModList component or we refactor ModList to interface.
    // But modifying ModList breaks old code.
    // So we need ModernModList.kt.
    
    // For this step, I will create ModernModList content inline or assume generic lists.
    // However, looking at ModList implementation (not visible in previous turns but imported in AllMod.kt),
    // It's likely complex (Grid/List toggle etc).
    
    // Wait, ModList isn't shown in the file view of AllMod.kt, it's imported?
    // No, ModList is likely in ModList.kt.
    // I need to check ModList.kt before I can finish this file properly.
    // But I will write a placeholder or better yet, modify this file to use a new ModernModList which I will create.
    
    // Scroll State Management
    val filterKey = filter.name // "ALL", "ENABLE", "DISABLE"
    
    val (listInitIndex, listInitOffset) = remember(filterKey) {
        modListViewModel.getScrollState("${filterKey}_LIST")
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = listInitIndex,
        initialFirstVisibleItemScrollOffset = listInitOffset
    )

    val (gridInitIndex, gridInitOffset) = remember(filterKey) {
        modListViewModel.getScrollState("${filterKey}_GRID")
    }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
        initialFirstVisibleItemIndex = gridInitIndex,
        initialFirstVisibleItemScrollOffset = gridInitOffset
    )

    // Save scroll state on filter change or dispose
    androidx.compose.runtime.DisposableEffect(filterKey) {
        onDispose {
            modListViewModel.saveScrollState(
                "${filterKey}_LIST",
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
            modListViewModel.saveScrollState(
                "${filterKey}_GRID",
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset
            )
        }
    }

    ModernModList(
            mods = currentModList,
            modsSelected = modsSelected,
            modSwitchEnable = modSwitchEnable,
            isMultiSelect = isMultiSelect,
            isGridView = modBrowserState.isGridView,
            showDialog = { mod -> modDetailViewModel.openModDetail(mod, true) },
            enableMod = { mod, enable -> modOperationViewModel.switchMod(mod, enable) },
            onLongClick = { mod -> modListViewModel.modLongClick(mod) },
            onMultiSelectClick = { mod -> modListViewModel.modMultiSelectClick(mod) },
            modOperationViewModel = modOperationViewModel,
            modDetailViewModel = modDetailViewModel,
            modListViewModel = modListViewModel,
            listState = listState,
            gridState = gridState
    )
}


@Composable
fun NoMod() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = R.string.mod_page_no_mod),
            style = MaterialTheme.typography.titleMedium
        )
    }
}