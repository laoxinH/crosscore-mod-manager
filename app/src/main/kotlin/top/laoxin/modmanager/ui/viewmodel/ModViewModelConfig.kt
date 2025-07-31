// `app/src/main/kotlin/top/laoxin/modmanager/ui/viewmodel/ModViewModelConfig.kt`
package top.laoxin.modmanager.ui.viewmodel

import top.laoxin.modmanager.domain.usercase.gameinfo.UpdateGameInfoUserCase
import top.laoxin.modmanager.domain.usercase.mod.*
import top.laoxin.modmanager.domain.usercase.repository.*
import top.laoxin.modmanager.domain.usercase.userpreference.GetUserPreferenceUseCase
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import javax.inject.Inject

data class ModViewModelConfig @Inject constructor(
    val getUserPreferenceUseCase: GetUserPreferenceUseCase,
    val getGameEnableModsUserCase: GetGameEnableModsUserCase,
    val getGameAllModsUserCase: GetGameAllModsUserCase,
    val getGameDisEnableUserCase: GetGameDisEnableUserCase,
    val checkCanFlashModsUserCase: CheckCanFlashModsUserCase,
    val flashModsUserCase: FlashModsUserCase,
    val deleteModsUserCase: DeleteModsUserCase,
    val checkCanSwitchModsUserCase: CheckCanSwitchModsUserCase,
    val enableModsUserCase: EnableModsUserCase,
    val disableModsUserCase: DisableModsUserCase,
    val checkModPasswordUserCase: CheckModPasswordUserCase,
    val flashModImageUserCase: FlashModImageUserCase,
    val flashModDetailUserCase: FlashModDetailUserCase,
    val searchModsUserCase: SearchModsUserCase,
    val deleteSelectedModUserCase: DeleteSelectedModUserCase,
    val deleteModUserCase: DeleteModUserCase,
    val updateModUserCase: UpdateModUserCase,
    val updateGameInfoUserCase: UpdateGameInfoUserCase,
    val flashModsObserverManager: FlashModsObserverManager,
    val gameInfoManager: GameInfoManager,
    val appPathsManager: AppPathsManager,
    val permissionTools: PermissionTools,
    val fileToolsManager: FileToolsManager,
    val conflictDetectUserCase: ConflictDetectUserCase
)