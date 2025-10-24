package top.laoxin.modmanager.ui.view.modView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.viewmodel.ModViewModel

@Composable
fun AllModPage(
    viewModel: ModViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.modList.isEmpty()) {
        NoMod()
        return
    }
    
    val modList = when (uiState.modsView) {
        NavigationIndex.ALL_MODS -> uiState.modList
        NavigationIndex.ENABLE_MODS -> uiState.enableModList
        NavigationIndex.DISABLE_MODS -> uiState.disableModList
        NavigationIndex.SEARCH_MODS -> uiState.searchModList
        else -> uiState.modList
    }

    if (modList.isEmpty()) {
        NoMod()
        return
    }
    
    ModList(
        mods = modList,
        showDialog = viewModel::openModDetail,
        enableMod = viewModel::switchMod,
        modSwitchEnable = uiState.modSwitchEnable,
        isMultiSelect = uiState.isMultiSelect,
        modsSelected = uiState.modsSelected,
        onLongClick = viewModel::modLongClick,
        onMultiSelectClick = viewModel::modMultiSelectClick,
        modViewModel = viewModel
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

