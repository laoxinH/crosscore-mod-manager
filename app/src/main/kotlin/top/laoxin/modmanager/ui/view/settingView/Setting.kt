package top.laoxin.modmanager.ui.view.settingView

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.bean.DownloadGameConfigBean
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.bean.ThanksBean
import top.laoxin.modmanager.ui.state.SettingUiState
import top.laoxin.modmanager.ui.view.commen.DialogCommon
import top.laoxin.modmanager.ui.view.commen.DialogCommonForUpdate
import top.laoxin.modmanager.ui.view.commen.RequestUriPermission
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel

@Composable
fun SettingPage(viewModel: SettingViewModel, onHideBottomBar: (Boolean) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (uiState.showAbout) {
                    onHideBottomBar(available.y < 0)
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
        AnimatedContent(
            targetState = uiState.showAbout,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            }
        ) { showAbout ->
            when {
                showAbout -> {
                    License(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        viewModel = viewModel
                    )
                }

                else -> {
                    SettingContent(
                        uiState,
                        viewModel::setAboutPage,
                        viewModel::setDeleteBackupDialog,
                        viewModel::setDeleteCacheDialog,
                        viewModel::deleteAllBackups,
                        viewModel::deleteCache,
                        viewModel::deleteTemp,
                        viewModel::openUrl,
                        viewModel::showAcknowledgments,
                        viewModel::showSwitchGame,
                        viewModel::flashGameConfig,
                        viewModel::checkUpdate,
                        viewModel::checkInformation,
                        viewModel::setShowDownloadGameConfig,
                        viewModel::requestShizukuPermission
                    )
                }
            }
        }
    }

    ThanksDialogCommon(
        title = stringResource(R.string.setting_acknowledgments),
        onConfirm = { viewModel.showAcknowledgments(false) },
        onCancel = { viewModel.showAcknowledgments(false) },
        thinks = uiState.thinksList,
        openUrl = viewModel::openUrl,
        showDialog = uiState.showAcknowledgments
    )
    if (viewModel.requestPermissionPath.isNotEmpty()) {
        RequestUriPermission(
            path = viewModel.requestPermissionPath, uiState.openPermissionRequestDialog,
            onDismissRequest = { viewModel.setOpenPermissionRequestDialog(false) },
            permissionTools = viewModel.getPermissionTools(),
            fileTools = viewModel.getFileToolsManager().getFileTools()
        )
    }
    viewModel.updateContent?.let {
        DialogCommonForUpdate(
            title = stringResource(id = R.string.console_upgrade_title),
            content = it,
            onConfirm = {
                viewModel.downloadUrl?.let { url -> viewModel.openUrl(context, url) }
            },
            onDismiss = {
                viewModel.universalUrl?.let { url -> viewModel.openUrl(context, url) }
            },
            showDialog = uiState.showUpdateDialog
        )
    }

    DialogCommon(
        title = stringResource(id = R.string.console_game_tips_title),
        content = viewModel.gameInfo.tips,
        onConfirm = {
            viewModel.setGameInfo(viewModel.gameInfo, true)
            viewModel.setShowGameTipsDialog(false)
        },
        onCancel = {
            viewModel.setShowGameTipsDialog(false)
        },
        showDialog = uiState.showGameTipsDialog
    )
    DialogCommon(
        title = stringResource(id = R.string.console_info_title),
        content = uiState.infoBean.msg,
        onConfirm = {
            viewModel.setShowInfoDialog(false)
        },
        onCancel = {
            viewModel.setShowInfoDialog(false)
        },
        showDialog = uiState.showNotificationDialog
    )
    SwitchGameDialog(
        gameInfoList = uiState.gameInfoList,
        setGameInfo = viewModel::setGameInfo,
        showSwitchGameInfo = viewModel::showSwitchGame,
        showDialog = uiState.showSwitchGame
    )
    DownloadGameConfigDialog(
        gameInfoList = uiState.downloadGameConfigList,
        downloadGameConfig = viewModel::downloadGameConfig,
        showDownloadGameConfigDialog = viewModel::setShowDownloadGameConfig,
        showDialog = uiState.showDownloadGameConfigDialog
    )
}


