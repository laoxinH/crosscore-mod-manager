package top.laoxin.modmanager.ui.view.modview

import androidx.compose.runtime.Composable
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel


@Composable
fun EnableModPage(viewModel: ModViewModel, uiState: ModUiState) {
    if (uiState.enableModList.isEmpty()){
        NoMod()
        return
    }
    ModList(
        mods = uiState.enableModList,
        showDialog = viewModel::openModDetail,
        enableMod = viewModel::switchMod,
        modSwitchEnable = uiState.modSwitchEnable
    )
}