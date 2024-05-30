package top.laoxin.modmanager.ui.view.modview

import androidx.compose.runtime.Composable
import top.laoxin.modmanager.ui.viewmodel.ModUiState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel

@Composable
fun SearchModPage(viewModel: ModViewModel, uiState: ModUiState) {
    if (uiState.searchModList.isEmpty()){
        NoMod()
        return
    }
    ModList(
        mods = uiState.searchModList,
        showDialog = viewModel::openModDetail,
        enableMod = viewModel::switchMod,
        modSwitchEnable = uiState.modSwitchEnable
    )
}