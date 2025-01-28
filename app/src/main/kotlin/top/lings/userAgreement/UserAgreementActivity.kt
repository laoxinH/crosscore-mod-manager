package top.lings.userAgreement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import top.laoxin.modmanager.ui.theme.ModManagerTheme

class UserAgreementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowConfiguration()
        setContent {
            ModManagerTheme {
                ConfigureSystemBars()
                UserAgreement()
            }
        }
    }

    // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
    private fun setupWindowConfiguration() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
    }

    // 设置导航栏背景颜色和图标亮度
    @Composable
    private fun ConfigureSystemBars() {
        val activity = LocalActivity.current
        activity?.let { WindowInsetsControllerCompat(it.window, activity.window.decorView) }.apply {
            this?.isAppearanceLightNavigationBars = true
            if (activity != null) {
                @Suppress("DEPRECATION")
                activity.window.navigationBarColor =
                    MaterialTheme.colorScheme.surfaceContainer.toArgb()
            }
        }
    }
}