package top.laoxin.modmanager.ui.view.modview

import android.util.Log
import androidx.compose.runtime.Composable
import kotlinx.coroutines.selects.whileSelect
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel

@Composable
fun AllModPage(
    viewModel: ModViewModel,
    uiState: ModUiState
) {
    if (uiState.modList.isEmpty()){
        NoMod()
        return
    }
    val modList = when (uiState.modsView) {
        NavigationIndex.ALL_MODS -> uiState.modList
        NavigationIndex.ENABLE_MODS -> uiState.enableModList
        NavigationIndex.DISABLE_MODS -> uiState.disableModList
        NavigationIndex.SEARCH_MODS -> uiState.searchModList
    }

    if (modList.isEmpty()){
        NoMod()
        return
    }
    ModList(
        mods = modList,
        showDialog = viewModel::openModDetail,
        enableMod = viewModel::switchMod,
        modSwitchEnable = uiState.modSwitchEnable
    )
}