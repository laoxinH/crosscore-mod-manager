package top.laoxin.modmanager.ui.view.commen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import top.laoxin.modmanager.ui.view.SettingItem


@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestStoragePermission(
) {
    when (App.osVersion) {
        OSVersion.OS_13, OSVersion.OS_11, OSVersion.OS_14 -> {
            if (!Environment.isExternalStorageManager()) {
                var showDialog by remember { mutableStateOf(true) }
                val context = LocalContext.current
                val startForResult =
                    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            ToastUtils.longCall(R.string.toast_permission_granted)
                            showDialog = false
                        } else {
                            if (!Environment.isExternalStorageManager()) {
                                ToastUtils.longCall(R.string.toast_permission_not_granted)
                            } else {
                                ModTools.makeModsDirs()
                                ToastUtils.longCall(R.string.toast_permission_granted)
                                showDialog = false
                            }
                        }
                    }

                val intent: Intent =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(
                        Uri.parse("package:" + context.packageName)
                    )

                DialogCommon(
                    title = stringResource(id = R.string.dialog_storage_title),
                    content = stringResource(id = R.string.dialog_storage_message),
                    onConfirm = {
                        startForResult.launch(intent)
                    },
                    onCancel = {
                        // 直接关闭应用
                        if (context is Activity) {
                            context.finish()
                        }
                    },
                    showDialog = showDialog
                )
            }
        }

        OSVersion.OS_6 -> {
            val permissionState =
                rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            var showDialog by remember { mutableStateOf(true) }
            val context = LocalContext.current
            when (permissionState.status) {
                PermissionStatus.Granted -> {//已授权
                    showDialog = false
                }

                is PermissionStatus.Denied -> {
                    Column {
                        if ((permissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                            //如果用户拒绝了该权限但可以显示理由，那么请温和地解释为什么应用程序需要此权限(拒绝权限)
                            DialogCommon(
                                title = stringResource(id = R.string.dialog_storage_title),
                                content = stringResource(id = R.string.dialog_storage_message_a6),
                                onConfirm = {
                                    permissionState.launchPermissionRequest()
                                },
                                onCancel = {
                                    // 直接关闭应用
                                    if (context is Activity) {
                                        context.finish()
                                    }
                                },
                                showDialog = showDialog
                            )
                        } else {
                            //如果这是用户第一次登陆此功能，或者用户不想再次被要求获得此权限，请说明该权限是必需的(用户选择拒绝且不再询问)
                            DialogCommon(
                                title = stringResource(id = R.string.dialog_storage_title),
                                content = stringResource(id = R.string.dialog_storage_message_a6),
                                onConfirm = {
                                    permissionState.launchPermissionRequest()
                                },
                                onCancel = {
                                    if (context is Activity) {
                                        context.finish()
                                    }
                                },
                                showDialog = showDialog
                            )
                        }
                    }
                }

            }
        }

        else -> {
            ToastUtils.longCall(R.string.toast_permission_granted)
        }
    }

}

@Composable
fun RequestUriPermission(path: String, showDialog: Boolean, onDismissRequest: () -> Unit) {

    SelectPermissionDialog(
        path = path, onDismissRequest = { onDismissRequest() }, showDialog = showDialog
    )
}


