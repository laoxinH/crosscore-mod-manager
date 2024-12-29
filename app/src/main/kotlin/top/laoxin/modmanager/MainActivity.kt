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
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ModManagerApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 通过背景色使状态栏和导航栏透明
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 添加 Shizuku 权限请求监听
        Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)

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