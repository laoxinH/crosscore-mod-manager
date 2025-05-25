package top.laoxin.modmanager.activity.start

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import top.laoxin.modmanager.activity.main.MainActivity
import top.laoxin.modmanager.activity.userAgreement.UserAgreementActivity
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.startView.StartContent
import java.util.concurrent.atomic.AtomicBoolean

// 显示启动页
class StartActivity : ComponentActivity() {
    private val isKeepOnScreen = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { isKeepOnScreen.get() }

        splashScreen.setOnExitAnimationListener { provider ->
            provider.iconView.animate()
                .alpha(0f)
                .setDuration(0L)
                .withEndAction(provider::remove)
                .start()
        }

        checkOrientation()

        setupWindowConfiguration()

        setContent {
            ModManagerTheme {
                ConfigureSystemBars()
                Surface(Modifier.fillMaxSize()) {
                    StartContent()
                }
            }
        }

        isKeepOnScreen.set(false)

        Handler.createAsync(mainLooper).postDelayed({
            jumpToActivity()
        }, 600)
    }

    // 跳转到目标 Activity
    private fun jumpToActivity() {
        val targetActivity =
            if (isUserAgreementConfirmed()) MainActivity::class.java else UserAgreementActivity::class.java
        startActivity(Intent(this, targetActivity))
        // 设置转场动画
        // API 34+ 用 overrideActivityTransition，否则用 overridePendingTransition
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        // 结束当前 Activity
        finish()
    }

    // 判断用户是否已确认用户协议
    private fun isUserAgreementConfirmed() =
        getSharedPreferences("AppLaunch", MODE_PRIVATE).getBoolean("isConfirm", false)

    // 启用边到边
    private fun setupWindowConfiguration() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
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
