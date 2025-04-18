package top.laoxin.modmanager.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import top.laoxin.modmanager.activity.start.StartActivity
import java.util.concurrent.atomic.AtomicBoolean

// 原生启动页
class SplashActivity : ComponentActivity() {
    private val isKeepOnScreen = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowConfiguration()
        checkOrientation()
        setupSplashScreen()
        jumpToStart()
    }

    // 跳转到启动 Activity
    private fun jumpToStart() {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }

    // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
    private fun setupWindowConfiguration() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
    }

    // 设置启动画面
    private fun setupSplashScreen() {
        installSplashScreen().apply {
            setKeepOnScreenCondition { isKeepOnScreen.get() }
            setOnExitAnimationListener { provider ->
                provider.iconView.animate()
                    .alpha(0f)
                    .setDuration(0)
                    .withEndAction(provider::remove)
                    .start()
            }
        }
        isKeepOnScreen.set(false)
    }

    // 检查屏幕方向
    private fun checkOrientation() {
        requestedOrientation = when {
            isTabletScreen() -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // 判断是否为平板屏幕
    private fun isTabletScreen(): Boolean {
        val screenWidthDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width() / resources.displayMetrics.density
        } else {
            val metrics = DisplayMetrics().also {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(it)
            }
            metrics.widthPixels / metrics.density
        }
        return screenWidthDp >= 600
    }
}