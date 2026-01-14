package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.service.DecryptStep
import top.laoxin.modmanager.domain.service.EnableStep
import top.laoxin.modmanager.domain.usercase.mod.DeleteCheckResult
import top.laoxin.modmanager.domain.usercase.mod.DeleteStep

/** Mod 操作相关的 UI 状态 */
data class ModOperationUiState(
        val modSwitchEnable: Boolean = true,
        val showPasswordDialog: Boolean = false,
        val passwordRequestMod: ModBean? = null, // 需要输入密码的Mod
        val passwordError: String? = null, // 密码错误提示
        val showTips: Boolean = false,
        val tipsText: String = "",
        val unzipProgress: String = "",
        val multitaskingProgress: String = "",
        val isSnackbarHidden: Boolean = true,
        val openFailedMods: List<ModBean> = emptyList(),
        val showOpenFailedDialog: Boolean = false,
        val showDelSelectModsDialog: Boolean = false,
        val showDelModDialog: Boolean = false,
        val delModTarget: ModBean? = null, // 将要删除的Mod
        val openPermissionRequestDialog: Boolean = false,
        val requestPermissionPath: String = "",
        // 开启/关闭进度状态
        val enableProgress: EnableProgressState? = null,
        // 解密进度状态
        val decryptProgress: DecryptProgressState? = null,
        // 解密后待开启的 MOD
        val pendingEnableAfterDecrypt: ModBean? = null,
        // 删除进度状态
        val deleteProgress: DeleteProgressState? = null,
        // 删除前检查状态（确认对话框）
        val deleteCheckState: DeleteCheckState? = null,
)

/** MOD 开启进度状态 */
data class EnableProgressState(
        val isProcessing: Boolean = true,
        val step: EnableStep = EnableStep.VALIDATING,
        val modName: String = "",
        val currentFile: String = "",
        val progress: Float = 0f,
        val current: Int = 0,
        val total: Int = 0,
        val subProgress: Float = -1f,
        val result: EnableResultState? = null,
        val error: AppError? = null // 错误信息（使用 AppError 支持不同错误类型）
)

/** MOD 开启结果状态 */
data class EnableResultState(
        val enabledCount: Int,
        val needPasswordMods: List<ModBean> = emptyList(),
        val fileMissingMods: List<ModBean> = emptyList(),
        val backupFailedMods: List<ModBean> = emptyList(),
        val enableFailedMods: List<ModBean> = emptyList(),
        val restoreFailedMods: List<ModBean> = emptyList(),
        /** 互相冲突的 MOD（待开启列表内部冲突） */
        val mutualConflictMods: List<ModBean> = emptyList(),
        /** 与已开启 MOD 冲突的 MOD */
        val enabledConflictMods: List<ModBean> = emptyList()
)

/** MOD 解密进度状态 */
data class DecryptProgressState(
        val isProcessing: Boolean = true,
        val step: DecryptStep = DecryptStep.VALIDATING_PASSWORD,
        val modName: String = "",
        val current: Int = 0,
        val total: Int = 0,
        val progress: Float = 0f,
        val error: String? = null,
        val isComplete: Boolean = false,
        val decryptedCount: Int = 0,
        // 成功后待开启的信息
        val pendingPassword: String? = null,
        val pendingMod: ModBean? = null
)

/** MOD 删除进度状态 */
data class DeleteProgressState(
        val isProcessing: Boolean = true,
        val step: DeleteStep = DeleteStep.AUTHENTICATING,
        val modName: String = "",
        val progress: Float = 0f,
        val current: Int = 0,
        val total: Int = 0,
        val result: DeleteResultState? = null,
        val error: AppError? = null
)

/** MOD 删除结果状态 */
data class DeleteResultState(
        val deletedCount: Int,
        val deletedMods: List<ModBean> = emptyList(), // 已删除的MOD（物理文件和数据库都已删除）
        val deletedEnabledMods: List<ModBean> = emptyList(), // 已删除的已启用MOD（物理文件已删除，数据库保留）
        val skippedIntegratedMods: List<ModBean> = emptyList(),
        val failedMods: List<Pair<ModBean, AppError>> = emptyList()
)

/** 删除前检查状态（确认对话框） */
data class DeleteCheckState(val checkResult: DeleteCheckResult, val isLoading: Boolean = false)
