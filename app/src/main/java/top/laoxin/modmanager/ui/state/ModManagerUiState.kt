package top.laoxin.modmanager.ui.state

data class ModManagerUiState(
    val currentNavigationIndex: Int = 0,
    val openPermissionRequestDialog: Boolean = false
)
