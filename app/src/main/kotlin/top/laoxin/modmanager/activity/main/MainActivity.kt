package top.laoxin.modmanager.activity.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import top.laoxin.modmanager.activity.userAgreement.UserAgreementActivity
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.notification.AppNotificationManager
import top.laoxin.modmanager.service.ScanForegroundService
import top.laoxin.modmanager.ui.view.ModernModManagerApp
import top.laoxin.modmanager.ui.view.splash.RandomSplashScreen
import top.laoxin.modmanager.ui.theme.ModManagerTheme

@AndroidEntryPoint
class MainActivity() : ComponentActivity() {

    @Inject lateinit var permissionService: PermissionService

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // 检查用户协议
        if (!isUserAgreementConfirmed()) {
            startActivity(Intent(this, UserAgreementActivity::class.java))
            finish()
            return
        }

        setupWindow()
        setupShizuku()
        enableEdgeToEdge()
        setContent {
            ModManagerTheme {
                ConfigureSystemBars()
                Surface(Modifier.fillMaxSize()) {
                    // 使用随机启动屏包裹主内容
                    // 如果不需要随机启动屏，直接使用 ModernModManagerApp()
                    RandomSplashScreen(
                        durationMillis = 1500L,
                        onSplashFinished = { /* 启动屏结束回调 */ }
                    ) {
                        ModernModManagerApp()
                    }
                }
            }
        }

        // 处理从通知点击进入
        handleNotificationClick(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理从通知点击进入
        handleNotificationClick(intent)
    }

    /**
     * 处理通知点击
     * 退出后台模式，重新显示扫描覆盖层
     */
    private fun handleNotificationClick(intent: Intent?) {
        val uri = intent?.data ?: return
        
        // 检查是否来自扫描通知的 Deep Link
        if (uri.toString().startsWith(AppNotificationManager.DEEP_LINK_SCAN_PROGRESS) ||
            uri.toString().startsWith(AppNotificationManager.DEEP_LINK_SCAN_RESULT)) {
            // 通知 Service 退出后台模式，让覆盖层重新出现
            ScanForegroundService.exitBackground(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupShizuku()

        if (isFinishing) {
            // LogTools.flushAll()
        }
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupShizuku() {
        permissionService.registerShizukuListener()
    }

    private fun cleanupShizuku() {
        permissionService.unregisterShizukuListener()
    }

    private fun isUserAgreementConfirmed() =
            getSharedPreferences("AppLaunch", MODE_PRIVATE).getBoolean("isConfirm", false)

    // 设置状态栏和导航栏
    @Composable
    private fun ConfigureSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = true
            @Suppress("DEPRECATION")
            window.navigationBarColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
        }
    }
}

