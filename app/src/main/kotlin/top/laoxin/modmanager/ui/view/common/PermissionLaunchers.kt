package top.laoxin.modmanager.ui.view.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
//import top.laoxin.modmanager.App

/** SAF (Storage Access Framework) 权限请求 Launcher 用于请求目录访问权限 */
@Composable
fun rememberSAFPermissionLauncher(
        onPermissionGranted: (Uri) -> Unit,
        onPermissionDenied: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // 持久化 URI 权限
                context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                onPermissionGranted(uri)
            }
                    ?: onPermissionDenied()
        } else {
            onPermissionDenied()
        }
    }
}

/** 全局存储权限请求 Launcher (Android 11+) 用于请求 MANAGE_EXTERNAL_STORAGE 权限 */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun rememberStoragePermissionLauncher(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 不管返回什么结果，都检查实际权限状态
        if (Environment.isExternalStorageManager()) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
}

/** 通知权限请求 Launcher */
@Composable
fun rememberNotificationPermissionLauncher(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 检查实际权限状态
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
}

/**
 * 创建 SAF Intent
 * @param path 需要请求权限的路径
 * @return Intent 用于启动 SAF 选择器
 */

fun createSAFIntent(path: String, context: Context): Intent {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    intent.setFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    )

    // 尝试设置初始目录
    try {
        val treeUri = pathToTreeUri(path)
        val df = DocumentFile.fromTreeUri(context, treeUri)
        df?.let { intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it.uri) }
    } catch (_: Exception) {
        // 忽略初始目录设置失败
    }

    return intent
}

/** 创建全局存储权限 Intent (Android 11+) */
@RequiresApi(Build.VERSION_CODES.R)
//@Composable
fun createStoragePermissionIntent(context : Context): Intent {
    //val  context = LocalContext.current

    return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = ("package:" + context.packageName).toUri()
    }
}

/** 创建通知权限设置 Intent */
@Composable
fun createNotificationSettingsIntent(): Intent {
    val  context = LocalContext.current
    return Intent().apply {
        action = "android.settings.APP_NOTIFICATION_SETTINGS"
        // for Android 5-7
        putExtra("app_package", context.packageName)
        putExtra("app_uid", context.applicationInfo.uid)
        // for Android 8 and above
        putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
    }
}

/** 将文件路径转换为 Tree URI */
private fun pathToTreeUri(path: String): Uri {
    val encodedPath = path.replace("/storage/emulated/0/", "primary:").replace("/", "%2F")
    return "content://com.android.externalstorage.documents/tree/$encodedPath".toUri()
}
