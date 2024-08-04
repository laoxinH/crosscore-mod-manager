package top.laoxin.modmanager

import ModManagerApp
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import rikka.shizuku.Shizuku
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme

class MainActivity : ComponentActivity() {


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(PermissionTools.REQUEST_PERMISSION_RESULT_LISTENER)
        setContent {
            ModManagerTheme {
                // A surface container using the 'background' color from the theme
                //enableEdgeToEdge()
                val systemUiController = rememberSystemUiController()
                val colors = MaterialTheme.colorScheme

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = colors.primaryContainer
                    )
                    systemUiController.setNavigationBarColor(
                        color = colors.surfaceContainer
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
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