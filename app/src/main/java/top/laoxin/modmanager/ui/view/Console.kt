package top.laoxin.modmanager.ui.view

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.laoxin.modmanager.R
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.commen.RequestStoragePermission
import top.laoxin.modmanager.ui.view.commen.RequestUriPermission
import top.laoxin.modmanager.ui.viewmodel.ConsoleUiState
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ConsoleContent(innerPadding: PaddingValues = PaddingValues(0.dp)) {

    val viewModel: ConsoleViewModel = viewModel(
        factory = ConsoleViewModel.Factory
    )
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {

        // 权限提示框
       RequestStoragePermission()
        // 亲求权限

        if (viewModel.requestPermissionPath.isNotEmpty()) {
            RequestUriPermission(path = viewModel.requestPermissionPath, uiState.openPermissionRequestDialog) {
                viewModel.setOpenPermissionRequestDialog(false)
            }
        }

        GameInformationCard(viewModel, Modifier.align(Alignment.CenterHorizontally))
        // 添加一些间距
        Spacer(modifier = Modifier.height(16.dp))

        // 第二部分：包含两个卡片用于展示其他信息
        SettingInformationCard(viewModel, uiState)
        Spacer(modifier = Modifier.height(16.dp))

        ConfigurationCard(viewModel)

    }
}

// 游戏信息选项卡
@Composable
fun GameInformationCard(viewModel: ConsoleViewModel, modifier: Modifier = Modifier) {
    val gameInfo = viewModel.gameInfo
    // 第一部分：一个卡片展示当前设置项目的一些信息
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 第一个区域：添加一个圆角图片
            Image(
                bitmap = gameInfo.icon, // 替换为你的图片资源
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp)) // 设置图片的圆角
            )

            // 添加一些间距
            Spacer(modifier = Modifier.width(16.dp))

            // 第二个区域：采用垂直布局，添加一些描述信息
            Column {
                Text(
                    text = stringResource(id = R.string.console_game_name, gameInfo.name),
                    style = typography.labelLarge
                )
                Text(
                    text = stringResource(
                        id = R.string.console_game_packegname,
                        gameInfo.packageName
                    ), style = typography.labelLarge
                )
                Text(
                    text = stringResource(
                        id = R.string.console_game_version,
                        gameInfo.versionCode
                    ), style = typography.labelLarge
                )
                Text(
                    text = stringResource(
                        id = R.string.console_game_service,
                        gameInfo.serviceName
                    ), style = typography.labelLarge
                )
            }
        }
    }
}

// 设置信息选项卡

@Composable
fun SettingInformationCard(viewModel: ConsoleViewModel, uiState: ConsoleUiState) {

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.console_setting_info_mod),
                    //modifier = Modifier.padding(16.dp),
                    style = typography.titleLarge
                )
                // 添加一些间距
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        id = R.string.console_setting_info_mod_total,
                        uiState.modCount.toString()
                    ),
                    style = typography.labelLarge
                )
                Text(
                    text = stringResource(
                        id = R.string.console_setting_info_mod_enable,
                        uiState.enableModCount.toString()
                    ),
                    style = typography.labelLarge
                )
            }

        }

        // 添加一些间距
        Spacer(modifier = Modifier.width(16.dp))

        Card(modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.console_setting_info_configuration),
                    //modifier = Modifier.padding(16.dp),
                    style = typography.titleLarge
                )
                // 添加一些间距
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    stringResource(
                        id = R.string.console_setting_info_configuration_anti_harmony,
                        if (uiState.antiHarmony) stringResource(R.string.console_setting_info_configuration_anti_harmony_enable) else stringResource(
                            R.string.console_setting_info_configuration_anti_harmony_disable
                        )
                    ), style = typography.labelLarge
                )
                Text(
                    stringResource(
                        id = R.string.console_setting_info_configuration_install_loction,
                        if (viewModel.canInstallMod) "是" else "否"
                    ),
                    style = typography.labelLarge
                )
            }
        }
    }
}

// 配置选项卡
@Composable
fun ConfigurationCard(viewModel: ConsoleViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val openDirectoryLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            // 这里的uri就是用户选择的目录
            // 你可以在这里处理用户选择的目录
            if (uri != null) {
                // 使用uri
                val path = uri.path?.split(":")?.last()


// 使用"/"将路径部分连接起来

                viewModel.setSelectedDirectory(
                    path ?: context.getString(R.string.console_configuration_mod_path_not_select)
                )
                // TODO: 使用path
            }

        }

    Card {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.console_configuration_title),
                style = typography.titleLarge
            )
            // 添加一些间距
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.console_configuration_anti_harmony),
                    style = typography.titleMedium
                )
                Switch(
                    checked = uiState.antiHarmony,
                    onCheckedChange = { viewModel.openAntiHarmony(it) })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.console_configuration_scanQQ),
                    style = typography.titleMedium
                )
                Switch(checked = uiState.scanQQDirectory, onCheckedChange = {

                    viewModel.openScanQQDirectoryDialog(it)
                })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.console_configuration_scan_download),
                    style = typography.titleMedium
                )
                Switch(checked = uiState.scanDownload, onCheckedChange = {
                    viewModel.openScanDownloadDirectoryDialog(it)
                })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { openDirectoryLauncher.launch(null) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.console_configuration_select_mod_directory),
                        modifier = Modifier.padding(0.dp),
                        style = typography.titleMedium
                    )
                }
                Text(text = uiState.selectedDirectory) // 显示当前选择的文件夹
            }
            // 添加一个按钮，用户点击按钮后，打开文件选择器

        }
    }
}


@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolePage() {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                title = {
                    Text(
                        stringResource(id = R.string.console),
                        style = typography.titleLarge

                    )
                }
            )
        },
    ) { innerPadding ->
        ConsoleContent(innerPadding)
    }
}

// 权限请求弹窗
@Composable
fun PermissionShizukuRequestDialog(
    showDialog: Boolean,
    viewModel: ConsoleViewModel,

    ) {
    val messageOk = stringResource(id = R.string.toast_shizuku_permission_granted)
    val messageNo = stringResource(id = R.string.toast_shizuku_permission_denied)
    val messageNoShizuku = stringResource(id = R.string.toast_shizuku_not_available)
    val context = LocalContext.current
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.setOpenPermissionRequestDialog(false)
            }, // 空的 lambda 函数，表示点击对话框外的区域不会关闭对话框
            title = { Text(stringResource(id = R.string.dialog_shizhuku_title)) },
            text = { Text(stringResource(id = R.string.dialog_shizuku_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.getPermissionShizuku(messageOk, messageNo, messageNoShizuku)
                }) {
                    Text(stringResource(id = R.string.dialog_button_request_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setOpenPermissionRequestDialog(false)
                    viewModel.openUrl(context, "https://shizuku.rikka.app/")
                }) {
                    Text(stringResource(id = R.string.dialog_button_request_download))
                }
            }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.R)
@Preview(showBackground = true)
@Composable
fun PreviewBottomNavigationBar() {
    ModManagerTheme {
        ConsolePage()
    }

}