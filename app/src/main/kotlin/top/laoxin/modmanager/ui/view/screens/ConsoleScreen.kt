package top.laoxin.modmanager.ui.view.screens

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.ui.view.components.common.DialogCommon
import top.laoxin.modmanager.ui.view.components.common.DialogCommonForUpdate
import top.laoxin.modmanager.ui.view.components.common.DialogType
import top.laoxin.modmanager.ui.view.components.common.PermissionHandler
import top.laoxin.modmanager.ui.view.components.common.openUrl
import top.laoxin.modmanager.ui.state.ConsoleUiState
import top.laoxin.modmanager.ui.theme.ExpressiveSwitch
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton

import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel

@SuppressLint("NewApi")
@Composable
fun ConsoleScreen(viewModel: ConsoleViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    DialogCommon(
            title = stringResource(id = R.string.console_scan_directory_mods),
            type = DialogType.WARNING,
            content = stringResource(id = R.string.console_scan_directory_mods_content),
            onConfirm = { viewModel.switchScanDirectoryMods(false) },
            onCancel = { viewModel.setShowScanDirectoryModsDialog(false) },
            showDialog = uiState.showScanDirectoryModsDialog
    )

    // Upgrade Dialog
    uiState.updateInfo?.let {
        DialogCommonForUpdate(
                title = stringResource(id = R.string.console_upgrade_title),
                content = it.changelog,
                onConfirm = { context.openUrl(it.downloadUrl) },
                onDismiss = { viewModel.setShowUpgradeDialog(false) },
                showDialog = uiState.showUpgradeDialog
        )
    }

    // Permission Handler
    PermissionHandler(
            permissionStateFlow = viewModel.permissionState,
            onPermissionGranted = viewModel::onPermissionGranted,
            onPermissionDenied = viewModel::onPermissionDenied,
            onRequestShizuku = viewModel::requestShizukuPermission,
            isShizukuAvailable = viewModel.isShizukuAvailable()
    )

    // Info Dialog
    uiState.infoBean?.let {
        DialogCommon(
                title = stringResource(id = R.string.console_info_title),
                content = it.msg,
                onConfirm = { viewModel.setShowInfoDialog(false) },
                onCancel = { viewModel.setShowInfoDialog(false) },
                showDialog = uiState.showInfoDialog
        )
    }

    Column(
            modifier =
                    Modifier.padding(start = 16.dp, end = 16.dp)
                            .padding(0.dp)
                            .fillMaxSize()
                            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        GameInformationCard(
                viewModel,
                uiState.gameInfo,
                Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingInformationCard(viewModel, uiState)

        Spacer(modifier = Modifier.height(16.dp))
        ConfigurationCard(viewModel, uiState)
    }
}

// ----------------------------------------------------------------------
// Reused Components (Copied from Console.kt)
// ----------------------------------------------------------------------

@Composable
fun GameInformationCard(
        viewModel: ConsoleViewModel,
        gameInfo: GameInfoBean,
        modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = MaterialTheme.shapes.large) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                    bitmap = viewModel.getGameIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.extraLarge)
            )

            Spacer(modifier = Modifier.width(16.dp))

            if (gameInfo == GameInfoConstant.NO_GAME) {
                Column {
                    Text(
                            text = App.get().getString(R.string.toast_please_select_game),
                            style = typography.titleLarge
                    )
                }
            } else {
                Column {
                    Text(
                            text =
                                    stringResource(
                                            id = R.string.console_game_name,
                                            gameInfo.gameName
                                    ),
                            style = typography.labelLarge
                    )
                    Text(
                            text =
                                    stringResource(
                                            id = R.string.console_game_packegname,
                                            gameInfo.packageName
                                    ),
                            style = typography.labelLarge
                    )
                    Text(
                            text =
                                    stringResource(
                                            id = R.string.console_game_version,
                                            viewModel.getGameVersion(gameInfo)
                                    ),
                            style = typography.labelLarge
                    )
                    Text(
                            text =
                                    stringResource(
                                            id = R.string.console_game_service,
                                            gameInfo.serviceName
                                    ),
                            style = typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SettingInformationCard(viewModel: ConsoleViewModel, uiState: ConsoleUiState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.console_setting_info_mod),
                        style = typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        text =
                                stringResource(
                                        id = R.string.console_setting_info_mod_total,
                                        uiState.modCount.toString()
                                ),
                        style = typography.labelLarge
                )
                Text(
                        text =
                                stringResource(
                                        id = R.string.console_setting_info_mod_enable,
                                        uiState.enableModCount.toString()
                                ),
                        style = typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.console_setting_info_configuration),
                        style = typography.titleLarge
                )

                Spacer(modifier = Modifier.height(14.dp))
                val fileAccessType: FileAccessType = viewModel.checkFileAccessType()
                Text(
                        when (fileAccessType) {
                            FileAccessType.STANDARD_FILE ->
                                    stringResource(id = R.string.permission, "FILE")
                            FileAccessType.DOCUMENT_FILE ->
                                    stringResource(id = R.string.permission, "DOCUMENT")
                            FileAccessType.SHIZUKU ->
                                    stringResource(id = R.string.permission, "SHIZUKU")
                            FileAccessType.NONE -> stringResource(id = R.string.permission, "NONE")
                        },
                        style = typography.labelLarge
                )
                Text(
                        stringResource(
                                id = R.string.console_setting_info_configuration_install_loction,
                                if (uiState.canInstallMod)
                                        stringResource(
                                                R.string
                                                        .console_setting_info_configuration_can_install
                                        )
                                else
                                        stringResource(
                                                R.string
                                                        .console_setting_info_configuration_not_install
                                        )
                        ),
                        style = typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun ConfigurationCard(viewModel: ConsoleViewModel, uiState: ConsoleUiState) {
    val openDirectoryLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                if (uri != null) {
                    val path =
                            uri.path?.split(":")?.last()?.replace("${PathConstants.ROOT_PATH}/", "")

                    viewModel.setSelectedDirectory(
                            path
                                    ?: (PathConstants.ROOT_PATH +
                                            "/" +
                                            PathConstants.DOWNLOAD_MOD_PATH)
                    )
                }
            }

    Card {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                    text = stringResource(id = R.string.console_configuration_title),
                    style = typography.titleLarge
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.gameInfo.antiHarmonyFile.isNotEmpty() ||
                            uiState.gameInfo.antiHarmonyContent.isNotEmpty()
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                            text = stringResource(id = R.string.console_configuration_anti_harmony),
                            style = typography.titleMedium
                    )
                    ExpressiveSwitch(
                            checked = uiState.antiHarmony,
                            onCheckedChange = { viewModel.openAntiHarmony(it) }
                    )
                }
            }

            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                        text =
                                stringResource(
                                        id = R.string.console_configuration_disable_scan_dictionary
                                ),
                        style = typography.titleMedium
                )
                ExpressiveSwitch(
                        checked = uiState.scanDirectoryMods,
                        onCheckedChange = { viewModel.switchScanDirectoryMods(it) }
                )
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
                ExpressiveSwitch(
                        checked = uiState.scanQQDirectory,
                        onCheckedChange = { viewModel.switchScanQQDirectory(it) }
                )
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
                ExpressiveSwitch(
                        checked = uiState.scanDownload,
                        onCheckedChange = { viewModel.switchScanDownloadDirectory(it) }
                )
            }

            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                        text =
                                stringResource(
                                        id = R.string.console_configuration_conflict_detection
                                ),
                        style = typography.titleMedium
                )
                ExpressiveSwitch(
                        checked = uiState.conflictDetectionEnabled,
                        onCheckedChange = { viewModel.switchConflictDetection(it) }
                )
            }

            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                ExpressiveTextButton(
                        onClick = { openDirectoryLauncher.launch(null) },
                        contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                            text =
                                    stringResource(
                                            id = R.string.console_configuration_select_mod_directory
                                    ),
                            modifier = Modifier.padding(0.dp),
                            style = typography.titleMedium
                    )
                }
                Text(text = uiState.selectedDirectory)
            }
        }
    }
}