@Composable
fun SettingContent(
    uiState: SettingUiState,
    setAboutPage: (Boolean) -> Unit,
    setDeleteBackupDialog: (Boolean) -> Unit,
    setDeleteCacheDialog: (Boolean) -> Unit,
    deleteAllBackups: () -> Unit,
    deleteCache: () -> Unit,
    deleteTemp: () -> Unit,
    openUrl: (Context, String) -> Unit,
    showAcknowledgments: (Boolean) -> Unit,
    showSwitchGame: (Boolean) -> Unit,
    flashGameConfig: () -> Unit,
    checkUpdate: () -> Unit,
    checkInformation: () -> Unit,
    showDownloadGameConfig: (Boolean) -> Unit,
    requestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    DialogCommon(
        title = stringResource(R.string.setting_del_backups_dialog_title),
        content = stringResource(R.string.setting_del_backups_dialog_content_txt),
        onConfirm = { deleteAllBackups() },
        onCancel = { setDeleteBackupDialog(false) },
        showDialog = uiState.deleteBackupDialog,
    )
    DialogCommon(
        title = stringResource(R.string.setting_del_backups_dialog_title),
        content = stringResource(R.string.setting_del_cache_dialog_content_txt),
        onConfirm = { deleteCache() },
        onCancel = { setDeleteCacheDialog(false) },
        showDialog = uiState.deleteCacheDialog,
    )
    Column(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingTitle(
            title = stringResource(R.string.setting_page_app_title),
            icon = Icons.Default.Settings
        )
        SettingItem(
            name = stringResource(R.string.lincence),
            description = stringResource(R.string.show_lincence),
            onClick = {
                setAboutPage(!uiState.showAbout)
            })
        SettingItem(
            name = stringResource(R.string.setting_page_app_del_backup),
            description = stringResource(R.string.setting_page_app_del_descript),
            onClick = { setDeleteBackupDialog(true) })
        SettingItem(
            name = stringResource(R.string.setting_page_app_clean_cache),
            description = stringResource(R.string.setting_page_app_clean_cache_descript),
            onClick = { setDeleteCacheDialog(true) })
        SettingItem(
            name = stringResource(R.string.setting_page_app_clean_temp),
            description = stringResource(R.string.setting_page_app_clean_temp_descript),
            onClick = { deleteTemp() })
        SettingItem(
            name = stringResource(R.string.setting_page_app_download_game_config),
            description = stringResource(R.string.setting_page_app_download_game_config_descript),
            onClick = { showDownloadGameConfig(true) })
        SettingItem(
            name = stringResource(R.string.setting_page_app_flash_game_config),
            description = stringResource(R.string.setting_page_app_flash_game_config_descript),
            onClick = { flashGameConfig() })
        SettingItem(
            name = stringResource(R.string.setting_page_app_swtch_permission_shizuku),
            description = stringResource(R.string.setting_page_app_swtch_permission_shizuku_desc),
            onClick = { requestShizukuPermission() })
        SettingItem(
            name = stringResource(R.string.setting_page_app_swtch_game),
            description = stringResource(R.string.setting_page_app_swtch_game_descript),
            onClick = { showSwitchGame(true) })

        SettingTitle(
            title = stringResource(R.string.setting_page_about_title),
            icon = Icons.Default.Info
        )
        SettingItem(
            name = stringResource(R.string.setting_page_about_author),
            description = stringResource(R.string.setting_page_about_github),
            icon = painterResource(id = R.drawable.github_icon),
            onClick = {
                openUrl(context, context.getString(R.string.github_url_releases_latest))
            })
        SettingItem(
            name = stringResource(R.string.setting_page_about_pay),
            description = stringResource(R.string.setting_page_about_pay_descript),
            icon = painterResource(id = R.drawable.alipay_icon),
            onClick = {
                openUrl(context, context.getString(R.string.alipay_url))
            })
        SettingItem(
            name = stringResource(R.string.setting_page_about_update),
            description = stringResource(
                R.string.setting_page_about_update_descript, uiState.versionName
            ),
            icon = painterResource(id = R.drawable.update_icon),
            onClick = {
                checkUpdate()
            })
        SettingItem(
            name = stringResource(R.string.setting_page_about_info),
            description = stringResource(R.string.setting_page_about_info_descript),
            icon = painterResource(id = R.drawable.notification_icon),
            onClick = {
                checkInformation()
            })

        SettingTitle(
            title = stringResource(R.string.setting_page_app_other),
            icon = Icons.Default.MoreVert
        )
        SettingItem(
            name = stringResource(R.string.setting_page_more_shizuku),
            description = stringResource(R.string.setting_page_more_shizuku_descript),
            icon = painterResource(id = R.drawable.shizuku_icon),
            onClick = {
                openUrl(context, context.getString(R.string.shzuiku_url))
            })
        SettingItem(
            name = stringResource(R.string.setting_page_more_reference),
            description = stringResource(R.string.setting_page_more_reference_descript),
            icon = painterResource(id = R.drawable.book_icon),
            onClick = {
                openUrl(context, context.getString(R.string.reference_url))
            })
        SettingItem(
            name = stringResource(R.string.setting_page_more_qq),
            description = stringResource(R.string.setting_page_more_qq_descript),
            icon = painterResource(id = R.drawable.qq_icon),
            onClick = {
                openUrl(context, context.getString(R.string.qq_url))
            })
        SettingItem(
            name = stringResource(R.string.setting_page_more_discord),
            description = stringResource(R.string.setting_page_more_discord_descript),
            icon = painterResource(id = R.drawable.discord_icon),
            onClick = {
                openUrl(context, context.getString(R.string.disscord_url))
            })
        SettingItem(
            name = stringResource(R.string.setting_page_more_acknowledgments),
            description = stringResource(R.string.setting_page_more_acknowledgments_descript),
            icon = painterResource(id = R.drawable.thank_icon),
            onClick = {
                showAcknowledgments(true)
            })
    }
}


