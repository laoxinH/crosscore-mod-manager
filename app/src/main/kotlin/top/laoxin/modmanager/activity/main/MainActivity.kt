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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.activity.userAgreement.UserAgreementActivity
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ModManagerApp

@AndroidEntryPoint
class MainActivity() : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen
        installSplashScreen()
        
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
                    ModManagerApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupShizuku()
        
        if (isFinishing) {
            LogTools.flushAll()
        }
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupShizuku() {
        try {
            if (PermissionTools.isShizukuAvailable) {
                Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
            }
        } catch (_: Exception) {
        }
    }

    private fun cleanupShizuku() {
        try {
            if (PermissionTools.isShizukuAvailable) {
                Shizuku.removeRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
            }
        } catch (_: Exception) {
        }
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