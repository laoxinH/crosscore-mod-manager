package top.lings.start

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
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
import top.laoxin.modmanager.MainActivity
import top.lings.userAgreement.UserAgreementActivity

class StartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 通过背景色使状态栏和导航栏透明
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 启用 Edge-to-Edge 模式
        enableEdgeToEdge()

        // 检查屏幕方向
        checkOrientation()

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
                Start(onTimeout = {
                    // 检查用户协议状态并跳转到 MainActivity 或 UserAgreementActivity
                    val sharedPreferences = getSharedPreferences("AppLaunch", MODE_PRIVATE)
                    val isConfirm = sharedPreferences.getBoolean("isConfirm", false)

                    val intent = if (isConfirm) {
                        Intent(this, MainActivity::class.java)
                    } else {
                        Intent(this, UserAgreementActivity::class.java)
                    }

                    startActivity(intent)
                    finish()
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // 检查屏幕方向
    fun checkOrientation() {
        // 获取屏幕宽度
        val screenWidthDp =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11
                val windowMetrics = windowManager.currentWindowMetrics
                val widthPixels = windowMetrics.bounds.width()
                widthPixels / resources.displayMetrics.density
            } else {
                // Android 10
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(DisplayMetrics())
                DisplayMetrics().widthPixels / DisplayMetrics().density
            }

        // 根据屏幕宽度设置屏幕方向
        requestedOrientation = if (screenWidthDp >= 600) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}