@Composable
fun SelectPermissionDialog(
    path: String, onDismissRequest: () -> Unit, showDialog: Boolean
) {
    Log.d("SelectPermissionDialog", "SelectPermissionDialog: $path")
    val requestPermissionPath = PermissionTools.getRequestPermissionPath(path)
    if (showDialog) {
        val context = LocalContext.current
        val startForResult =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

                Log.d("SelectPermissionDialog", "SelectPermissionDialog: ${result.resultCode}")
                if (result.resultCode == Activity.RESULT_OK) {
                    ToastUtils.longCall(R.string.toast_permission_granted)

                    val uri = result.data?.data
                    if (uri != null) {
                        Log.d("SelectPermissionDialog", "SelectPermissionDialog: $uri")
                        App.get().contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    onDismissRequest()
                } else {
                    ToastUtils.longCall(R.string.toast_permission_not_granted)
                    onDismissRequest()
                }
            }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.setFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )

        val treeUri = FileTools.pathToUri(requestPermissionPath)
        val df = DocumentFile.fromTreeUri(context, treeUri)

        if (df != null) {
            Log.d("SelectPermissionDialog", "SelectPermissionDialog: ${df.uri}")
        }
        if (df != null) {

            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, df.uri)
        }
        AlertDialog(onDismissRequest = {}, // 点击对话框外的区域时关闭对话框
            title = { Text(stringResource(R.string.choose_method_title)) }, text = {
                Column {
                    Text(stringResource(R.string.select_permission_dialog_descript))
                    Spacer(modifier = Modifier.height(10.dp))
                    if (App.osVersion == OSVersion.OS_11 || App.osVersion == OSVersion.OS_13) {

                        SettingItem(name = stringResource(R.string.select_permission_dialog_by_file),
                            description = stringResource(R.string.select_permission_dialog_by_file_descript),
                            //icon = painterResource(id = R.drawable.ic_launcher_foreground),
                            onClick = {
                                //onDismissRequest()
                                startForResult.launch(intent)
                            })

                    }
                    SettingItem(name = stringResource(R.string.select_permission_dialog_by_shizuku),
                        description = stringResource(R.string.select_permission_dialog_by_shizuku_descript),
                        //icon = painterResource(id = R.drawable.ic_launcher_foreground),
                        onClick = {
                            onDismissRequest()
                            if (PermissionTools.isShizukuAvailable) {
                                if (PermissionTools.hasShizukuPermission()) {
                                    ToastUtils.longCall(R.string.toast_permission_granted)
                                } else {
                                    PermissionTools.requestShizukuPermission()
                                }
                            } else {
                                ToastUtils.longCall(R.string.toast_shizuku_not_available)
                            }
                        })
                }
            }, confirmButton = {
                TextButton(onClick = {
                    onDismissRequest()
                }) {
                    Text(text = stringResource(R.string.mod_page_mod_detail_dialog_close))
                }
            })

    }


}

// 请求通知权限

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )
        when (permissionState.status) {
            PermissionStatus.Granted -> {
                showDialog = false
            }

            is PermissionStatus.Denied -> {
                Column {
                    if ((permissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                        val startForResult =
                            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                                if (result.resultCode == Activity.RESULT_OK) {
                                    ToastUtils.longCall(R.string.toast_permission_granted)
                                    showDialog = false
                                } else {
                                    if (!NotificationManagerCompat.from(context)
                                            .areNotificationsEnabled()
                                    ) {
                                        ToastUtils.longCall(R.string.toast_permission_not_granted)
                                    } else {
                                        ToastUtils.longCall(R.string.toast_permission_granted)
                                        showDialog = false
                                    }
                                }
                            }
                        //如果用户拒绝了该权限但可以显示理由，那么请温和地解释为什么应用程序需要此权限(拒绝权限)
                        DialogCommon(
                            title = stringResource(id = R.string.dialog_reqest_notification_title),
                            content = stringResource(id = R.string.dialog_reqest_notification_message),
                            onConfirm = {
                                val intent = Intent().apply {
                                    action = "android.settings.APP_NOTIFICATION_SETTINGS"

                                    // for Android 5-7
                                    putExtra("app_package", context.packageName)
                                    putExtra("app_uid", context.applicationInfo.uid)

                                    // for Android 8 and above
                                    putExtra(
                                        "android.provider.extra.APP_PACKAGE",
                                        context.packageName
                                    )
                                }
                                startForResult.launch(intent)
                            },
                            onCancel = {
                                // 直接关闭应用
                                showDialog = false
                            },
                            showDialog = showDialog
                        )

                    } else {

                        //如果这是用户第一次登陆此功能，或者用户不想再次被要求获得此权限，请说明该权限是必需的(用��选择拒绝且不再询问)
                        DialogCommon(
                            title = stringResource(id = R.string.dialog_reqest_notification_title),
                            content = stringResource(id = R.string.dialog_reqest_notification_message),
                            onConfirm = {
                                Log.d("RequestNotificationPermission", "第一次执行通知请求: ")
                                permissionState.launchPermissionRequest()
                            },
                            onCancel = {
                                // 直接关闭应用
                                showDialog = false
                            },
                            showDialog = showDialog
                        )
                    }
                }
            }
        }

    }

}
