package top.laoxin.modmanager.ui.view.modView

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.theme.ExpressiveCircularProgressIndicator
import top.laoxin.modmanager.ui.theme.ExpressiveOutlinedTextField
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton
import top.laoxin.modmanager.ui.view.common.DialogCommon
import top.laoxin.modmanager.ui.view.common.PermissionHandler
import top.laoxin.modmanager.ui.viewmodel.MainViewModel
import top.laoxin.modmanager.ui.viewmodel.ModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel

// lateinit var viewModel: ModViewModel
enum class NavigationIndex(
    @param:StringRes val title: Int,
    val index: Int,
) {
    ALL_MODS(R.string.mod_page_title_all_mods, 0),
    ENABLE_MODS(R.string.mod_page_title_enable_mods, 1),
    DISABLE_MODS(R.string.mod_page_title_disable_mods, 2),
    SEARCH_MODS(R.string.mod_page_title_search_mods, 3),
    MODS_BROWSER(R.string.mod_page_title_mods_browser, 4),
}

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModPage(
    modScanViewModel: ModScanViewModel,
    modOperationViewModel: ModOperationViewModel,
    modListViewModel: ModListViewModel,
    modDetailViewModel: ModDetailViewModel,
    modSearchViewModel: ModSearchViewModel,
    modBrowserViewModel: ModBrowserViewModel
) {
    // 获取 MainViewModel 用于导航
    val mainViewModel: MainViewModel = hiltViewModel()

    // 收集各个ViewModel的状态
    // 收集状态
    val modBrowserUiState by modBrowserViewModel.uiState.collectAsState()

    val modListUiState = modListViewModel.uiState.collectAsState()
    val modList = modListUiState.value.modList
    val isMultiSelect = modListUiState.value.isMultiSelect
    val modsSelected = modListUiState.value.modsSelected
    // 收集操作状态
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modDetailUiState by modDetailViewModel.uiState.collectAsState()
    val modSwitchEnable = modOperationUiState.modSwitchEnable

    // 收集扫描状态
    val modScanUiState by modScanViewModel.uiState.collectAsState()
    val scanState = modScanUiState.isLoading
    val loadingPath = modScanUiState.loadingPath
    val showDisEnableModsDialog = modScanUiState.showDisEnableModsDialog
    val delEnableModsList = modScanUiState.delEnableModsList
    val showForceScanDialog = modScanUiState.showForceScanDialog
    val openPermissionRequestDialog = modScanUiState.openPermissionRequestDialog
    val requestPermissionPath = modScanUiState.requestPermissionPath

    val showOpenFailedDialog = modOperationUiState.showOpenFailedDialog
    val openFailedMods = modOperationUiState.openFailedMods
    val showPasswordDialog = modOperationUiState.showPasswordDialog

    val modDetail = modDetailUiState.mod
    val showModDetail = modDetailUiState.isShown

    val modsView = modBrowserUiState.modsView

    // 临时使用一个简化的状态，后续会完全移除对ModUiState的依赖
    val isReady = true // 暂时硬编码
    val isInitializing = false // 暂时硬编码

    // 扫描权限处理器 - 一行代码集成
    PermissionHandler(
        permissionStateFlow = modScanViewModel.permissionState,
        onPermissionGranted = modScanViewModel::onPermissionGranted,
        onPermissionDenied = modScanViewModel::onPermissionDenied,
        onRequestShizuku = modScanViewModel::requestShizukuPermission,
        isShizukuAvailable = modScanViewModel.isShizukuAvailable()
    )

    // 开启权限处理器 - 一行代码集成
    PermissionHandler(
        permissionStateFlow = modOperationViewModel.permissionState,
        onPermissionGranted = modOperationViewModel::onPermissionGranted,
        onPermissionDenied = modOperationViewModel::onPermissionDenied,
        onRequestShizuku = modOperationViewModel::requestShizukuPermission,
        isShizukuAvailable = modOperationViewModel.isShizukuAvailable()
    )

    // 密码提示对话框
    if (modOperationUiState.showPasswordDialog) {
        PasswordInputDialog(
            mod = modOperationUiState.passwordRequestMod,
            errorMessage = modOperationUiState.passwordError,
            onDismiss = { modOperationViewModel.dismissPasswordDialog() },
            onSubmit = { modOperationViewModel.submitPassword(it) }
        )
    }

    Box {
      /*  // 显示无效mod确认对话框
        DisEnableModsDialog(
            showDialog = showDisEnableModsDialog,
            mods = delEnableModsList,
            switchMod = { mod, enable -> modOperationViewModel.switchMod(mod, enable, true) },
            onConfirmRequest = { modScanViewModel.confirmDeleteMods() },
            modDetailViewModel = modDetailViewModel
        )*/

        // 显示强制扫描对话框
        ForceUpdateDialog(showDialog = showForceScanDialog, modScanViewModel = modScanViewModel)

/*        // 显示开启失败是否回滚MODS的弹窗
        DialogCommon(
            title = stringResource(R.string.open_mod_failed_dialog_title),
            content = stringResource(R.string.open_mod_failed_dialog_desc, openFailedMods.size),
            onConfirm = {
                modOperationViewModel.setShowOpenFailedDialog(false)
                modOperationViewModel.switchSelectMods(openFailedMods, false)
            },
            onCancel = { modOperationViewModel.setShowOpenFailedDialog(false) },
            showDialog = showOpenFailedDialog
        )*/

        if (scanState && modScanUiState.scanProgress == null) {
            // 兼容旧的 Loading 显示（如果没有新的进度状态）
            Loading(loadingPath)
        } else if (isInitializing) {
            // 显示初始化状态
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ExpressiveCircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.mod_page_initializing),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        } else if (!isReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.mod_page_no_game),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {

            // if (modBrowserUiState.showCategory)
            AnimatedContent(
                targetState = modsView,
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                label = "ModViewAnimation"
            ) { targetState ->
                when (targetState) {
                    NavigationIndex.MODS_BROWSER ->
                        ModsBrowser(
                            modBrowserViewModel = modBrowserViewModel,
                            modListViewModel = modListViewModel,
                            modDetailViewModel = modDetailViewModel,
                            modOperationViewModel = modOperationViewModel,
                            modSearchViewModel = modSearchViewModel,
                        )

                    else ->
                        AllModPage(
                            modListViewModel = modListViewModel,
                            modDetailViewModel = modDetailViewModel,
                            modOperationViewModel = modOperationViewModel,
                            modSearchViewModel = modSearchViewModel,
                            modBrowserViewModel = modBrowserViewModel
                        )
                }
            }
        }

        // mod详情
        modDetail?.let {
            ModDetailPartialBottomSheet(
                showDialog = showModDetail,
                mod = it,
                modDetailViewModel = modDetailViewModel,
                modOperationViewModel = modOperationViewModel,
                modScanViewModel = modScanViewModel,
                onDismiss = { modDetailViewModel.setShowModDetail(false) }
            )
            /*PasswordInputDialog(
                    showDialog = showPasswordDialog,
                    onDismiss = { modOperationViewModel.setShowPasswordDialog(false) },
                    onPasswordSubmit = { password ->
                        modOperationViewModel.checkPassword(it, password)
                    }
            )*/
        }
        // Log.d("ModPage", "扫描状态: ${modScanUiState.scanProgress}")
        // 扫描进度覆盖层 - 放在最后以确保显示在所有内容之上
        ScanProgressOverlay(
            progressState = modScanUiState.scanProgress,
            onCancel = { modScanViewModel.cancelScan() },
            onDismiss = { modScanViewModel.dismissScanResult() },
            onGoSettings = {
                modScanViewModel.dismissScanResult()
                mainViewModel.navigateToSettings()
            },
            onGrantPermission = modScanViewModel::requestPermissionFromError,
            onDisableMod = { mod ->
                // 关闭已删除但仍启用的MOD（静默模式，不显示进度遮蔽层）
                modOperationViewModel.switchSelectMods(listOf(mod), false, silent = true)
            },
            onDisableAllMods = { mods ->
                // 关闭所有已删除但仍启用的MOD（静默模式）
                modOperationViewModel.switchSelectMods(mods, false, silent = true)
                modScanViewModel.dismissScanResult()
            }
        )

        // MOD 开启/关闭进度覆盖层
        EnableProgressOverlay(
            progressState = modOperationUiState.enableProgress,
            onCancel = { modOperationViewModel.cancelOperation() },
            onDismiss = { modOperationViewModel.dismissEnableProgress() },
            onGoSettings = {
                modOperationViewModel.dismissEnableProgress()
                mainViewModel.navigateToSettings()
            },
            onGrantPermission = modOperationViewModel::requestPermissionFromEnableError,
            onDisableMod = { mod ->
                // 关闭与当前MOD冲突的已开启MOD（静默模式）
                modOperationViewModel.switchSelectMods(listOf(mod), false, silent = true)
            },
            onRemoveFromSelection = { mod ->
                // 从多选列表中移除该MOD
                modListViewModel.removeModSelection(mod.id)
            }
        )

        // 解密进度覆盖层
        DecryptProgressOverlay(
            progressState = modOperationUiState.decryptProgress,
            onCancel = { modOperationViewModel.cancelDecrypt() },
            onConfirm = { modOperationViewModel.confirmDecryptSuccess() },
            onDismiss = { modOperationViewModel.dismissDecryptProgress() }
        )

        // MOD 删除进度覆盖层
        DeleteProgressOverlay(
            progressState = modOperationUiState.deleteProgress,
            onCancel = { modOperationViewModel.cancelDelete() },
            onDismiss = { modOperationViewModel.dismissDeleteProgress() },
            onDisableMod = { mod ->
                modOperationViewModel.switchSelectMods(
                    listOf(mod),
                    enable = false,
                    silent = true
                )
            },
            onDisableAllMods = { mods ->
                modOperationViewModel.switchSelectMods(
                    mods,
                    enable = false,
                    silent = true
                )
            }
        )

        // 删除检测结果覆盖层
        DeleteCheckConfirmDialog(
            checkState = modOperationUiState.deleteCheckState,
            onCancel = { modOperationViewModel.dismissDeleteCheck() },
            onSkipIntegrated = { modOperationViewModel.confirmDeleteSkipIntegrated() },
            onDeleteAll = { modOperationViewModel.confirmDeleteAll() },
            onDisableMod = { mod ->
                modOperationViewModel.switchSelectMods(
                    listOf(mod),
                    enable = false,
                    silent = true
                )
            },
            onDisableAllMods = { mods ->
                modOperationViewModel.switchSelectMods(
                    mods,
                    enable = false,
                    silent = true
                )
            },
        )
    }
}

