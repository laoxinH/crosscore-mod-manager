package top.laoxin.modmanager.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import top.laoxin.modmanager.domain.model.ScanStep
import top.laoxin.modmanager.domain.repository.ScanStateRepository
import top.laoxin.modmanager.domain.usercase.mod.ScanAndSyncModsUseCase
import top.laoxin.modmanager.domain.usercase.mod.ScanState
import top.laoxin.modmanager.notification.AppNotificationManager
import top.laoxin.modmanager.ui.state.ScanProgressState
import top.laoxin.modmanager.ui.state.ScanResultState
import top.laoxin.modmanager.ui.state.SnackbarManager
import top.laoxin.modmanager.R
import javax.inject.Inject

/**
 * 扫描前台服务
 * 负责执行 MOD 扫描任务，即使 App 在后台也能继续运行
 */
@AndroidEntryPoint
class ScanForegroundService : Service() {

    companion object {
        private const val TAG = "ScanForegroundService"
        
        // Intent Actions
        const val ACTION_START_SCAN = "top.laoxin.modmanager.action.START_SCAN"
        const val ACTION_SWITCH_BACKGROUND = "top.laoxin.modmanager.action.SWITCH_BACKGROUND"
        const val ACTION_EXIT_BACKGROUND = "top.laoxin.modmanager.action.EXIT_BACKGROUND"
        const val ACTION_CANCEL = "top.laoxin.modmanager.action.CANCEL"
        
        // Intent Extras
        const val EXTRA_FORCE_SCAN = "extra_force_scan"
        
        // Deep Link URIs
        const val DEEP_LINK_SCAN_PROGRESS = "modmanager://scan/progress"
        const val DEEP_LINK_SCAN_RESULT = "modmanager://scan/result"
        
        /**
         * 启动扫描
         */
        fun startScan(context: Context, forceScan: Boolean = false) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_START_SCAN
                putExtra(EXTRA_FORCE_SCAN, forceScan)
            }
            context.startForegroundService(intent)
        }
        
        /**
         * 切换到后台模式
         */
        fun switchToBackground(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_SWITCH_BACKGROUND
            }
            context.startService(intent)
        }
        
        /**
         * 取消扫描
         */
        fun cancel(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }

        /**
         * 退出后台模式 - 用户点击通知返回时调用
         */
        fun exitBackground(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_EXIT_BACKGROUND
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var scanAndSyncUseCase: ScanAndSyncModsUseCase
    
    @Inject
    lateinit var scanStateRepository: ScanStateRepository
    
    @Inject
    lateinit var notificationManager: AppNotificationManager
    
    @Inject
    lateinit var snackbarManager: SnackbarManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null
    private var isBackgroundMode = false
    private var isForceScan = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SCAN -> {
                isForceScan = intent.getBooleanExtra(EXTRA_FORCE_SCAN, false)
                startForegroundWithNotification()
                startScan()
            }
            ACTION_SWITCH_BACKGROUND -> {
                switchToBackgroundMode()
            }
            ACTION_EXIT_BACKGROUND -> {
                exitBackgroundMode()
            }
            ACTION_CANCEL -> {
                cancelScan()
            }
        }
        
        return START_NOT_STICKY
    }

    /**
     * 启动前台服务并显示通知
     */
    private fun startForegroundWithNotification() {
        val notification = notificationManager.createInitialScanNotification()
        startForeground(AppNotificationManager.SCAN_NOTIFICATION_ID, notification)
    }

    /**
     * 开始扫描
     */
    private fun startScan() {
        // 取消之前的扫描任务
        scanJob?.cancel()
        
        scanJob = serviceScope.launch {
            scanAndSyncUseCase.execute(scanExternalDirs = true, forceScan = isForceScan)
                .catch { e ->
                    Log.e(TAG, "Scan failed", e)
                    handleScanError(e)
                }
                .collect { state ->
                    handleScanState(state)
                }
        }
    }

    /**
     * 处理扫描状态
     */
    private fun handleScanState(state: ScanState) {
        when (state) {
            is ScanState.Preparing -> {
                updateProgressState(
                    step = ScanStep.LISTING_FILES,
                    progress = 0f
                )
            }
            is ScanState.Progress -> {
                updateProgressState(
                    step = state.step,
                    sourceName = state.sourceName,
                    currentFile = state.currentFile,
                    progress = state.overallPercent,
                    current = state.current,
                    total = state.total,
                    subProgress = state.subProgress
                )
            }
            is ScanState.ModFound -> {
                // 更新已发现 MOD 数量
                val currentState = scanStateRepository.scanState.value
                currentState?.let {
                    scanStateRepository.updateState(
                        it.copy(foundModsCount = it.foundModsCount + 1)
                    )
                }
            }
            is ScanState.TransferComplete -> {
                // 转移完成，继续扫描
            }
            is ScanState.ScanComplete -> {
                // 扫描完成，进入同步阶段
            }
            is ScanState.Success -> {
                handleScanSuccess(state.result)
            }
            is ScanState.Error -> {
                handleScanErrorState(state.error)
            }
        }
    }

    /**
     * 更新进度状态
     */
    private fun updateProgressState(
        step: ScanStep,
        sourceName: String = "",
        currentFile: String = "",
        progress: Float = 0f,
        current: Int = 0,
        total: Int = 0,
        subProgress: Float = -1f
    ) {
        val currentState = scanStateRepository.scanState.value
        val foundModsCount = currentState?.foundModsCount ?: 0
        
        val newState = ScanProgressState(
            isScanning = true,
            isBackgroundMode = isBackgroundMode,
            isForceScan = isForceScan,
            step = step,
            sourceName = sourceName,
            currentFile = currentFile,
            progress = progress,
            current = current,
            total = total,
            foundModsCount = foundModsCount,
            subProgress = subProgress
        )
        
        scanStateRepository.updateState(newState)
        
        // 始终更新通知（前台服务必须显示通知）
        notificationManager.showScanProgress(newState)
    }

    /**
     * 处理扫描成功
     * 强制扫描: 始终显示结果页面
     * 普通扫描: 只有在存在已删除开启 MOD 时才显示结果页面，否则显示 Snackbar
     */
    private fun handleScanSuccess(result: top.laoxin.modmanager.domain.usercase.mod.ScanSyncResult) {
        val resultState = ScanResultState(
            scannedCount = result.scannedCount,
            addedCount = result.addedCount,
            updatedCount = result.updatedCount,
            deletedCount = result.deletedCount,
            skippedCount = result.skippedCount,
            transferredCount = result.transferredCount,
            deletedEnabledMods = result.deletedEnabledMods,
            errors = result.errors
        )
        
        // 判断是否需要显示结果页面
        val shouldShowResult = isForceScan || result.deletedEnabledMods.isNotEmpty()
        
        if (shouldShowResult) {
            // 显示结果页面
            val finalState = ScanProgressState(
                isScanning = false,
                isBackgroundMode = isBackgroundMode,
                isForceScan = isForceScan,
                result = resultState
            )
            scanStateRepository.updateState(finalState)
        } else {
            // 不显示结果页面，清除状态并显示 Snackbar
            scanStateRepository.clear()
            snackbarManager.showMessageAsync(R.string.scan_complete_toast, result.addedCount, result.updatedCount, result.deletedCount)
        }
        
        // 显示完成通知
        notificationManager.showScanComplete(
            addedCount = result.addedCount,
            updatedCount = result.updatedCount,
            deletedCount = result.deletedCount
        )
        
        // 停止前台服务
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    /**
     * 处理扫描错误状态
     */
    private fun handleScanErrorState(error: top.laoxin.modmanager.domain.model.AppError) {
        val errorState = ScanProgressState(
            isScanning = false,
            isBackgroundMode = isBackgroundMode,
            error = error
        )
        
        scanStateRepository.updateState(errorState)
        
        // 显示错误通知
        notificationManager.showScanError()
        
        // 停止前台服务
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    /**
     * 处理扫描异常
     */
    private fun handleScanError(e: Throwable) {
        val errorState = ScanProgressState(
            isScanning = false,
            isBackgroundMode = isBackgroundMode,
            error = top.laoxin.modmanager.domain.model.AppError.Unknown(e as? Exception ?: Exception(e))
        )
        
        scanStateRepository.updateState(errorState)
        
        notificationManager.showScanError()
        
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    /**
     * 切换到后台模式
     */
    private fun switchToBackgroundMode() {
        isBackgroundMode = true
        
        // 更新当前状态为后台模式
        val currentState = scanStateRepository.scanState.value
        currentState?.let {
            val updatedState = it.copy(isBackgroundMode = true)
            scanStateRepository.updateState(updatedState)
            notificationManager.showScanProgress(updatedState)
        }
    }

    /**
     * 退出后台模式 - 用户点击通知返回时调用
     */
    private fun exitBackgroundMode() {
        isBackgroundMode = false
        
        // 更新当前状态为前台模式
        val currentState = scanStateRepository.scanState.value
        currentState?.let {
            val updatedState = it.copy(isBackgroundMode = false)
            scanStateRepository.updateState(updatedState)
        }
    }

    /**
     * 取消扫描
     */
    private fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        isBackgroundMode = false
        
        scanStateRepository.clear()
        notificationManager.cancelScanNotification()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
