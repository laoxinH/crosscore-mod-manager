package top.lings.start

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class StartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 通过背景色使状态栏和导航栏透明
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 启用 Edge-to-Edge 模式
        enableEdgeToEdge()

        setContent {
            // 设置导航栏背景颜色和图标亮度
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightNavigationBars = true
                @Suppress("DEPRECATION")
                window.navigationBarColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
            }
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                UserAgreement()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
