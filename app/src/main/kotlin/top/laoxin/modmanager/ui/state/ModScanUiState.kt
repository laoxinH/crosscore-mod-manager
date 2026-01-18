package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.ScanStep

/** Mod 扫描和列表 UI 状态 */
data class ModScanUiState(
        val isLoading: Boolean = false,
        val loadingPath: String = "",
        val allMods: List<ModBean> = emptyList(),
        val enableMods: List<ModBean> = emptyList(),
        val disableMods: List<ModBean> = emptyList(),
        val openPermissionRequestDialog: Boolean = false,
        val requestPermissionPath: String = "",
        val showDisEnableModsDialog: Boolean = false,
        val delEnableModsList: List<ModBean> = emptyList(),
        val showForceScanDialog: Boolean = false,
        // 新增：扫描进度状态
        val scanProgress: ScanProgressState? = null,
)

/** 扫描进度状态 */
data class ScanProgressState(
        val isScanning: Boolean = true,
        val isBackgroundMode: Boolean = false, // 后台扫描模式
        val isForceScan: Boolean = false, // 是否为强制扫描
        val step: ScanStep = ScanStep.LISTING_FILES, // 当前扫描步骤（用于 i18n）
        val sourceName: String = "", // 当前扫描的压缩包/文件夹名
        val currentFile: String = "", // 当前处理的文件名
        val progress: Float = 0f, // 总体进度百分比 0-1
        val current: Int = 0, // 当前进度值
        val total: Int = 0, // 总数
        val foundModsCount: Int = 0, // 已发现的MOD数量
        // 子进度信息（文件分析等）
        val subProgress: Float = -1f, // 子进度百分比 0-1，-1 表示无子进度
        val result: ScanResultState? = null, // 扫描结果（完成时）
        val error: AppError? = null // 错误信息（使用 AppError 支持不同错误类型）
)

/** 扫描结果状态 */
data class ScanResultState(
        val scannedCount: Int,
        val addedCount: Int,
        val updatedCount: Int,
        val deletedCount: Int,
        val skippedCount: Int = 0,
        val transferredCount: Int = 0,
        val errors: List<String> = emptyList(),
        /** 物理文件已删除但仍处于启用状态的MOD列表 */
        val deletedEnabledMods: List<ModBean> = emptyList()
)
