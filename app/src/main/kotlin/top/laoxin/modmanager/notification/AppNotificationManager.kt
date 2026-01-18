package top.laoxin.modmanager.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.R
import top.laoxin.modmanager.activity.main.MainActivity
import top.laoxin.modmanager.service.ScanForegroundService
import top.laoxin.modmanager.ui.state.ScanProgressState
import top.laoxin.modmanager.ui.state.ScanResultState

/** 统一的应用通知管理器 处理所有类型的通知（扫描进度、未来可能的其他通知类型） */
@Singleton
class AppNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        // 扫描通知 ID (公开供 Service 使用)
        const val SCAN_NOTIFICATION_ID = 1001
        
        // Deep Link URIs
        const val DEEP_LINK_SCAN_PROGRESS = "modmanager://scan/progress"
        const val DEEP_LINK_SCAN_RESULT = "modmanager://scan/result"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /** 创建所有通知渠道 */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val scanChannel =
                    NotificationChannel(
                                    context.getString(R.string.scan_notification_channel_id),
                                    context.getString(R.string.scan_notification_channel_name),
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description =
                                        context.getString(
                                                R.string.scan_notification_channel_description
                                        )
                                setShowBadge(false)
                            }

            val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(scanChannel)
        }
    }

    /** 检查是否有通知权限 */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    /** 创建初始扫描通知 (用于 Foreground Service 启动) */
    fun createInitialScanNotification(): Notification {
        return createScanNotificationBuilder(DEEP_LINK_SCAN_PROGRESS)
                .setContentTitle(context.getString(R.string.scan_notification_title))
                .setContentText(context.getString(R.string.scan_notification_preparing))
                .setProgress(0, 0, true)
                .setOngoing(true)
                .addAction(
                        android.R.drawable.ic_delete,
                        context.getString(R.string.scan_notification_cancel),
                        createCancelPendingIntent()
                )
                .build()
    }

    /** 显示扫描进度通知 */
    fun showScanProgress(state: ScanProgressState) {
        if (!hasNotificationPermission()) return

        val contentText =
                context.getString(
                        R.string.scan_notification_progress,
                        state.sourceName.ifEmpty { state.currentFile }
                )

        val notification =
                createScanNotificationBuilder(DEEP_LINK_SCAN_PROGRESS)
                        .setContentTitle(context.getString(R.string.scan_notification_title))
                        .setContentText(contentText)
                        .setProgress(100, (state.progress * 100).toInt(), false)
                        .setOngoing(true)
                        .addAction(
                                android.R.drawable.ic_delete,
                                context.getString(R.string.scan_notification_cancel),
                                createCancelPendingIntent()
                        )
                        .build()

        notificationManager.notify(SCAN_NOTIFICATION_ID, notification)
    }

    /** 显示扫描完成通知 */
    fun showScanComplete(result: ScanResultState) {
        showScanComplete(result.addedCount, result.updatedCount, result.deletedCount)
    }

    /** 显示扫描完成通知 */
    fun showScanComplete(addedCount: Int, updatedCount: Int, deletedCount: Int = 0) {
        if (!hasNotificationPermission()) return

        val contentText =
                context.getString(
                        R.string.scan_notification_complete,
                        addedCount,
                        updatedCount,
                        deletedCount
                )

        val notification =
                createScanNotificationBuilder(DEEP_LINK_SCAN_RESULT)
                        .setContentTitle(context.getString(R.string.scan_result_title))
                        .setContentText(contentText)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .build()

        notificationManager.notify(SCAN_NOTIFICATION_ID, notification)
    }

    /** 显示扫描错误通知 */
    fun showScanError(errorMessage: String? = null) {
        if (!hasNotificationPermission()) return

        val notification =
                createScanNotificationBuilder(DEEP_LINK_SCAN_PROGRESS)
                        .setContentTitle(context.getString(R.string.scan_error_title))
                        .setContentText(
                                errorMessage ?: context.getString(R.string.scan_notification_error)
                        )
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .build()

        notificationManager.notify(SCAN_NOTIFICATION_ID, notification)
    }

    /** 取消扫描通知 */
    fun cancelScanNotification() {
        notificationManager.cancel(SCAN_NOTIFICATION_ID)
    }

    /** 创建扫描通知的基础 Builder */
    private fun createScanNotificationBuilder(deepLink: String): NotificationCompat.Builder {
        val intent =
                Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
                    setClass(context, MainActivity::class.java)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

        val pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        return NotificationCompat.Builder(
                        context,
                        context.getString(R.string.scan_notification_channel_id)
                )
                .setSmallIcon(R.drawable.app_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
    }

    /** 创建取消扫描的 PendingIntent */
    private fun createCancelPendingIntent(): PendingIntent {
        val cancelIntent = Intent(context, ScanForegroundService::class.java).apply {
            action = ScanForegroundService.ACTION_CANCEL
        }
        return PendingIntent.getService(
                context,
                1, // 使用不同的 requestCode 避免冲突
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

