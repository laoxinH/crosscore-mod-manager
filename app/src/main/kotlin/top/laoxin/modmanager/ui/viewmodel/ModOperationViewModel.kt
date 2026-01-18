package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.EnableStep
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.usercase.mod.DecryptModsUseCase
import top.laoxin.modmanager.domain.usercase.mod.DecryptState
import top.laoxin.modmanager.domain.usercase.mod.DeleteModsUseCase
import top.laoxin.modmanager.domain.usercase.mod.DeleteResult
import top.laoxin.modmanager.domain.usercase.mod.DeleteState
import top.laoxin.modmanager.domain.usercase.mod.DeleteStep
import top.laoxin.modmanager.domain.usercase.mod.EnableModsUseCase
import top.laoxin.modmanager.domain.usercase.mod.EnableResult
import top.laoxin.modmanager.domain.usercase.mod.EnableState
import top.laoxin.modmanager.listener.ProgressUpdateListener
import top.laoxin.modmanager.ui.state.DecryptProgressState
import top.laoxin.modmanager.ui.state.DeleteCheckState
import top.laoxin.modmanager.ui.state.DeleteProgressState
import top.laoxin.modmanager.ui.state.DeleteResultState
import top.laoxin.modmanager.ui.state.EnableProgressState
import top.laoxin.modmanager.ui.state.EnableResultState
import top.laoxin.modmanager.ui.state.ModOperationUiState
import top.laoxin.modmanager.ui.state.PermissionRequestState
import top.laoxin.modmanager.ui.state.PermissionType
import top.laoxin.modmanager.ui.state.SnackbarManager
import top.laoxin.modmanager.domain.model.Result

