package top.laoxin.modmanager

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ModManagerApp
import top.lings.start.StartActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取屏幕宽度
        val screenWidthDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 使用 WindowMetrics 获取屏幕宽度（单位：dp）
            val windowMetrics = windowManager.currentWindowMetrics
            val widthPixels = windowMetrics.bounds.width()
            widthPixels / resources.displayMetrics.density
        } else {
            // 兼容 Android 11 (API 30) 以下的版本
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels / displayMetrics.density
        }

        // 判定设备是否是平板（假设屏幕宽度大于等于 600dp 为平板）
        requestedOrientation = if (screenWidthDp >= 600) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // 添加 Shizuku 权限请求监听
        Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 检查是否同意许可，跳转到 StartActivity 页面
        val sharedPreferences = getSharedPreferences("AppLaunch", MODE_PRIVATE)
        val isConfirm = sharedPreferences.getBoolean("isConfirm", false)

        if (!isConfirm) {
            // 如果未同意许可，跳转到 StartActivity 进行确认
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 同意许可，加载 MainActivity 的内容
        setContent {
            // 使用 Material3 主题适配深色模式
            ModManagerTheme {
                val systemUiController = rememberSystemUiController()
                val colors = MaterialTheme.colorScheme
                val dark = isSystemInDarkTheme()

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = !dark
                    )
                    systemUiController.setNavigationBarColor(
                        color = colors.surfaceContainer,
                        darkIcons = !dark
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ModManagerApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (PermissionTools.isShizukuAvailable) {
            // 移除 Shizuku 权限请求回调
            Shizuku.removeRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ModManagerTheme {
        ModManagerApp()
    }
}