@Composable
fun ForceUpdateDialog(showDialog: Boolean, modScanViewModel: ModScanViewModel) {
    if (showDialog) {
        DialogCommon(
            title = stringResource(id = R.string.console_scan_directory_mods),
            content = stringResource(id = R.string.mod_page_force_update_mod_warning),
            onConfirm = {
                modScanViewModel.flashMods(isLoading = true, forceScan = true)
                modScanViewModel.setShowForceScanDialog(false)
            },
            onCancel = { modScanViewModel.setShowForceScanDialog(false) },
            showDialog = showDialog
        )
    }
}

// 关闭mods提示框
@Composable
fun DisEnableModsDialog(
    showDialog: Boolean,
    mods: List<ModBean>,
    switchMod: (ModBean, Boolean) -> Unit,
    onConfirmRequest: () -> Unit,
    modDetailViewModel: ModDetailViewModel,
) {
    if (showDialog) {
        if (mods.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                shape = MaterialTheme.shapes.extraLarge,
                title = {
                    Text(
                        stringResource(id = R.string.dialog_dis_enable_mods_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                text = {
                    LazyColumn {
                        itemsIndexed(mods) { _, mod ->
                            ModListItem(
                                mod = mod,
                                onLongClick = {},
                                onMultiSelectClick = {},
                                modSwitchEnable = true,
                                openModDetail = {},
                                enableMod = switchMod,
                                modDetailViewModel = modDetailViewModel,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    ExpressiveTextButton(onClick = { onConfirmRequest() }) {
                        Text(stringResource(id = R.string.dialog_button_confirm))
                    }
                },
                dismissButton = {}
            )
        } else {
            onConfirmRequest()
        }
    }
}

// 使用缓存加载图片，保持向后兼容
@Deprecated("使用 loadImageBitmapWithCache 或 rememberImageBitmap 代替")
suspend fun loadImageBitmapFromPath(
    context: Context,
    path: String,
    reqWidth: Int,
    reqHeight: Int
): ImageBitmap? {
    return loadImageBitmapWithCache(context, path, reqWidth, reqHeight)
}

// 创建一个全屏的加载动画
@Composable
fun Loading(loadingPath: String = "loading") {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ExpressiveCircularProgressIndicator()
        Text(
            text = stringResource(R.string.mod_pag_loading, loadingPath),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

// 输入密码的弹窗
@Composable
fun PasswordInputDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPasswordSubmit: (String) -> Unit
) {
    if (showDialog) {
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = stringResource(R.string.password_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                ExpressiveOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_dialog_label)) },
                )
            },
            confirmButton = {
                ExpressiveTextButton(
                    onClick = {
                        onPasswordSubmit(password)
                        onDismiss()
                    }
                ) { Text(text = stringResource(id = R.string.dialog_button_confirm)) }
            },
            dismissButton = {
                ExpressiveTextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.dialog_button_request_close))
                }
            }
        )
    }
}
