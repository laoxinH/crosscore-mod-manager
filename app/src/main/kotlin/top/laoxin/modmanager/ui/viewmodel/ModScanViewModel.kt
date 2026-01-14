package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.ScanStep
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.usercase.mod.ScanAndSyncModsUseCase
import top.laoxin.modmanager.domain.usercase.mod.ScanState
import top.laoxin.modmanager.domain.usercase.mod.DeleteModsUserCase
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.observer.FlashObserverInterface
import top.laoxin.modmanager.ui.state.ModScanUiState
import top.laoxin.modmanager.ui.state.PermissionRequestState
import top.laoxin.modmanager.ui.state.PermissionType
import top.laoxin.modmanager.ui.state.ScanProgressState
import top.laoxin.modmanager.ui.state.ScanResultState
import top.laoxin.modmanager.ui.state.SnackbarManager

/** Mod扫描和刷新ViewModel */
@HiltViewModel
class ModScanViewModel
@Inject
constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val modRepository: ModRepository,
        private val scanAndSyncUseCase: ScanAndSyncModsUseCase,
        private val deleteModsUserCase: DeleteModsUserCase,
        private val flashModsObserverManager: FlashModsObserverManager,
        private val permissionService: PermissionService,
        private val snackbarManager: SnackbarManager,
) : ViewModel(), FlashObserverInterface {

    private val _internalState = MutableStateFlow(InternalState())
    private val _scanProgress = MutableStateFlow<ScanProgressState?>(null)
 

    // 扫描 Job，支持取消
    private var scanJob: Job? = null

    // 权限请求状态
    private val _permissionState = MutableStateFlow(PermissionRequestState())
    val permissionState: StateFlow<PermissionRequestState> = _permissionState.asStateFlow()

    val uiState: StateFlow<ModScanUiState> =
            combine(userPreferencesRepository.selectedGame, _internalState, _scanProgress) {
                            selectedGame,
                            internalState,
                            scanProgress ->
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

    /** 扫描Mods（新实现） */
    fun flashMods(isLoading: Boolean = true, forceScan: Boolean = false) {
        // 如果已有扫描任务在运行，先取消
        scanJob?.cancel()

        scanJob =
                viewModelScope.launch {
                    scanAndSyncUseCase
                            .execute(scanExternalDirs = true, forceScan)
                            .onEach { state -> handleScanState(state) }
                            .launchIn(this)
                }
    }

    /** 处理扫描状态 */
    private fun handleScanState(state: ScanState) {
        when (state) {
            is ScanState.Preparing -> {
                _scanProgress.update {
                    ScanProgressState(
                            isScanning = true,
                            step = ScanStep.LISTING_FILES,
                            sourceName = state.message,
                            progress = 0f
                    )
                }
            }
            is ScanState.Progress -> {
             //   Log.d("ModScanViewModel", "收集的状态Progress: $state")
                _scanProgress.update { current ->
                    (current ?: ScanProgressState()).copy(
                            step = state.step,
                            sourceName = state.sourceName,
                            currentFile = state.currentFile,
                            progress = state.overallPercent,
                            current = state.current,
                            total = state.total,
                            subProgress = state.subProgress
                    )
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
                    (current ?: ScanProgressState()).copy(
                            //  stepName = "同步数据库...",
                            foundModsCount = state.modsCount
                    )
                }
            }
            is ScanState.Success -> {
                val result = state.result
                _scanProgress.update { current ->
                    (current ?: ScanProgressState()).copy(
                            isScanning = false,
                            // stepName = "完成",
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
            is ScanState.Error -> {
                when (val error = state.error) {
                    is AppError.PermissionError -> {
                        // 权限错误：更新错误状态，用户需要点击按钮去授权
                        _scanProgress.update { current ->
                            (current ?: ScanProgressState()).copy(isScanning = false, error = error)
                        }
                    }
                    is AppError.GameError -> {
                        // 游戏错误（未选择/未安装）：直接显示错误信息
                        _scanProgress.update { current ->
                            (current ?: ScanProgressState()).copy(isScanning = false, error = error)
                        }
                    }
                    else -> {
                        // 其他错误：显示错误信息
                        _scanProgress.update { current ->
                            (current ?: ScanProgressState()).copy(isScanning = false, error = error)
                        }
                    }
                }
            }
        }
    }

    /** 取消扫描 */
    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _scanProgress.update { null }
        snackbarManager.showMessageAsync(R.string.scan_progress_cancel)
    }

    /** 关闭扫描结果对话框 */
    fun dismissScanResult() {
        _scanProgress.update { null }
    }

    /** 从错误对话框请求权限 */
    fun requestPermissionFromError() {
        // 关闭扫描进度
        scanJob?.cancel()
        scanJob = null
        // 获取游戏路径并弹出权限对话框
        val gamePath = userPreferencesRepository.selectedGameValue.gamePath
        _scanProgress.value?.error?.let { error ->
            _scanProgress.update { null }
            Log.d("ModScanViewModel", "权限缺失类型: $error")
            when (error) {
                is AppError.PermissionError.StoragePermissionDenied -> {

                    return showPermissionDialog(gamePath, PermissionType.STORAGE)
                }
                is AppError.PermissionError.UriPermissionNotGranted -> {
                    return showPermissionDialog(gamePath, PermissionType.URI_SAF)
                }
                else -> {

                }
            }
        }
        _scanProgress.update { null }
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
    fun onPermissionGranted() {
        _permissionState.update { PermissionRequestState() }
        snackbarManager.showMessageAsync(R.string.toast_permission_granted)
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

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
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
