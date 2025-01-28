package top.lings.start

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import top.laoxin.modmanager.MainActivity
import top.lings.userAgreement.UserAgreementActivity
import java.util.concurrent.atomic.AtomicBoolean

class StartActivity : ComponentActivity() {
    private val isKeepOnScreen = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowConfiguration()
        checkOrientation()
        setupSplashScreen()
        jumpToActivity()
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
                    .setDuration(300)
                    .withEndAction(provider::remove)
                    .start()
            }
        }
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

    // 跳转到目标 Activity
    private fun jumpToActivity() {
        val targetActivity =
            if (isUserAgreementConfirmed()) MainActivity::class.java else UserAgreementActivity::class.java
        startActivity(Intent(this, targetActivity))
        isKeepOnScreen.set(false)
        finish()
    }

    // 判断用户是否已确认用户协议
    private fun isUserAgreementConfirmed() =
        getSharedPreferences("AppLaunch", MODE_PRIVATE).getBoolean("isConfirm", false)
}