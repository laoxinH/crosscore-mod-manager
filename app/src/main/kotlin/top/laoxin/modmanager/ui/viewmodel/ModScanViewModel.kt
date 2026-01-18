package top.laoxin.modmanager.ui.viewmodel

import android.app.Application
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
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.repository.ScanStateRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.usercase.mod.DeleteModsUserCase
import top.laoxin.modmanager.domain.usercase.mod.ScanAndSyncModsUseCase
import top.laoxin.modmanager.notification.AppNotificationManager
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.observer.FlashObserverInterface
import top.laoxin.modmanager.service.ScanForegroundService
import top.laoxin.modmanager.ui.state.ModScanUiState
import top.laoxin.modmanager.ui.state.PermissionRequestState
import top.laoxin.modmanager.ui.state.PermissionType
import top.laoxin.modmanager.ui.state.SnackbarManager

/** Mod扫描和刷新ViewModel */
@HiltViewModel
class ModScanViewModel
@Inject
constructor(
        private val application: Application,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val modRepository: ModRepository,
        private val scanAndSyncUseCase: ScanAndSyncModsUseCase,
        private val deleteModsUserCase: DeleteModsUserCase,
        private val flashModsObserverManager: FlashModsObserverManager,
        private val permissionService: PermissionService,
        private val snackbarManager: SnackbarManager,
        private val appNotificationManager: AppNotificationManager,
        private val scanStateRepository: ScanStateRepository,
) : ViewModel(), FlashObserverInterface {

    private val _internalState = MutableStateFlow(InternalState())

    // 从 ScanStateRepository 获取扫描状态
    private val _scanProgress = scanStateRepository.scanState

    // 记录当前扫描是否为强制扫描
    private var isForceScan: Boolean = false

    // 权限请求状态
    private val _permissionState = MutableStateFlow(PermissionRequestState())
    val permissionState: StateFlow<PermissionRequestState> = _permissionState.asStateFlow()

    val uiState: StateFlow<ModScanUiState> =
            combine(
                            userPreferencesRepository.selectedGame,
                            _internalState,
                            scanStateRepository.scanState
                    ) { selectedGame, internalState, scanProgress ->
                        ModScanUiState(
                                isLoading = internalState.isLoading,
                                loadingPath = internalState.loadingPath,
                                allMods =
                                        modRepository
                                                .getModsByGamePackageName(selectedGame.packageName)
                                                .first(),
                                enableMods =
                                        modRepository
                                                .getEnableMods(selectedGame.packageName)
                                                .first(),
                                disableMods =
                                        modRepository
                                                .getDisableMods(selectedGame.packageName)
                                                .first(),
                                openPermissionRequestDialog =
                                        internalState.openPermissionRequestDialog,
                                requestPermissionPath = internalState.requestPermissionPath,
                                showDisEnableModsDialog = internalState.showDisEnableModsDialog,
                                delEnableModsList = internalState.delEnableModsList,
                                showForceScanDialog = internalState.showForceScanDialog,
                                scanProgress = scanProgress
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = ModScanUiState(isLoading = true)
                    )

    private var delModsList = emptyList<ModBean>()

    init {
        flashModsObserverManager.startWatching()
    }

    /** 扫描Mods - 通过 Foreground Service 执行 */
    fun flashMods(isLoading: Boolean = true, forceScan: Boolean = false) {
        // 检查是否已有扫描任务在运行
        val currentScanState = scanStateRepository.scanState.value
        if (currentScanState != null && currentScanState.isScanning) {
            snackbarManager.showMessageAsync(R.string.scan_task_already_exists)
            return
        }

        // 保存当前扫描模式
        isForceScan = forceScan

        // 启动 Foreground Service 执行扫描
        ScanForegroundService.startScan(application, forceScan)
    }

    /** 处理扫描状态 */
    /*private fun handleScanState(state: ScanState) {
        when (state) {
            is ScanState.Preparing -> {
                val progressState =
                        ScanProgressState(
                                isScanning = true,
                                isBackgroundMode = isBackgroundMode,
                                step = ScanStep.LISTING_FILES,
                                sourceName = state.message,
                                progress = 0f
                        )
                _scanProgress.update { progressState }
                if (isBackgroundMode) {
                    appNotificationManager.showScanProgress(progressState)
                }
            }
            is ScanState.Progress -> {
                _scanProgress.update { current ->
                    val updated =
                            (current ?: ScanProgressState()).copy(
                                    isBackgroundMode = isBackgroundMode,
                                    step = state.step,
                                    sourceName = state.sourceName,
                                    currentFile = state.currentFile,
                                    progress = state.overallPercent,
                                    current = state.current,
                                    total = state.total,
                                    subProgress = state.subProgress
                            )
                    if (isBackgroundMode) {
                        appNotificationManager.showScanProgress(updated)
                    }
                    updated
                }
            }
            is ScanState.ModFound -> {
                _scanProgress.update { current ->
                    (current ?: ScanProgressState()).copy(
                            foundModsCount = (current?.foundModsCount ?: 0) + 1
                    )
                }
            }
            is ScanState.TransferComplete -> {
                Log.d(
                        "ModScanViewModel",
                        "Transfer complete: ${state.result.transferredCount} files"
                )
            }
            is ScanState.ScanComplete -> {
                _scanProgress.update { current ->
                    (current ?: ScanProgressState()).copy(foundModsCount = state.modsCount)
                }
            }
            is ScanState.Success -> {
                handleScanSuccess(state)
            }
            is ScanState.Error -> {
                handleScanError(state)
            }
        }
    }*/

    /*    */
    /** 处理扫描成功 */
    /*
    private fun handleScanSuccess(state: ScanState.Success) {
        val result = state.result
        val hasDeletedEnabledMods = result.deletedEnabledMods.isNotEmpty()

        // 重置后台模式
        val wasBackgroundMode = isBackgroundMode
        isBackgroundMode = false

        // 后台模式：显示完成通知
        if (wasBackgroundMode) {
            appNotificationManager.showScanComplete(
                    ScanResultState(
                            scannedCount = result.scannedCount,
                            addedCount = result.addedCount,
                            updatedCount = result.updatedCount,
                            deletedCount = result.deletedCount,
                            skippedCount = result.skippedCount,
                            transferredCount = result.transferredCount,
                            errors = result.errors,
                            deletedEnabledMods = result.deletedEnabledMods
                    )
            )
            _scanProgress.update { null }
            return
        }

        // 前台模式：普通扫描且没有已删除但启用的MOD，用snackbar简略通知
        if (!isForceScan && !hasDeletedEnabledMods) {
            _scanProgress.update { null }
            snackbarManager.showMessageAsync(
                    R.string.toast_flash_complate,
                    result.addedCount.toString(),
                    result.updatedCount.toString(),
                    result.deletedCount.toString()
            )
        } else {
            // 强制扫描或存在已删除但启用的MOD，显示完整结果页面
            _scanProgress.update { current ->
                (current ?: ScanProgressState()).copy(
                        isScanning = false,
                        isBackgroundMode = false,
                        progress = 1f,
                        result =
                                ScanResultState(
                                        scannedCount = result.scannedCount,
                                        addedCount = result.addedCount,
                                        updatedCount = result.updatedCount,
                                        deletedCount = result.deletedCount,
                                        skippedCount = result.skippedCount,
                                        transferredCount = result.transferredCount,
                                        errors = result.errors,
                                        deletedEnabledMods = result.deletedEnabledMods
                                )
                )
            }
        }
    }

    */
    /** 处理扫描错误 */
    /*
    private fun handleScanError(state: ScanState.Error) {
        val wasBackgroundMode = isBackgroundMode
        isBackgroundMode = false

        // 后台模式：显示错误通知
        if (wasBackgroundMode) {
            appNotificationManager.showScanError()
            _scanProgress.update { null }
            return
        }

        // 前台模式：显示错误UI
        val error = state.error
        _scanProgress.update { current ->
            (current ?: ScanProgressState()).copy(isScanning = false, error = error)
        }
    }*/

    /** 取消扫描 */
    fun cancelScan() {
        ScanForegroundService.cancel(application)
        snackbarManager.showMessageAsync(R.string.scan_progress_cancel)
    }

    // 记录是否有待处理的后台切换请求
    private var pendingBackgroundSwitch: Boolean = false

    /** 切换到后台扫描模式 - 检查权限并切换 */
    fun switchToBackground() {
        // 检查是否有通知权限
        if (appNotificationManager.hasNotificationPermission()) {
            // 已有权限，直接切换
            performBackgroundSwitch()
        } else {
            // 没有权限，记录待处理状态并请求权限
            pendingBackgroundSwitch = true
            _permissionState.update {
                PermissionRequestState(
                        showDialog = true,
                        permissionType = PermissionType.NOTIFICATION
                )
            }
        }
    }

    /** 实际执行后台切换 */
    private fun performBackgroundSwitch() {
        // 通知 Service 切换到后台模式
        ScanForegroundService.switchToBackground(application)
    }

    /** 检查是否有通知权限 */
    fun hasNotificationPermission(): Boolean = appNotificationManager.hasNotificationPermission()

    /** 关闭扫描结果对话框 */
    fun dismissScanResult() {
        scanStateRepository.clear()
    }

    /** 从错误对话框请求权限 */
    fun requestPermissionFromError() {
        // 关闭扫描进度
        //  scanJob?.cancel()
        //   scanJob = null
        // 获取游戏路径并弹出权限对话框
        val gamePath = userPreferencesRepository.selectedGameValue.gamePath
        _scanProgress.value?.error?.let { error ->
            //  _scanProgress.update { null }
            Log.d("ModScanViewModel", "权限缺失类型: $error")
            when (error) {
                is AppError.PermissionError.StoragePermissionDenied -> {

                    return showPermissionDialog(gamePath, PermissionType.STORAGE)
                }
                is AppError.PermissionError.UriPermissionNotGranted -> {
                    return showPermissionDialog(gamePath, PermissionType.URI_SAF)
                }
                else -> {}
            }
        }
        //  _scanProgress.update { null }
        showPermissionDialog(gamePath, PermissionType.URI_SAF)
    }

    /** 确认删除Mods */
    fun confirmDeleteMods() {
        viewModelScope.launch {
            deleteModsUserCase(delModsList)
            _internalState.update {
                it.copy(showDisEnableModsDialog = false, delEnableModsList = emptyList())
            }
        }
    }

    /** 文件变化监听回调 */
    override fun onFlash() {
        Log.d("ModScanViewModel", "onFlash: 检测到文件变化，自动刷新mods")
        flashMods(isLoading = true, forceScan = false)
    }

    fun setOpenPermissionRequestDialog(show: Boolean) {
        _internalState.update { it.copy(openPermissionRequestDialog = show) }
    }

    fun setShowForceScanDialog(show: Boolean) {
        _internalState.update { it.copy(showForceScanDialog = show) }
    }

    fun setShowDisEnableModsDialog(show: Boolean) {
        _internalState.update { it.copy(showDisEnableModsDialog = show) }
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
        if (permissionType == PermissionType.NOTIFICATION) {
            // 如果有待处理的后台切换，执行切换
            if (pendingBackgroundSwitch) {
                pendingBackgroundSwitch = false
                performBackgroundSwitch()
            }
        }
    }

    /** 权限拒绝回调 */
    fun onPermissionDenied(permissionType: PermissionType) {
        _permissionState.update { PermissionRequestState() }
       // snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
        if (permissionType == PermissionType.NOTIFICATION) {
            // 如果有待处理的后台切换，执行切换
            pendingBackgroundSwitch = false
        }
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

    override fun onCleared() {
        super.onCleared()
        // scanJob?.cancel()
        flashModsObserverManager.stopWatching()
    }

    data class InternalState(
            val isLoading: Boolean = false,
            val loadingPath: String = "",
            val openPermissionRequestDialog: Boolean = false,
            val requestPermissionPath: String = "",
            val showDisEnableModsDialog: Boolean = false,
            val delEnableModsList: List<ModBean> = emptyList(),
            val showForceScanDialog: Boolean = false
    )
}
