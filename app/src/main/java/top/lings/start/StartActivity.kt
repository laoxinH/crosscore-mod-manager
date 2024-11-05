package top.lings.start

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class StartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 使用 Material3 主题适配深色模式
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                val systemUiController = rememberSystemUiController()
                val colors = MaterialTheme.colorScheme
                val dark = isSystemInDarkTheme()
                val backgroundColor = colors.background

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = backgroundColor,
                        darkIcons = !dark
                    )
                    systemUiController.setNavigationBarColor(
                        color = colors.surfaceContainer,
                        darkIcons = !dark
                    )
                }
                UserAgreement()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
