package top.laoxin.modmanager.ui.view.common

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow
import top.laoxin.modmanager.App
import top.laoxin.modmanager.ui.state.PermissionRequestState

/**
 * 权限请求处理器 整合 Dialog + Launchers，提供完整的权限请求流程
 *
 * 使用方式：
 * ```kotlin
 * @Composable
 * fun MyScreen(viewModel: MyViewModel) {
 *     PermissionHandler(
 *         permissionStateFlow = viewModel.permissionState,
 *         onPermissionGranted = { viewModel.onPermissionGranted() },
 *         onPermissionDenied = { viewModel.onPermissionDenied() },
 *         onRequestShizuku = { viewModel.requestShizukuPermission() },
 *         isShizukuAvailable = viewModel.isShizukuAvailable
 *     )
 *
 *     // 页面其他内容...
 * }
 * ```
 *
 * @param permissionStateFlow 权限请求状态 Flow
 * @param onPermissionGranted 权限授予回调
 * @param onPermissionDenied 权限被拒绝回调
 * @param onRequestShizuku 请求 Shizuku 权限回调
 * @param isShizukuAvailable Shizuku 是否可用
 */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PermissionHandler(
        permissionStateFlow: StateFlow<PermissionRequestState>,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit,
        onRequestShizuku: () -> Unit,
        isShizukuAvailable: Boolean = false
) {
    val permissionState by permissionStateFlow.collectAsState()
    val  context = LocalContext.current


    // SAF 权限 launcher
    val safLauncher =
            rememberSAFPermissionLauncher(
                    onPermissionGranted = { _ -> onPermissionGranted() },
                    onPermissionDenied = onPermissionDenied
            )

    // 全局存储权限 launcher
    val storageLauncher =
            rememberStoragePermissionLauncher(
                    onPermissionGranted = onPermissionGranted,
                    onPermissionDenied = onPermissionDenied
            )

    // 通知权限 launcher
    val notificationLauncher =
            rememberNotificationPermissionLauncher(
                    onPermissionGranted = onPermissionGranted,
                    onPermissionDenied = onPermissionDenied
            )

    // 显示权限对话框
    PermissionRequestDialog(
            state = permissionState,
            osVersion = App.osVersion,
            isShizukuAvailable = isShizukuAvailable,
            onRequestSAF = { path ->
                val intent = createSAFIntent(path, context)
                safLauncher.launch(intent)
            },
            onRequestShizuku = { onRequestShizuku() },
            onRequestStorage = {
                val intent = createStoragePermissionIntent(context)
                storageLauncher.launch(intent)
            },
            onDismiss = onPermissionDenied
    )
}

/** 简化版权限处理器 用于只需要 SAF 权限的简单场景 */
@Composable
fun SimpleSAFPermissionHandler(
        showDialog: Boolean,
        requestPath: String,
        onPermissionGranted: (Uri) -> Unit,
        onPermissionDenied: () -> Unit
) {
    val  context = LocalContext.current

    val safLauncher =
            rememberSAFPermissionLauncher(
                    onPermissionGranted = onPermissionGranted,
                    onPermissionDenied = onPermissionDenied
            )

    if (showDialog) {
        SAFOrShizukuPermissionDialog(
                requestPath = requestPath,
                osVersion = App.osVersion,
                isShizukuAvailable = false,
                onRequestSAF = { path ->
                    val intent = createSAFIntent(path,context)
                    safLauncher.launch(intent)
                },
                onRequestShizuku = { /* 不支持 */},
                onDismiss = onPermissionDenied
        )
    }
}
