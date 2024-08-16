package top.laoxin.modmanager

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ModManagerApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 添加 Shizuku 权限请求监听
        Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)

        // 设置全屏模式，使内容可以扩展到状态栏和导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ModManagerTheme {
                val systemUiController = rememberSystemUiController()
                val colors = MaterialTheme.colorScheme

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = true
                    )
                    systemUiController.setNavigationBarColor(
                        color = colors.background,
                        darkIcons = true
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(0.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ModManagerApp()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (PermissionTools.isShizukuAvailable) {
            // 移除Shizuku权限请求回调
            Shizuku.removeRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
        }
    }


    // 读取文件路径
    /*    private fun loadPath(path: String?, isUserClicked: Boolean) {
        if (path == null) {
            return
        }
        val isNavigate = !TextUtils.equals(mPathCache, path)
        mPathCache = path
        if (FileTools.shouldRequestUriPermission(path)) {
            if (isUserClicked) {

                // 读取安卓目录权限
                showRequestUriPermissionDialog()
            }
        } else {
            mDirectory = File(path)
            binding.tvPath.setText(mDirectory!!.getPath())
            val list: List<BeanFile?> = FileTools.getSortedFileList(path)
            val bundle = Bundle()
            bundle.putParcelableArrayList(BundleKey.FILE_LIST, list as ArrayList<out Parcelable?>)
            if (!isNavigate) {
                mNavController.popBackStack()
            }
            mNavController.navigate(R.id.fileListFragment, bundle)
        }
    }*/

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ModManagerTheme {
        ModManagerApp()
    }
}