@Composable
fun SettingItem(
    name: String, description: String, icon: Painter? = null, onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(32.dp),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = name, style = MaterialTheme.typography.titleSmall // 更大的字体
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description, style = MaterialTheme.typography.bodySmall // 较小的字体
                )
            }
        }
    }
}

@Composable
fun SettingTitle(
    title: String, icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(60.dp),
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title, style = MaterialTheme.typography.headlineSmall,
            // modifier = Modifier.align(Alignment.CenterStart),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// 切换游戏版本的弹窗
@Composable
fun SwitchGameDialog(
    gameInfoList: List<GameInfoBean>,
    setGameInfo: (GameInfoBean, Boolean) -> Unit,
    showSwitchGameInfo: (Boolean) -> Unit,
    showDialog: Boolean
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchGameInfo(false) }, // 点击对话框外的区域时关闭对话框
            title = { Text(text = stringResource(R.string.switch_game_service_tiltle)) }, text = {
                val toMutableList = gameInfoList.toMutableList()
                toMutableList.removeAt(0)

                LazyColumn {
                    itemsIndexed(toMutableList) { index, gameInfo ->
                        SettingItem(
                            name = gameInfo.gameName + "(${gameInfo.serviceName})",
                            description = gameInfo.packageName,
                            //icon = painterResource(id = R.drawable.ic_launcher_foreground),
                            onClick = {
                                setGameInfo(gameInfo, false)
                                showSwitchGameInfo(false)
                            })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSwitchGameInfo(false)
                }) {
                    Text(text = stringResource(R.string.mod_page_mod_detail_dialog_close))
                }
            }
        )
    }
}

@Composable
fun DownloadGameConfigDialog(
    gameInfoList: List<DownloadGameConfigBean>,
    downloadGameConfig: (DownloadGameConfigBean) -> Unit,
    showDownloadGameConfigDialog: (Boolean) -> Unit,
    showDialog: Boolean
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadGameConfigDialog(false) }, // 点击对话框外的区域时关闭对话框
            title = { Text(text = stringResource(R.string.switch_download_game_tiltle)) }, text = {
                val toMutableList = gameInfoList.toMutableList()
                //toMutableList.removeAt(0)
                LazyColumn {
                    itemsIndexed(toMutableList) { index, gameInfo ->
                        SettingItem(
                            name = gameInfo.gameName + "(${gameInfo.serviceName})",
                            description = gameInfo.packageName,
                            //icon = painterResource(id = R.drawable.ic_launcher_foreground),
                            onClick = {
                                downloadGameConfig(gameInfo)
                                showDownloadGameConfigDialog(false)
                            })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadGameConfigDialog(false)
                }) {
                    Text(text = stringResource(R.string.mod_page_mod_detail_dialog_close))
                }
            }
        )
    }
}

@Composable
fun ThanksDialogCommon(
    title: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    thinks: List<ThanksBean>,
    showDialog: Boolean = false,
    openUrl: (Context, String) -> Unit
) {
    if (showDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = {}, // 空的 lambda 函数，表示点击对话框外的区域不会关闭对话框
            title = { Text(text = title) }, text = {
                LazyColumn {
                    itemsIndexed(thinks) { index, thank ->
                        SettingItem(
                            name = thank.name,
                            description = context.getString(
                                R.string.setting_thinks_link_desc, thank.job
                            ),
                            //icon = painterResource(id = R.drawable.ic_launcher_foreground),
                            onClick = {
                                openUrl(context, thank.link)
                            })
                    }
                }
            }, confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                }) {
                    Text(stringResource(id = R.string.dialog_button_confirm))
                }
            }, dismissButton = {
                TextButton(onClick = {
                    onCancel()
                }) {
                    Text(stringResource(id = R.string.dialog_button_request_close))
                }
            })

    }
}
