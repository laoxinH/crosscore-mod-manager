package top.laoxin.modmanager

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ModManagerApp
import top.lings.start.StartActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 通过背景色使状态栏和导航栏透明
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 获取屏幕宽度
        val screenWidthDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val widthPixels = windowMetrics.bounds.width()
            widthPixels / resources.displayMetrics.density
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels / displayMetrics.density
        }

        requestedOrientation = if (screenWidthDp >= 600) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // 添加 Shizuku 权限请求监听
        Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)

        // 检查是否同意许可，跳转到 StartActivity 页面
        val sharedPreferences = getSharedPreferences("AppLaunch", MODE_PRIVATE)
        val isConfirm = sharedPreferences.getBoolean("isConfirm", false)

        if (!isConfirm) {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            ModManagerTheme {
                // 设置导航栏背景颜色和图标亮度
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightNavigationBars = true
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
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