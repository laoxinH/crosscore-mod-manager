package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.DownloadGameConfigBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.bean.ThanksBean
import top.laoxin.modmanager.data.repository.UserPreferencesRepositoryImpl
import top.laoxin.modmanager.data.service.AppInfoService
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.AppDataRepository
import top.laoxin.modmanager.domain.repository.GameInfoRepository
import top.laoxin.modmanager.domain.repository.UpdateInfo
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.usercase.app.CheckUpdateUserCase
import top.laoxin.modmanager.domain.usercase.app.GetCurrentInformationUserCase
import top.laoxin.modmanager.ui.state.PermissionRequestState
import top.laoxin.modmanager.ui.state.PermissionType
import top.laoxin.modmanager.ui.state.SettingUiState
import top.laoxin.modmanager.ui.state.SnackbarManager

@HiltViewModel
class SettingViewModel
@Inject
constructor(
    private val gameInfoRepository: GameInfoRepository,
    private val appDataRepository: AppDataRepository,
    private val userPreferencesRepository: UserPreferencesRepositoryImpl,
    private val checkUpdateUserCase: CheckUpdateUserCase,
    private val getCurrentInformationUserCase: GetCurrentInformationUserCase,
    private val appInfoService: AppInfoService,
    private val permissionService: PermissionService,
    private val snackbarManager: SnackbarManager,
    private val fileService: FileService
) : ViewModel() {

    companion object {
        private const val TAG = "SettingViewModel"
    }

    private val _internalState = MutableStateFlow(InternalState())

    // 权限请求状态
    private val _permissionState = MutableStateFlow(PermissionRequestState())
    val permissionState: StateFlow<PermissionRequestState> = _permissionState.asStateFlow()
    val uiState: StateFlow<SettingUiState> =
        combine(
            _internalState,
            gameInfoRepository.getGameInfoList(),
            userPreferencesRepository.selectedGame
        ) { internal, gameList, currentGame ->
            SettingUiState(
                showDeleteBackupDialog = internal.showDeleteBackupDialog,
                showDeleteCacheDialog = internal.showDeleteCacheDialog,
                showAcknowledgmentsDialog = internal.showAcknowledgmentsDialog,
                showSwitchGameDialog = internal.showSwitchGameDialog,
                showGameTipsDialog = internal.showGameTipsDialog,
                showUpdateDialog = internal.showUpdateDialog,
                showDownloadGameConfigDialog =
                    internal.showDownloadGameConfigDialog,
                showNotificationDialog = internal.showNotificationDialog,
                openPermissionRequestDialog = internal.openPermissionRequestDialog,
                showAboutDialog = internal.showAboutDialog,
                thanksList = internal.thanksList,
                gameInfoList = gameList,
                currentGame = currentGame,
                targetGame = internal.targetGame,
                downloadGameConfigList = internal.downloadGameConfigList,
                infoBean = internal.infoBean,
                updateInfo = internal.updateInfo,
                versionName =
                    appInfoService
                        .getVersionName(appInfoService.getPackageName())
                        .getOrNull()
                        ?: "",
                requestPermissionPath = internal.requestPermissionPath,
                isLoading = false,
                isDownloading = internal.isDownloading,
                isAboutPage = internal.isAboutPage
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingUiState(isLoading = true)
            )

    init {
        checkUpdate()
        getNewInfo()
    }

    fun onSwitchGame(game: GameInfoBean) {
        val currentGame = uiState.value.currentGame
        if (game.packageName != currentGame.packageName) {
            viewModelScope.launch {

                val gameInfo =
                    gameInfoRepository.enrichGameInfo(
                        game,
                        PathConstants.getFullModPath(
                            userPreferencesRepository.selectedDirectory.first()
                        )
                    )
                userPreferencesRepository.saveSelectedGame(gameInfo)
                snackbarManager.showMessage(R.string.toast_setect_game_success,game.gameName,game.serviceName)
                _internalState.update {
                    it.copy(targetGame = null, showGameTipsDialog = false)
                }


            }
        }
    }




    fun clearCache() {
        viewModelScope.launch {
            val result = fileService.deleteFile(PathConstants.MODS_TEMP_PATH)
            when (result) {
                is Result.Success -> {
                    snackbarManager.showMessage("清理成功")
                }

                is Result.Error -> {
                    snackbarManager.showMessage("清理失败")
                }
            }
        }
        setShowDeleteCacheDialog(false)
    }

    fun getThanks() {
        viewModelScope.launch {
            when (val result = appDataRepository.getThanksList()) {
                is Result.Success -> {
                    _internalState.update { 
                        it.copy(thanksList = result.data, showAcknowledgmentsDialog = true) 
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "获取感谢名单失败: ${result.error}")
                    snackbarManager.showMessageAsync(R.string.thanks_list_fetch_failed)
                }
            }
        }
    }

    fun checkUpdate(autoCheck: Boolean = true) {
        viewModelScope.launch {
            checkUpdateUserCase(BuildConfig.VERSION_NAME, autoCheck).let { updateInfo ->
                _internalState.update { it.copy(updateInfo = updateInfo, showUpdateDialog = true) }
            }
        }
    }

    fun getNewInfo(autoCheck: Boolean = true) {
        viewModelScope.launch {
            val info = getCurrentInformationUserCase(autoCheck)
            _internalState.update {
                it.copy(infoBean = info, showNotificationDialog = info != null)
            }
        }
    }

    fun getDownloadGameConfig() {
        viewModelScope.launch {
            when (val result = gameInfoRepository.getRemoteGameConfigs()) {
                is Result.Success -> {
                    _internalState.update {
                        it.copy(
                            downloadGameConfigList = result.data,
                            showDownloadGameConfigDialog = true
                        )
                    }
                }

                is Result.Error -> {
                    snackbarManager.showMessageAsync(R.string.game_config_fetch_failed)
                }
            }
        }
    }

    fun installGameConfig(config: DownloadGameConfigBean) {
        viewModelScope.launch {
            _internalState.update { it.copy(isDownloading = true) }
            when (val result = gameInfoRepository.downloadRemoteGameConfig(config)) {
                is Result.Success -> {
                    snackbarManager.showMessageAsync(
                        R.string.game_config_added,
                        result.data.gameName
                    )
                    _internalState.update {
                        it.copy(isDownloading = false, showDownloadGameConfigDialog = false)
                    }
                }

                is Result.Error -> {
                    snackbarManager.showMessageAsync(R.string.game_config_download_failed)
                    _internalState.update { it.copy(isDownloading = false) }
                }
            }
        }
    }

    /** 获取应用图标 */
    fun getAppIcon(packageName: String) = appInfoService.getAppIcon(packageName)

    // Dialog state setters
    fun setShowDeleteBackupDialog(show: Boolean) =
        _internalState.update { it.copy(showDeleteBackupDialog = show) }

    fun setShowDeleteCacheDialog(show: Boolean) =
        _internalState.update { it.copy(showDeleteCacheDialog = show) }

    fun setShowAcknowledgmentsDialog(show: Boolean) =
        _internalState.update { it.copy(showAcknowledgmentsDialog = show) }

    fun setShowSwitchGameDialog(show: Boolean) =
        _internalState.update { it.copy(showSwitchGameDialog = show) }

    fun setShowGameTipsDialog(show: Boolean) =
        _internalState.update { it.copy(showGameTipsDialog = show) }

    fun setShowUpdateDialog(show: Boolean) =
        _internalState.update { it.copy(showUpdateDialog = show) }

    fun setShowDownloadGameConfigDialog(show: Boolean) =
        _internalState.update { it.copy(showDownloadGameConfigDialog = show) }

    fun setShowNotificationDialog(show: Boolean) =
        _internalState.update { it.copy(showNotificationDialog = show) }



    fun setAboutPage(bool: Boolean) = _internalState.update { it.copy(isAboutPage = bool) }
    fun setGameInfo(targetGame: GameInfoBean) {
        if (targetGame.packageName == userPreferencesRepository.selectedGameValue.packageName) return
        Log.d(TAG, "setGameInfo: $targetGame")
        if (permissionService.getFileAccessType(targetGame.gamePath) != FileAccessType.NONE) {
            //appInfoService.isAppInstalled(targetGame.packageName)
            if (appInfoService.isAppInstalled(targetGame.packageName).isSuccess) {
                if (targetGame.tips.isNotEmpty()) {
                    _internalState.update {
                        it.copy(
                            targetGame = targetGame,
                            showGameTipsDialog = true
                        )
                    }
                } else {
                    onSwitchGame(targetGame)
                }

            } else {
                //Log.d(TAG, "setGameInfo: app not installed")
                snackbarManager.showMessageAsync(
                    R.string.toast_set_game_info_failed,
                    targetGame.gameName, targetGame.serviceName
                )
            }
            //onSwitchGame(targetGame)
        } else {
            showPermissionDialog(targetGame.gamePath)
        }
    }



    fun reloadGameConfig() {
        viewModelScope.launch {
            val modPath = userPreferencesRepository.selectedDirectory.first()
            val customConfigPath = PathConstants.getFullModPath(modPath) + PathConstants.GAME_CONFIG_PATH
            
            when (val result = gameInfoRepository.importCustomGameConfigs(customConfigPath)) {
                is Result.Success -> {
                    val data = result.data
                    when {
                        data.successCount == 0 && data.failedCount == 0 -> {
                            snackbarManager.showMessageAsync(R.string.game_config_import_no_files)
                        }
                        data.failedCount == 0 -> {
                            snackbarManager.showMessageAsync(R.string.game_config_import_success, data.successCount)
                        }
                        else -> {
                            snackbarManager.showMessageAsync(
                                R.string.game_config_import_partial,
                                data.successCount,
                                data.failedCount
                            )
                        }
                    }
                }
                is Result.Error -> {
                    snackbarManager.showMessageAsync(R.string.game_config_import_failed)
                }
            }
        }
    }

    private fun showPermissionDialog(
        gamePath: String,
        permissionType: PermissionType = PermissionType.URI_SAF
    ) {
        _permissionState.update {
            PermissionRequestState(
                showDialog = true,
                requestPath = permissionService.getRequestPermissionPath(gamePath),
                permissionType = permissionType
            )
        }
    }

    /** 权限授予回调 */
    fun onPermissionGranted() {
        _permissionState.update { PermissionRequestState() }
        snackbarManager.showMessageAsync(R.string.toast_permission_granted)
        // TODO: 重试之前的操作
    }

    /** 权限拒绝回调 */
    fun onPermissionDenied() {
        _permissionState.update { PermissionRequestState() }
        snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
    }

    /** 请求 Shizuku 权限 */
    fun requestShizukuPermission() {
        val result = permissionService.requestShizukuPermission()
        result
            .onSuccess {
                // Shizuku 权限请求已发起，等待结果
            }
            .onError {
                snackbarManager.showMessageAsync(R.string.toast_shizuku_not_available)
                _permissionState.update { PermissionRequestState() }
            }
    }

    /** Shizuku 是否可用 */
    fun isShizukuAvailable(): Boolean = permissionService.isShizukuAvailable()

    // Internal state data class
    private data class InternalState(
        val showDeleteBackupDialog: Boolean = false,
        val showDeleteCacheDialog: Boolean = false,
        val showAcknowledgmentsDialog: Boolean = false,
        val showSwitchGameDialog: Boolean = false,
        val showGameTipsDialog: Boolean = false,
        val showUpdateDialog: Boolean = false,
        val showDownloadGameConfigDialog: Boolean = false,
        val showNotificationDialog: Boolean = false,
        val openPermissionRequestDialog: Boolean = false,
        val showAboutDialog: Boolean = false,
        val thanksList: List<ThanksBean> = emptyList(),
        val targetGame: GameInfoBean? = null,
        val downloadGameConfigList: List<DownloadGameConfigBean> = emptyList(),
        val infoBean: InfoBean? = null,
        val updateInfo: UpdateInfo? = null,
        val requestPermissionPath: String = "",
        val isAboutPage: Boolean = false,
        val isDownloading: Boolean = false,
    )
}
