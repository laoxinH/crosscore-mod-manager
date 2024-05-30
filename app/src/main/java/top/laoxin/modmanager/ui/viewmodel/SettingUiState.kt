package top.laoxin.modmanager.ui.viewmodel

import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.constant.GameInfoConstant

data class SettingUiState(
    // 删除备份对话框
    val deleteBackupDialog: Boolean = false,
    val showAcknowledgments: Boolean = false,
    val showSwitchGame: Boolean = false,
    val gameInfoList : List<GameInfoConstant> = emptyList()
)
