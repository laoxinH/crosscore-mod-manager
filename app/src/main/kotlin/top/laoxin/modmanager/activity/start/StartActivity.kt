package top.laoxin.modmanager.activity.start

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import top.laoxin.modmanager.activity.main.MainActivity
import top.laoxin.modmanager.activity.userAgreement.UserAgreementActivity
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.startView.StartContent

// 用于显示自定义启动页
class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowConfiguration()

        setContent {
            ModManagerTheme {
                ConfigureSystemBars()
                Surface(Modifier.fillMaxSize()) {
                    StartContent()
                }
            }
        }

        Handler.createAsync(mainLooper).postDelayed({
            jumpToActivity()
        }, 600)
    }

    // 跳转到目标 Activity
    private fun jumpToActivity() {
        val targetActivity =
            if (isUserAgreementConfirmed()) MainActivity::class.java else UserAgreementActivity::class.java
        startActivity(Intent(this, targetActivity))
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

}