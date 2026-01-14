package top.laoxin.modmanager.ui.view.modView

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
import top.laoxin.modmanager.ui.viewmodel.ModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
import top.laoxin.modmanager.ui.viewmodel.ModBrowserViewModel
import top.laoxin.modmanager.ui.view.common.fuzzyContains

@Composable
fun AllModPage(
    modListViewModel: ModListViewModel,
    modDetailViewModel: ModDetailViewModel,
    modOperationViewModel: ModOperationViewModel,
    modSearchViewModel: ModSearchViewModel,
    modBrowserViewModel: ModBrowserViewModel
) {
    // 收集列表状态
    val modListUiState by modListViewModel.uiState.collectAsState()
    val modList = modListUiState.modList
    val enableModList = modListUiState.enableModList
    val disableModList = modListUiState.disableModList
    val isMultiSelect = modListUiState.isMultiSelect
    val modsSelected = modListUiState.modsSelected

    //收集搜索状态
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val searchModList = modSearchUiState.searchModList
    val searchQuery = modSearchUiState.searchContent

    // 收集浏览器状态
    val modBrowserState by modBrowserViewModel.uiState.collectAsState()
    val modsView = modBrowserState.modsView

    // 收集操作状态
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modSwitchEnable = modOperationUiState.modSwitchEnable

    
    if (modList.isEmpty()) {
        NoMod()
        return
    }
    
    val currentMods = when (modsView) {
        NavigationIndex.ALL_MODS -> modList
        NavigationIndex.ENABLE_MODS -> enableModList
        NavigationIndex.DISABLE_MODS -> disableModList
        NavigationIndex.SEARCH_MODS -> modList
        else -> modList
    }
    val currentModList =  remember(currentMods, searchQuery) {
        currentMods.filter { searchQuery.isBlank() || it.name.fuzzyContains(searchQuery) }
    }

    if (currentModList.isEmpty()) {
        NoMod()
        return
    }
    
    ModList(
        mods = currentModList,
        modsSelected = modsSelected,
        modSwitchEnable = modSwitchEnable,
        isMultiSelect = isMultiSelect,
        isGridView = modBrowserState.isGridView,
        showDialog = { mod -> modDetailViewModel.openModDetail(mod,true) },
        enableMod = { mod, enable -> modOperationViewModel.switchMod(mod, enable) },
        onLongClick = { mod -> modListViewModel.modLongClick(mod) },
        onMultiSelectClick = { mod -> modListViewModel.modMultiSelectClick(mod) },
        modListViewModel = modListViewModel,
        modOperationViewModel = modOperationViewModel,
        modDetailViewModel = modDetailViewModel
    )
}

@Composable
fun NoMod() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.mod_page_no_mod),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

