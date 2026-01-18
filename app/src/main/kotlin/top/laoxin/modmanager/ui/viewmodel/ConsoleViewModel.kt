package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.AppInfoService
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.usercase.app.CheckUpdateUserCase
import top.laoxin.modmanager.domain.usercase.app.GetCurrentInformationUserCase
import top.laoxin.modmanager.domain.usercase.app.UpdateLogServiceUserCase
import top.laoxin.modmanager.domain.usercase.console.CheckInstallModUseCase
import top.laoxin.modmanager.domain.usercase.console.GetCurrentGameAntiHarmonyStateUserCase
import top.laoxin.modmanager.domain.usercase.console.SwitchAntiHarmonyUserCase
import top.laoxin.modmanager.domain.usercase.mod.GetGameEnableModsCountUserCase
import top.laoxin.modmanager.domain.usercase.mod.GetGameModsCountUserCase
import top.laoxin.modmanager.ui.state.ConsoleUiState
import top.laoxin.modmanager.ui.state.PermissionRequestState
import top.laoxin.modmanager.ui.state.PermissionType
import top.laoxin.modmanager.ui.state.SnackbarManager
import top.laoxin.modmanager.ui.state.UserPreferencesState

@HiltViewModel
class ConsoleViewModel
@Inject
constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val checkUpdateUserCase: CheckUpdateUserCase,
        private val getCurrentInformationUserCase: GetCurrentInformationUserCase,
        private val checkInstallModUseCase: CheckInstallModUseCase,
        private val updateLogServiceUserCase: UpdateLogServiceUserCase,
        private val getCurrentGameAntiHarmonyStateUserCase: GetCurrentGameAntiHarmonyStateUserCase,
        private val switchAntiHarmonyUserCase: SwitchAntiHarmonyUserCase,
        private val getGameModsCountUserCase: GetGameModsCountUserCase,
        private val getGameEnableModsCountUserCase: GetGameEnableModsCountUserCase,
        private val permissionService: PermissionService,
        private val snackbarManager: SnackbarManager,
        private val appInfoService: AppInfoService,
) : ViewModel() {

    // 内部 UI 状态
    private val _uiState = MutableStateFlow(ConsoleUiState())

    // 权限请求状态
    private val _permissionState = MutableStateFlow(PermissionRequestState())
    val permissionState: StateFlow<PermissionRequestState> = _permissionState.asStateFlow()

    private val userPreferencesState: StateFlow<UserPreferencesState> =
            combine(
                            userPreferencesRepository.selectedGame,
                            userPreferencesRepository.scanQQDirectory,
                            userPreferencesRepository.selectedDirectory,
                            userPreferencesRepository.scanDownload,
                            combine(
                                    userPreferencesRepository.scanDirectoryMods,
                                    userPreferencesRepository.deleteUnzipDirectory,
                                    userPreferencesRepository.showCategoryView,
                                    userPreferencesRepository.conflictDetectionEnabled
                            ) { scanMods, delUnzip, category, conflictDetection ->
                                object {
                                    val scanMods = scanMods
                                    val delUnzip = delUnzip
                                    val category = category
                                    val conflictDetection = conflictDetection
                                }
                            }
                    ) { game, qq, dir, download, settings ->
                        UserPreferencesState(
                                selectedGame = game,
                                scanQQDirectory = qq,
                                selectedDirectory = dir,
                                scanDownload = download,
                                scanDirectoryMods = settings.scanMods,
                                delUnzipDictionary = settings.delUnzip,
                                showCategoryView = settings.category,
                                conflictDetectionEnabled = settings.conflictDetection
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = UserPreferencesState()
                    )

    val uiState: StateFlow<ConsoleUiState> =
            combine(
                            _uiState,
                            getCurrentGameAntiHarmonyStateUserCase(),
                            userPreferencesState,
                            combine(
                                    getGameModsCountUserCase(),
                                    getGameEnableModsCountUserCase(),
                                    checkInstallModUseCase()
                            ) { modCount, enableCount, canInstall ->
                                Triple(modCount, enableCount, canInstall)
                            },
                    ) { uiState, antiHarmonyBean, prefs, lastThree ->
                        ConsoleUiState(
                                infoBean = uiState.infoBean,
                                updateInfo = uiState.updateInfo,
                                showInfoDialog = uiState.showInfoDialog,
                                showUpgradeDialog = uiState.showUpgradeDialog,
                                gameInfo = prefs.selectedGame,
                                canInstallMod = lastThree.third,
                                showScanDirectoryModsDialog = uiState.showScanDirectoryModsDialog,
                                openPermissionRequestDialog = uiState.openPermissionRequestDialog,
                                requestPermissionPath = uiState.requestPermissionPath,
                                modCount = lastThree.first,
                                enableModCount = lastThree.second,
                                antiHarmony = antiHarmonyBean?.isEnable ?: false,
                                scanQQDirectory = prefs.scanQQDirectory,
                                selectedDirectory = prefs.selectedDirectory,
                                scanDownload = prefs.scanDownload,
                                scanDirectoryMods = prefs.scanDirectoryMods,
                                delUnzipDictionary = prefs.delUnzipDictionary,
                                showCategoryView = prefs.showCategoryView,
                                conflictDetectionEnabled = prefs.conflictDetectionEnabled,
                                isLoading = false
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = ConsoleUiState(isLoading = true)
                    )

    init {
        checkStoragePermission()
        checkUpdate()
        getNewInfo()
        viewModelScope.launch {
            // reLoadGameConfigUserCase()
            userPreferencesState.collectLatest { prefs ->
                updateLogServiceUserCase(prefs.selectedDirectory)
                checkInstallMod(prefs.selectedGame.gamePath)
            }
        }
    }

    /** 检查全局存储权限 如果没有权限则显示权限请求对话框 */
    private fun checkStoragePermission() {
        if (!permissionService.hasStoragePermission()) {
            _permissionState.update {
                PermissionRequestState(
                        showDialog = true,
                        requestPath = "",
                        permissionType = PermissionType.STORAGE
                )
            }
        }
    }

    private fun checkUpdate() {
        viewModelScope.launch {
            checkUpdateUserCase(BuildConfig.VERSION_NAME).let { updateInfo ->
                _uiState.update { it.copy(updateInfo = updateInfo, showUpgradeDialog = true) }
            }
        }
    }

    private fun getNewInfo() {
        viewModelScope.launch {
            getCurrentInformationUserCase()?.let { info ->
                if (info.version > userPreferencesRepository.cachedInformationVision.first()) {
                    _uiState.update { it.copy(infoBean = info, showInfoDialog = true) }
                }
            }
        }
    }

    private fun checkInstallMod(gamePath: String) {
        val checkPermission = permissionService.getFileAccessType(gamePath)
        val canInstall = checkPermission != FileAccessType.NONE && gamePath.isNotEmpty()
        _uiState.update { it.copy(canInstallMod = canInstall) }
    }

    fun openAntiHarmony(flag: Boolean) {
        viewModelScope.launch {
            switchAntiHarmonyUserCase(flag).onSuccess { /* 成功处理 */}.onError { error ->
                when (error) {
                    is AppError.AntiHarmonyError.NotSupported ->
                            snackbarManager.showMessage(R.string.toast_game_not_suppose_anti)
                    // ToastUtils.longCall(R.string.toast_game_not_suppose_anti)
                    is AppError.PermissionError.StoragePermissionDenied ->
                            showPermissionDialog(_uiState.value.gameInfo.gamePath)
                    is AppError.PermissionError.UriPermissionNotGranted ->
                            showPermissionDialog(_uiState.value.gameInfo.gamePath)
                    else -> snackbarManager.showMessage(R.string.toast_unknow_err, error)
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
    fun onPermissionGranted(permissionType: PermissionType) {
        _permissionState.update { PermissionRequestState() }
        snackbarManager.showMessageAsync(R.string.toast_permission_granted)
        // TODO: 重试之前的操作
    }

    /** 权限拒绝回调 */
    fun onPermissionDenied(permissionType: PermissionType) {
        _permissionState.update { PermissionRequestState() }
       // snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
    }

    /** 请求 Shizuku 权限 */
    fun requestShizukuPermission() {
        viewModelScope.launch {
            when (val result = permissionService.requestShizukuPermission()) {
                is Result.Success -> {
                    if (result.data) {
                        snackbarManager.showMessageAsync(R.string.toast_permission_granted)
                    } else {
                        snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
                    }
                }
                is Result.Error -> {
                    snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
                }
            }
        }
    }

    /** Shizuku 是否可用 */
    fun isShizukuAvailable(): Boolean = permissionService.isShizukuAvailable()

    fun setScanQQDirectory(scan: Boolean) {
        val path = PathConstants.SCAN_PATH_QQ
        if (permissionService.getFileAccessType(path) != FileAccessType.NONE) {
            viewModelScope.launch { userPreferencesRepository.saveScanQQDirectory(scan) }
        } else {
            showPermissionDialog(path)
        }
    }

    fun setSelectedDirectory(selectedDirectory: String) {
        viewModelScope.launch {
            userPreferencesRepository
                    .prepareAndSetModDirectory(selectedDirectory)
                    .onSuccess {}
                    .onError {
                        when (it) {
                            is AppError.FileError.PermissionDenied ->
                                    snackbarManager.showMessage(
                                            R.string.toast_this_dir_has_no_prim_android
                                    )
                            else -> snackbarManager.showMessage(R.string.toast_unknow_err, it)
                        }
                    }
        }
    }

    fun switchScanDirectoryMods(scan: Boolean) {
        if (!scan) {
            if (uiState.value.showScanDirectoryModsDialog) {
                _uiState.update { it.copy(showScanDirectoryModsDialog = false) }
                viewModelScope.launch { userPreferencesRepository.saveScanDirectoryMods(scan) }
            } else {
                _uiState.update { it.copy(showScanDirectoryModsDialog = true) }
            }
        } else {
            viewModelScope.launch { userPreferencesRepository.saveScanDirectoryMods(scan) }
        }
    }

    fun setScanDownload(scan: Boolean) {
        viewModelScope.launch { userPreferencesRepository.saveScanDirectoryMods(scan) }
    }

    fun startGame() {
        appInfoService.startGame(userPreferencesRepository.selectedGameValue)
    }

    fun openUrl(context: Context, url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    // Dialog state setters
    fun setShowInfoDialog(show: Boolean) = _uiState.update { it.copy(showInfoDialog = show) }
    fun setShowUpgradeDialog(show: Boolean) = _uiState.update { it.copy(showUpgradeDialog = show) }
    fun setShowScanDirectoryModsDialog(show: Boolean) {
        Log.i("setShowScanDirectoryModsDialog", "扫描目录对话框显示: $show")
        _uiState.update { it.copy(showScanDirectoryModsDialog = show) }
    }

    fun setOpenPermissionRequestDialog(show: Boolean) =
            _uiState.update { it.copy(openPermissionRequestDialog = show) }

    fun switchDelUnzip(bool: Boolean) {
        viewModelScope.launch { userPreferencesRepository.saveDeleteUnzipDirectory(bool) }
    }

    fun setShowDelUnzipDialog(bool: Boolean) {
        _uiState.update { it.copy(showDeleteUnzipDialog = bool) }
    }

    fun getGameIcon(): ImageBitmap {
        return appInfoService.getAppIcon(uiState.value.gameInfo.packageName)
    }

    fun checkFileAccessType(): FileAccessType {
        return permissionService.getFileAccessType(_uiState.value.gameInfo.gamePath)
    }

    fun setShowCategoryView(bool: Boolean) {
        viewModelScope.launch { userPreferencesRepository.saveShowCategoryView(bool) }
    }

    fun switchScanQQDirectory(it: Boolean) {
        viewModelScope.launch { userPreferencesRepository.saveScanQQDirectory(it) }
    }

    fun switchScanDownloadDirectory(it: Boolean) {
        viewModelScope.launch { userPreferencesRepository.saveScanDownload(it) }
    }

    fun getGameVersion(gameInfo: GameInfoBean): String {
        return appInfoService.getVersionName(gameInfo.packageName).getOrDefault("")
    }

    fun switchConflictDetection(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.saveConflictDetectionEnabled(enabled) }
    }
}