/** Mod操作ViewModel（启用/禁用/删除） */
@HiltViewModel
class ModOperationViewModel
@Inject
constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val snackbarManager: SnackbarManager,
    private val enableModsUseCase: EnableModsUseCase,
    private val deleteModsUseCase: DeleteModsUseCase,
    private val permissionService: PermissionService,
    private val decryptModsUseCase: DecryptModsUseCase,
) : ViewModel(), ProgressUpdateListener {

    companion object {
        private const val TAG = "ModOperationViewModel"
    }

    // 权限请求状态
    private val _permissionState = MutableStateFlow(PermissionRequestState())
    val permissionState: StateFlow<PermissionRequestState> = _permissionState.asStateFlow()

    private val _uiState = MutableStateFlow(ModOperationUiState())
    val uiState: StateFlow<ModOperationUiState> = _uiState.asStateFlow()

    private var enableJob: Job? = null
    private var decryptJob: Job? = null
    private var deleteJob: Job? = null

    /** 开启或关闭mod */
    fun switchMod(modBean: ModBean, enable: Boolean, isDel: Boolean = false) {
        // 检查是否有需要解密的 MOD
        if (modBean.isEncrypted && modBean.password.isEmpty() && enable) {
            // 显示密码输入对话框

            _uiState.update {
                it.copy(
                    showPasswordDialog = true,
                    passwordRequestMod = modBean,
                    pendingEnableAfterDecrypt = modBean,
                    passwordError = null
                )
            }
            return
        }
        handleOperation(listOf(modBean), enable, isDel)
    }

    /** 批量切换选中的Mods */
    fun switchSelectMods(mods: List<ModBean?>, enable: Boolean, silent: Boolean = false) {
        val validMods = mods.filterNotNull()
        if (validMods.isEmpty()) return
        handleOperation(validMods, enable, silent = silent)
    }

    private fun handleOperation(
        mods: List<ModBean>,
        enable: Boolean,
        isDel: Boolean = false,
        silent: Boolean = false
    ) {
        if (mods.isEmpty()) return

        // 检查是否有正在进行的开关任务
        if (enableJob?.isActive == true) {
            snackbarManager.showMessageAsync(R.string.operation_task_in_progress)
            return
        }

        val gameInfo = userPreferencesRepository.selectedGameValue
        if (gameInfo == GameInfoConstant.NO_GAME) {
            snackbarManager.showMessageAsync(R.string.toast_please_select_game)
            return
        }

        viewModelScope.launch {
            if (enable) {

                enableMods(mods, gameInfo)
            } else {
                disableMods(mods, gameInfo, silent)
            }
        }
    }

    /** 开启 MOD（使用新的 Flow-based UseCase） */
    private suspend fun enableMods(mods: List<ModBean>, gameInfo: GameInfoBean) {
        enableJob =
            viewModelScope.launch {
                enableModsUseCase.execute(mods, gameInfo).collect { state ->
                    when (state) {
                        is EnableState.Progress -> {
                            _uiState.update {
                                it.copy(
                                    modSwitchEnable = false,
                                    enableProgress =
                                        EnableProgressState(
                                            isProcessing = true,
                                            step = state.step,
                                            modName = state.modName,
                                            currentFile = state.message,
                                            progress =
                                                if (state.total > 0)
                                                    state.current
                                                        .toFloat() /
                                                            state.total
                                                else 0f,
                                            current = state.current,
                                            total = state.total,
                                            subProgress = state.subProgress
                                        )
                                )
                            }
                        }

                        is EnableState.Success -> {
                            handleEnableSuccess(state.result, false)
                        }

                        is EnableState.Error -> {
                            handleEnableError(state.error, gameInfo)
                        }

                        is EnableState.Cancel -> {
                            handleEnableSuccess(state.result, true)
                        }
                    }
                }
            }
    }

    /**
     * 关闭 MOD（使用新的 Flow-based UseCase）
     * @param silent 静默模式：不显示进度覆盖层，只显示 Snackbar 提示
     */
    private suspend fun disableMods(
        mods: List<ModBean>,
        gameInfo: GameInfoBean,
        silent: Boolean = false
    ) {
        enableJob =
            viewModelScope.launch {
                enableModsUseCase.disable(mods, gameInfo).collect { state ->
                    when (state) {
                        is EnableState.Progress -> {
                            // 静默模式下跳过进度UI更新
                            if (!silent) {
                                _uiState.update {
                                    it.copy(
                                        modSwitchEnable = false,
                                        enableProgress =
                                            EnableProgressState(
                                                isProcessing = true,
                                                step = state.step,
                                                modName = state.modName,
                                                currentFile = state.message,
                                                progress =
                                                    if (state.total > 0)
                                                        state.current
                                                            .toFloat() /
                                                                state.total
                                                    else 0f,
                                                current = state.current,
                                                total = state.total,
                                                subProgress = state.subProgress
                                            )
                                    )
                                }
                            }
                        }

                        is EnableState.Success -> {
                            handleDisableSuccess(state.result, false, silent)
                        }

                        is EnableState.Error -> {
                            // 即使静默模式下也需要显示错误界面
                            handleEnableError(state.error, gameInfo)
                        }

                        is EnableState.Cancel -> {
                            handleDisableSuccess(state.result, true, silent)
                        }
                    }
                }
            }
    }

    /** 处理开启成功结果 */
    private fun handleEnableSuccess(result: EnableResult, isCancel: Boolean) {
        // 计算失败的 MOD 数量
        val failedCount =
            result.needPasswordMods.size +
                    result.fileMissingMods.size +
                    result.backupFailedMods.size +
                    result.enableFailedMods.size +
                    result.mutualConflictMods.size +
                    result.enabledConflictMods.size

        // 如果只有1个MOD成功开启且没有失败的MOD，使用Snackbar提示
        if (result.enabledCount == 1 && failedCount == 0 && !isCancel) {
            val modName = result.enabledSucceededMods.firstOrNull()?.name ?: "MOD"
            _uiState.update { it.copy(modSwitchEnable = true, enableProgress = null) }
            snackbarManager.showMessageAsync(R.string.toast_mod_enable_success, modName)
            return
        }

        // 多个MOD或有失败的情况，显示结果卡片
        _uiState.update {
            it.copy(
                modSwitchEnable = true,
                enableProgress =
                    EnableProgressState(
                        isProcessing = false,
                        step = EnableStep.COMPLETE,
                        result =
                            EnableResultState(
                                enabledCount = result.enabledCount,
                                needPasswordMods = result.needPasswordMods,
                                fileMissingMods = result.fileMissingMods,
                                backupFailedMods = result.backupFailedMods,
                                enableFailedMods = result.enableFailedMods,
                                restoreFailedMods = result.restoreFailedMods,
                                mutualConflictMods = result.mutualConflictMods,
                                enabledConflictMods = result.enabledConflictMods
                            )
                    )
            )
        }

        /*        // 显示 Toast
        val failedCount =
                result.needPasswordMods.size +
                        result.fileMissingMods.size +
                        result.backupFailedMods.size +
                        result.enableFailedMods.size +
                        result.mutualConflictMods.size +
                        result.enabledConflictMods.size

        if (isCancel) {
            snackbarManager.showMessageAsync(
                    R.string.toast_swtch_cancel_mods_result,
                    result.enabledCount.toString(),
                    failedCount.toString()
            )
        } else {
            snackbarManager.showMessageAsync(
                    R.string.toast_swtch_mods_result,
                    result.enabledCount.toString(),
                    failedCount.toString()
            )
        }*/
    }

    /**
     * 处理关闭成功结果
     * @param silent 静默模式：不更新进度UI，只显示 Snackbar
     */
    private fun handleDisableSuccess(
        result: EnableResult,
        isCancel: Boolean,
        silent: Boolean = false
    ) {
        val failedCount = result.restoreFailedMods.size

        // 如果只有1个MOD成功关闭且没有失败的MOD，使用Snackbar提示
        if (result.enabledCount == 1 && failedCount == 0 && !isCancel) {
            val modName = result.disabledSucceededMods.firstOrNull()?.name ?: "MOD"
            _uiState.update { it.copy(modSwitchEnable = true, enableProgress = null) }
            snackbarManager.showMessageAsync(R.string.toast_mod_disable_success, modName)
            return
        }

        // 静默模式下跳过UI更新
        if (!silent) {
            _uiState.update {
                it.copy(
                    modSwitchEnable = true,
                    enableProgress =
                        EnableProgressState(
                            isProcessing = false,
                            step = EnableStep.COMPLETE,
                            result =
                                EnableResultState(
                                    enabledCount = result.enabledCount,
                                    restoreFailedMods = result.restoreFailedMods
                                )
                        )
                )
            }
        } else {
            // 静默模式下只需恢复开关状态
            _uiState.update { it.copy(modSwitchEnable = true) }
        }

 /*       if (isCancel) {
            snackbarManager.showMessageAsync(
                R.string.toast_swtch_cancel_mods_result,
                result.enabledCount.toString(),
                result.restoreFailedMods.size.toString()
            )
        } else {
            snackbarManager.showMessageAsync(
                R.string.toast_swtch_mods_result,
                result.enabledCount.toString(),
                result.restoreFailedMods.size.toString()
            )
        }*/
    }

    /** 处理错误 - 显示错误对话框，用户点击按钮后再进行相应操作 */
    private fun handleEnableError(error: AppError, gameInfo: GameInfoBean) {
        // 直接显示错误对话框，用户需要点击按钮进行操作
        _uiState.update {
            it.copy(
                modSwitchEnable = true,
                enableProgress = EnableProgressState(isProcessing = false, error = error)
            )
        }
    }

    /** 取消当前操作 */
    fun cancelOperation() {
        /*enableJob?.cancel()
        _uiState.update { it.copy(modSwitchEnable = true, enableProgress = null) }*/
        _uiState.update {
            it.copy(
                modSwitchEnable = false,
                enableProgress =
                    EnableProgressState(
                        isProcessing = true,
                        step = EnableStep.CANCELING,
                    )
            )
        }
        enableModsUseCase.cancel()
    }

    /** 关闭进度对话框 */
    fun dismissEnableProgress() {
        _uiState.update { it.copy(enableProgress = null) }
    }

    /** 从错误对话框请求权限 */
    fun requestPermissionFromEnableError() {
        val gamePath = userPreferencesRepository.selectedGameValue.gamePath
        _uiState.value.enableProgress?.error?.let { error ->
            _uiState.update { it.copy(enableProgress = null) }
            when (error) {
                is AppError.PermissionError.StoragePermissionDenied -> {
                    showPermissionDialog(gamePath, PermissionType.STORAGE)
                }

                is AppError.PermissionError.UriPermissionNotGranted -> {
                    showPermissionDialog(gamePath, PermissionType.URI_SAF)
                }

                is AppError.PermissionError -> {
                    showPermissionDialog(gamePath, PermissionType.URI_SAF)
                }

                else -> {
                    // 其他错误不需要权限操作
                }
            }
        }
    }

    /** 检查密码 */
    fun checkPassword(mod: ModBean, password: String) {
        // TODO: 实现密码验证逻辑
    }

    /** 删除选中的Mods */
    fun deleteSelectedMods(selectedIds: Set<Int>) {
        // TODO: 实现批量删除逻辑
    }

    /** 批量删除 MOD（使用新的 Flow-based UseCase） */
    fun deleteMods(mods: List<ModBean>, deleteIntegratedPackage: Boolean = false) {
        deleteJob?.cancel()
        deleteJob =
            viewModelScope.launch {
                deleteModsUseCase.execute(mods, deleteIntegratedPackage).collect { state ->
                    when (state) {
                        is DeleteState.Progress -> {
                            _uiState.update {
                                it.copy(
                                    deleteProgress =
                                        DeleteProgressState(
                                            isProcessing = true,
                                            step = state.step,
                                            modName = state.modName,
                                            progress = state.progress,
                                            current = state.current,
                                            total = state.total
                                        )
                                )
                            }
                        }

                        is DeleteState.Success -> {
                            handleDeleteSuccess(state.result)
                        }

                        is DeleteState.Error -> {
                            _uiState.update {
                                it.copy(
                                    deleteProgress =
                                        DeleteProgressState(
                                            isProcessing = false,
                                            error = state.error
                                        )
                                )
                            }
                        }

                        is DeleteState.Cancel -> {
                            _uiState.update { it.copy(deleteProgress = null) }
                            snackbarManager.showMessage("删除已取消")
                        }
                    }
                }
            }
    }

    /** 处理删除成功结果 */
    private fun handleDeleteSuccess(result: DeleteResult) {
        _uiState.update {
            it.copy(
                deleteProgress =
                    DeleteProgressState(
                        isProcessing = false,
                        step = DeleteStep.COMPLETED,
                        result =
                            DeleteResultState(
                                deletedCount = result.deletedMods.size + result.deletedEnabledMods.size,
                                deletedMods = result.deletedMods,
                                deletedEnabledMods = result.deletedEnabledMods,
                                skippedIntegratedMods =
                                    result.skippedIntegratedMods,
                                failedMods = result.failedMods
                            )
                    )
            )
        }
    }

    /** 取消删除操作 */
    fun cancelDelete() {
        deleteModsUseCase.cancel()
        deleteJob?.cancel()
        deleteJob = null
    }

    /** 关闭删除进度对话框 */
    fun dismissDeleteProgress() {
        _uiState.update { it.copy(deleteProgress = null) }
    }

    /** 检查并准备删除MOD（先检查整合包，再显示确认对话框） */
    fun checkAndDeleteMods(mods: List<ModBean>) {
        Log.d(TAG, "删除的mod数量: ${mods.size}")
        viewModelScope.launch {
            when (val result = deleteModsUseCase.checkBeforeDelete(mods)) {
                is Result.Success -> {
                    val checkResult = result.data
                    /* if (checkResult.hasIntegratedPackages) {*/
                    // 存在整合包，显示确认对话框
                    _uiState.update {
                        it.copy(deleteCheckState = DeleteCheckState(checkResult))
                    }
                    /*   } else {
                           // 没有整合包，直接删除
                           deleteMods(mods, deleteIntegratedPackage = false)
                       }*/
                }

                is Result.Error -> {
                    snackbarManager.showMessage("检查失败: ${result.error}")
                }
            }
        }
    }

    /** 确认删除所有（包括整合包中的其他MOD） */
    fun confirmDeleteAll() {
        val checkState = _uiState.value.deleteCheckState ?: return
        val mods = checkState.checkResult.selectedMods
        _uiState.update { it.copy(deleteCheckState = null) }
        deleteMods(mods, deleteIntegratedPackage = true)
    }

    /** 确认删除但跳过整合包 */
    fun confirmDeleteSkipIntegrated() {
        val checkState = _uiState.value.deleteCheckState ?: return
        val mods = checkState.checkResult.selectedMods
        _uiState.update { it.copy(deleteCheckState = null) }
        deleteMods(mods, deleteIntegratedPackage = false)
    }

    /** 关闭删除前检查对话框 */
    fun dismissDeleteCheck() {
        _uiState.update { it.copy(deleteCheckState = null) }
    }


    /** 进度更新回调 */
    override fun onProgressUpdate(progress: String) {
        // 旧的回调，保留兼容性
    }

    // Dialog状态设置方法
    fun setShowPasswordDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showPasswordDialog = show,
                passwordRequestMod = if (!show) null else it.passwordRequestMod
            )
        }
    }

    fun setShowOpenFailedDialog(show: Boolean) {
        _uiState.update { it.copy(showOpenFailedDialog = show) }
    }

    fun setShowDelSelectModsDialog(show: Boolean) {
        _uiState.update { it.copy(showDelSelectModsDialog = show) }
    }

    fun setShowDelModDialog(show: Boolean) {
        _uiState.update {
            it.copy(showDelModDialog = show, delModTarget = if (!show) null else it.delModTarget)
        }
    }

    fun setOpenPermissionRequestDialog(show: Boolean) {
        _uiState.update { it.copy(openPermissionRequestDialog = show) }
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
        snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
    }

    /** 请求 Shizuku 权限 */
    fun requestShizukuPermission() {
        viewModelScope.launch {
            // Log.d(TAG, "requestShizukuPermission: 发起权限请求")

            // 先获取当前缓存的数量，用于跳过旧值
            val resultDeferred = async {
                permissionService.shizukuPermissionResult.drop(1).first()
            }

            // 发起权限请求
            permissionService.requestShizukuPermission()

            // 等待新的结果
            val shizukuPermissionResult = resultDeferred.await()
            // Log.d(TAG, "requestShizukuPermission: 权限请求结果: $shizukuPermissionResult")

            if (shizukuPermissionResult) {
                snackbarManager.showMessageAsync(R.string.toast_permission_granted)
            } else {
                snackbarManager.showMessageAsync(R.string.toast_permission_not_granted)
            }
        }
    }

    /** Shizuku 是否可用 */
    fun isShizukuAvailable(): Boolean = permissionService.isShizukuAvailable()

    // ==================== 密码对话框相关 ====================

    /** 关闭密码输入对话框 */
    fun dismissPasswordDialog() {
        _uiState.update {
            it.copy(
                showPasswordDialog = false,
                passwordRequestMod = null,
                pendingEnableAfterDecrypt = null,
                passwordError = null
            )
        }
    }

    /** 提交密码进行解密 */
    fun submitPassword(password: String) {
        val mod = _uiState.value.passwordRequestMod ?: return
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "密码不能为空") }
            return
        }

        // 关闭对话框，显示解密进度
        _uiState.update {
            it.copy(
                showPasswordDialog = false,
                passwordError = null,
                decryptProgress = DecryptProgressState(isProcessing = true)
            )
        }

        decryptJob =
            viewModelScope.launch {
                decryptModsUseCase.execute(mod.path, password).collect { state ->
                    when (state) {
                        is DecryptState.Progress -> {
                            _uiState.update {
                                it.copy(
                                    decryptProgress =
                                        DecryptProgressState(
                                            isProcessing = true,
                                            step = state.step,
                                            modName = state.modName,
                                            current = state.current,
                                            total = state.total,
                                            progress =
                                                if (state.total > 0)
                                                    state.current
                                                        .toFloat() /
                                                            state.total
                                                else 0f
                                        )
                                )
                            }
                        }

                        is DecryptState.Success -> {
                            handleDecryptSuccess(mod, state.result.decryptedCount, password)
                        }

                        is DecryptState.Error -> {
                            handleDecryptError(state.error)
                        }
                    }
                }
            }
    }

    /** 取消解密操作 */
    fun cancelDecrypt() {
        decryptJob?.cancel()
        decryptJob = null
        _uiState.update { it.copy(decryptProgress = null, pendingEnableAfterDecrypt = null) }
    }

    /** 解密成功处理 */
    private fun handleDecryptSuccess(originalMod: ModBean, decryptedCount: Int, password: String) {
        val pendingMod = _uiState.value.pendingEnableAfterDecrypt

        _uiState.update {
            it.copy(
                decryptProgress =
                    DecryptProgressState(
                        isProcessing = false,
                        isComplete = true,
                        decryptedCount = decryptedCount,
                        pendingPassword = password,
                        pendingMod = pendingMod
                    ),
                pendingEnableAfterDecrypt = null
            )
        }
    }

    /** 用户确认解密成功后开启 MOD */
    fun confirmDecryptSuccess() {
        // 关闭进度对话框
        _uiState.update { it.copy(decryptProgress = null) }
    }

    /** 解密失败处理 */
    private fun handleDecryptError(error: AppError) {
        val errorMessage =
            when (error) {
                is AppError.ModError.DecryptFailed -> error.reason
                else -> "解密失败"
            }

        // 重新显示密码对话框，显示错误信息
        _uiState.update {
            it.copy(decryptProgress = null, showPasswordDialog = true, passwordError = errorMessage)
        }
    }

    /** 关闭解密进度对话框 */
    fun dismissDecryptProgress() {
        _uiState.update { it.copy(decryptProgress = null) }
    }
}
