package top.laoxin.modmanager.ui.view.modview

import androidx.compose.runtime.Composable
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel

@Composable
fun  DisableModPage(viewModel: ModViewModel, uiState: ModUiState) {
    if (uiState.disableModList.isEmpty()){
        NoMod()
        return
    }
    ModList(
        mods = uiState.disableModList,
        showDialog = viewModel::openModDetail,
        enableMod = viewModel::switchMod,
        modSwitchEnable = uiState.modSwitchEnable
    )
}