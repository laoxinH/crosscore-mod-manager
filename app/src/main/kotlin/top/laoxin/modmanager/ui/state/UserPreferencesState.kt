package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.domain.bean.GameInfoBean

data class UserPreferencesState(
    var scanQQDirectory: Boolean = false,
    var selectedDirectory: String = "",
    val scanDownload: Boolean = false,
    val installPath: String = "",
    val gameService: String = "",
    val selectedGame: GameInfoBean = GameInfoConstant.NO_GAME,
    val scanDirectoryMods: Boolean = true,
    val delUnzipDictionary: Boolean = false,
    val showCategoryView: Boolean = true,
)

