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

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 通过背景色使状态栏和导航栏透明
        window.setBackgroundDrawableResource(android.R.color.transparent)
        // 启用 Edge-to-Edge 模式
        enableEdgeToEdge()

        checkOrientation()

        jumpToActivity()

        // 保持SplashScreen
        var splashScreen = installSplashScreen().apply {
            setKeepOnScreenCondition {
                isKeepOnScreen.get()
            }
        }

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.iconView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    splashScreenViewProvider.remove()
                }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // 检查用户协议状态并跳转到 MainActivity 或 UserAgreementActivity
    fun jumpToActivity() {
        val sharedPreferences = getSharedPreferences("AppLaunch", MODE_PRIVATE)
        val isConfirm = sharedPreferences.getBoolean("isConfirm", false)

        val intent = if (isConfirm) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, UserAgreementActivity::class.java)
        }

        startActivity(intent)
        isKeepOnScreen.set(false)
        finish()
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