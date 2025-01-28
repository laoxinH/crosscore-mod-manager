package top.laoxin.modmanager

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
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ModManagerApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }

    // 设置窗口
    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }

    // 添加 Shizuku 监听器
    private fun setupShizuku() {
        Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
    }

    // 移除 Shizuku 监听器
    private fun cleanupShizuku() {
        if (PermissionTools.isShizukuAvailable) {
            Shizuku.removeRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
        }
    }

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