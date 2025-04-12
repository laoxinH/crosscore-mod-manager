package top.laoxin.modmanager.activity.start

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import top.laoxin.modmanager.activity.main.MainActivity
import top.laoxin.modmanager.activity.userAgreement.UserAgreementActivity
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.startView.StartContent

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ModManagerTheme {
                Surface(Modifier.fillMaxSize()) {
                    StartContent()
                }
            }
        }

        Handler.createAsync(mainLooper).postDelayed({
            jumpToActivity()
        }, 400)
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
}