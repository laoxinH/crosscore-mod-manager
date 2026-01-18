package top.laoxin.modmanager.ui.view.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.ui.view.components.setting.SettingItem
import top.laoxin.modmanager.ui.state.PermissionRequestState
import top.laoxin.modmanager.ui.state.PermissionType

/**
 * 权限请求对话框 纯 UI 组件，根据权限类型显示不同的选项
 *
 * @param state 权限请求状态
 * @param osVersion 操作系统版本
 * @param isShizukuAvailable Shizuku 是否可用
 * @param onRequestSAF 用户选择 SAF 方式
 * @param onRequestShizuku 用户选择 Shizuku 方式
 * @param onRequestStorage 用户选择全局存储权限
 * @param onRequestNotification 用户选择通知权限
 * @param onDismiss 用户关闭对话框
 */
@Composable
fun PermissionRequestDialog(
    state: PermissionRequestState,
    osVersion: OSVersion = OSVersion.OS_11,
    isShizukuAvailable: Boolean = false,
    onRequestSAF: (String) -> Unit,
    onRequestShizuku: (() -> Unit),
    onRequestStorage: () -> Unit = {},
    onRequestNotification: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (!state.showDialog) return

    when (state.permissionType) {
        PermissionType.STORAGE -> {
            StoragePermissionDialog(onConfirm = onRequestStorage, onDismiss = onDismiss)
        }

        PermissionType.URI_SAF, PermissionType.SHIZUKU -> {
            SAFOrShizukuPermissionDialog(
                requestPath = state.requestPath,
                osVersion = osVersion,
                isShizukuAvailable = isShizukuAvailable,
                onRequestSAF = onRequestSAF,
                onRequestShizuku = onRequestShizuku,
                onDismiss = onDismiss
            )
        }

        PermissionType.NOTIFICATION -> {
            NotificationPermissionDialog(onConfirm = onRequestNotification, onDismiss = onDismiss)
        }

        PermissionType.NONE -> {
            /* 不显示 */
        }
    }
}

/** 全局存储权限请求对话框 (Android 11+) */
@Composable
fun StoragePermissionDialog(onConfirm: () -> Unit = {}, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.dialog_storage_title)) },
        text = { Text(stringResource(R.string.dialog_storage_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_request_close))
            }
        }
    )
}

/** SAF 或 Shizuku 权限选择对话框 */
@Composable
fun SAFOrShizukuPermissionDialog(
    requestPath: String,
    osVersion: OSVersion,
    isShizukuAvailable: Boolean,
    onRequestSAF: (String) -> Unit,
    onRequestShizuku: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.choose_method_title)) },
        text = {
            Column {
                Text(stringResource(R.string.select_permission_dialog_descript))
                Spacer(modifier = Modifier.height(10.dp))

                // SAF 选项 (仅 Android 11/13 支持)
                if (osVersion == OSVersion.OS_11 || osVersion == OSVersion.OS_13) {
                    SettingItem(
                        name = stringResource(R.string.select_permission_dialog_by_file),
                        description =
                            stringResource(
                                R.string.select_permission_dialog_by_file_descript
                            ),
                        onClick = {
                            onDismiss()
                            onRequestSAF(requestPath)
                        }
                    )
                }

                // Shizuku 选项
                SettingItem(
                    name = stringResource(R.string.select_permission_dialog_by_shizuku),
                    description =
                        if (isShizukuAvailable) {
                            stringResource(
                                R.string
                                    .select_permission_dialog_by_shizuku_descript
                            )
                        } else {
                            stringResource(R.string.toast_shizuku_not_available)
                        },
                    onClick = {
                        onDismiss()
                        onRequestShizuku()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mod_page_mod_detail_dialog_close))
            }
        }
    )
}

/** 通知权限请求对话框 */
@Composable
fun NotificationPermissionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.dialog_reqest_notification_title)) },
        text = { Text(stringResource(R.string.dialog_reqest_notification_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_request_close))
            }
        }
    )
}
