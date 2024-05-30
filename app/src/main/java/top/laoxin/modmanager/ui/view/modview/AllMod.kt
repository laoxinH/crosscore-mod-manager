package top.laoxin.modmanager.ui.view.modview

import androidx.compose.runtime.Composable
import top.laoxin.modmanager.ui.viewmodel.ModUiState
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
    ModList(
        mods = uiState.modList,
        showDialog = viewModel::openModDetail,
        enableMod = viewModel::switchMod,
        modSwitchEnable = uiState.modSwitchEnable
    )
}