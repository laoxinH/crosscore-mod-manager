package top.laoxin.modmanager.ui.view.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.DownloadGameConfigBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ThanksBean
import top.laoxin.modmanager.ui.state.SettingUiState
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton
import top.laoxin.modmanager.ui.viewmodel.MainViewModel
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel
import top.laoxin.modmanager.ui.view.components.setting.License
import top.laoxin.modmanager.ui.view.components.common.DialogCommon
import top.laoxin.modmanager.ui.view.components.common.DialogCommonForUpdate
import top.laoxin.modmanager.ui.view.components.common.DialogType
import top.laoxin.modmanager.ui.view.components.common.PermissionHandler
import top.laoxin.modmanager.ui.view.components.common.openUrl
// Import License component
// Import other necessary components if they are not copied locally. 
// Ideally we copy small components to keep this Screen self-contained or use a shared module.
// For now, I will inline the smaller components (SettingItem, etc) or copy them here to ensure it works.

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SettingScreen(
    viewModel: SettingViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel() 
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Bottom Bar Visibility Logic
    val onHideBottomBar: (Boolean) -> Unit = { hide ->
        mainViewModel.setBottomBarVisibility(!hide)
    }

    // 累计滑动距离
    val accumulatedScroll = remember { mutableFloatStateOf(0f) }
    val scrollThreshold = 32f

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (uiState.isAboutPage) {
                    // 只处理竖直方向滑动
                    accumulatedScroll.floatValue += available.y
                    if (accumulatedScroll.floatValue <= -scrollThreshold) {
                        onHideBottomBar(true)
                        accumulatedScroll.floatValue = 0f
                    } else if (accumulatedScroll.floatValue >= scrollThreshold) {
                        onHideBottomBar(false)
                        accumulatedScroll.floatValue = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
        AnimatedContent(
            targetState = uiState.isAboutPage,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
                    .using(SizeTransform(clip = false))
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
                    // Reset bottom bar when leaving about page
                    mainViewModel.setBottomBarVisibility(true)
                    SettingContent(uiState, viewModel)
                }
            }
        }
    }

    ThanksDialogCommon(
        title = stringResource(R.string.setting_acknowledgments),
        onConfirm = { viewModel.setShowAcknowledgmentsDialog(false) },
        onCancel = { viewModel.setShowAcknowledgmentsDialog(false) },
        thinks = uiState.thanksList,
        showDialog = uiState.showAcknowledgmentsDialog
    )
    
    uiState.updateInfo?.let {
        DialogCommonForUpdate(
            title = stringResource(id = R.string.console_upgrade_title),
            content = it.changelog,
            onConfirm = { it.downloadUrl.let { url -> context.openUrl(url) } },
            onDismiss = { it.universalUrl.let { url -> context.openUrl(url) } },
            showDialog = uiState.showUpdateDialog
        )
    }

    uiState.targetGame?.let {
        DialogCommon(
            title = stringResource(id = R.string.console_game_tips_title),
            type = DialogType.WARNING,
            content = it.tips,
            onConfirm = {
                viewModel.onSwitchGame(it)
                viewModel.setShowGameTipsDialog(false)
            },
            onCancel = { viewModel.setShowGameTipsDialog(false) },
            showDialog = uiState.showGameTipsDialog
        )
    }

    uiState.infoBean?.let {
        DialogCommon(
            title = stringResource(id = R.string.console_info_title),
            content = it.msg,
            onConfirm = { viewModel.setShowNotificationDialog(false) },
            onCancel = { viewModel.setShowNotificationDialog(false) },
            showDialog = uiState.showNotificationDialog
        )
    }

    SwitchGameDialog(
        gameInfoList = uiState.gameInfoList,
        setGameInfo = {viewModel.setGameInfo(it)},
        showSwitchGameInfo = viewModel::setShowSwitchGameDialog,
        showDialog = uiState.showSwitchGameDialog,
        getAppIcon = viewModel::getAppIcon
    )
    DownloadGameConfigDialog(
        gameInfoList = uiState.downloadGameConfigList,
        downloadGameConfig = viewModel::installGameConfig,
        showDownloadGameConfigDialog = viewModel::setShowDownloadGameConfigDialog,
        showDialog = uiState.showDownloadGameConfigDialog,
        isDownloading = uiState.isDownloading,
        getAppIcon = viewModel::getAppIcon
    )

    // 权限处理器
    PermissionHandler(
        permissionStateFlow = viewModel.permissionState,
        onPermissionGranted = viewModel::onPermissionGranted,
        onPermissionDenied = viewModel::onPermissionDenied,
        onRequestShizuku = viewModel::requestShizukuPermission,
        isShizukuAvailable = viewModel.isShizukuAvailable()
    )
}

@Composable
fun SettingContent(uiState: SettingUiState, settingViewModel: SettingViewModel) {
    val context = LocalContext.current
    
    DialogCommon(
        type = DialogType.WARNING,
        title = stringResource(R.string.setting_del_backups_dialog_title),
        content = stringResource(R.string.setting_del_cache_dialog_content_txt),
        onConfirm = { settingViewModel.clearCache() },
        onCancel = { settingViewModel.setShowDeleteCacheDialog(false) },
        showDialog = uiState.showDeleteCacheDialog,
    )
    Column(
        modifier =
            Modifier
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
            onClick = { settingViewModel.setAboutPage(!uiState.isAboutPage) }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_app_clean_cache),
            description = stringResource(R.string.setting_page_app_clean_cache_descript),
            onClick = { settingViewModel.setShowDeleteCacheDialog(true) }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_app_download_game_config),
            description =
                stringResource(R.string.setting_page_app_download_game_config_descript),
            onClick = { settingViewModel.getDownloadGameConfig() }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_app_flash_game_config),
            description = stringResource(R.string.setting_page_app_flash_game_config_descript),
            onClick = { settingViewModel.reloadGameConfig() }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_app_swtch_permission_shizuku),
            description =
                stringResource(R.string.setting_page_app_swtch_permission_shizuku_desc),
            onClick = { settingViewModel.requestShizukuPermission() }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_app_swtch_game),
            description = stringResource(R.string.setting_page_app_swtch_game_descript),
            onClick = { settingViewModel.setShowSwitchGameDialog(true) }
        )

        SettingTitle(
            title = stringResource(R.string.setting_page_about_title),
            icon = Icons.Default.Info
        )
        SettingItem(
            name = stringResource(R.string.setting_page_about_author),
            description = stringResource(R.string.setting_page_about_github),
            icon = painterResource(id = R.drawable.github_icon),
            onClick = {
                context.openUrl(context.getString(R.string.github_url_releases_latest))
            }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_about_pay),
            description = stringResource(R.string.setting_page_about_pay_descript),
            icon = painterResource(id = R.drawable.alipay_icon),
            onClick = { context.openUrl(context.getString(R.string.alipay_url)) }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_about_update),
            description =
                stringResource(
                    R.string.setting_page_about_update_descript,
                    uiState.versionName
                ),
            icon = painterResource(id = R.drawable.update_icon),
            onClick = { settingViewModel.checkUpdate(false) }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_about_info),
            description = stringResource(R.string.setting_page_about_info_descript),
            icon = painterResource(id = R.drawable.notification_icon),
            onClick = { settingViewModel.getNewInfo(false) }
        )

        SettingTitle(
            title = stringResource(R.string.setting_page_app_other),
            icon = Icons.Default.MoreVert
        )
        SettingItem(
            name = stringResource(R.string.setting_page_more_shizuku),
            description = stringResource(R.string.setting_page_more_shizuku_descript),
            icon = painterResource(id = R.drawable.shizuku_icon),
            onClick = { context.openUrl(context.getString(R.string.shzuiku_url)) }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_more_reference),
            description = stringResource(R.string.setting_page_more_reference_descript),
            icon = painterResource(id = R.drawable.book_icon),
            onClick = { context.openUrl(context.getString(R.string.reference_url)) }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_more_qq),
            description = stringResource(R.string.setting_page_more_qq_descript),
            icon = painterResource(id = R.drawable.tg_icon),
            onClick = { context.openUrl("https://t.me/PSSO_Mod") }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_more_community),
            description = stringResource(R.string.setting_page_more_community_descript),
            icon = painterResource(id = R.drawable.community_icon),
            onClick = { context.openUrl("https://www.modwu.com/forums") }
        )
        SettingItem(
            name = stringResource(R.string.setting_page_more_acknowledgments),
            description = stringResource(R.string.setting_page_more_acknowledgments_descript),
            icon = painterResource(id = R.drawable.thank_icon),
            onClick = { settingViewModel.getThanks() }
        )
    }
}

// ----------------------------------------------------------------------
// Reused Components (Copied from Setting.kt)
// ----------------------------------------------------------------------

@Composable
fun SettingItem(
    name: String,
    description: String,
    icon: Painter? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = name,
                    style = MaterialTheme.typography.titleSmall 
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall 
                )
            }
        }
    }
}

@Composable
fun SettingTitle(title: String, icon: ImageVector) {
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
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// 切换游戏版本的弹窗
@Composable
fun SwitchGameDialog(
    gameInfoList: List<GameInfoBean>,
    setGameInfo: (GameInfoBean) -> Unit,
    showSwitchGameInfo: (Boolean) -> Unit,
    showDialog: Boolean,
    getAppIcon: (String) -> androidx.compose.ui.graphics.ImageBitmap
) {
    if (showDialog) {
        AlertDialog(
            modifier = Modifier.heightIn(max = 400.dp),
            onDismissRequest = { showSwitchGameInfo(false) },
            title = { Text(text = stringResource(R.string.switch_game_service_tiltle)) },
            shape = MaterialTheme.shapes.extraLarge,
            text = {
                val toMutableList = gameInfoList.toMutableList()
                if (toMutableList.isNotEmpty()) toMutableList.removeAt(0)

                LazyColumn {
                    itemsIndexed(toMutableList) { _, gameInfo ->
                        GameInfoItem(
                            gameInfo = gameInfo,
                            getAppIcon = getAppIcon,
                            onClick = {
                                setGameInfo(gameInfo)
                                showSwitchGameInfo(false)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                ExpressiveTextButton(onClick = { showSwitchGameInfo(false) }) {
                    Text(text = stringResource(R.string.mod_page_mod_detail_dialog_close))
                }
            }
        )
    }
}

/** 游戏信息列表项 */
@Composable
private fun GameInfoItem(
    gameInfo: GameInfoBean,
    getAppIcon: (String) -> androidx.compose.ui.graphics.ImageBitmap,
    onClick: () -> Unit
) {
    val icon = remember(gameInfo.packageName) { getAppIcon(gameInfo.packageName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 游戏图标
            Box(modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)) {
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = gameInfo.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            // 游戏信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameInfo.gameName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = gameInfo.serviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = gameInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun DownloadGameConfigDialog(
    gameInfoList: List<DownloadGameConfigBean>,
    downloadGameConfig: (DownloadGameConfigBean) -> Unit,
    showDownloadGameConfigDialog: (Boolean) -> Unit,
    showDialog: Boolean,
    isDownloading: Boolean = false,
    getAppIcon: (String) -> androidx.compose.ui.graphics.ImageBitmap
) {
    if (showDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxHeight(0.6f),
            onDismissRequest = { if (!isDownloading) showDownloadGameConfigDialog(false) },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.switch_download_game_tiltle),
                        modifier = Modifier.weight(1f)
                    )
                    if (isDownloading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            text = {
                LazyColumn {
                    itemsIndexed(gameInfoList) { _, gameInfo ->
                        GameConfigItem(
                            config = gameInfo,
                            getAppIcon = getAppIcon,
                            enabled = !isDownloading,
                            onClick = { downloadGameConfig(gameInfo) }
                        )
                    }
                }
            },
            confirmButton = {
                ExpressiveTextButton(
                    onClick = { showDownloadGameConfigDialog(false) },
                    enabled = !isDownloading
                ) { Text(text = stringResource(R.string.mod_page_mod_detail_dialog_close)) }
            }
        )
    }
}

/** 游戏配置列表项 */
@Composable
private fun GameConfigItem(
    config: DownloadGameConfigBean,
    getAppIcon: (String) -> androidx.compose.ui.graphics.ImageBitmap,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val icon = remember(config.packageName) { getAppIcon(config.packageName) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable(enabled = enabled) {
                    onClick()
                },
        shape = MaterialTheme.shapes.large,
        colors =
            if (enabled) CardDefaults.cardColors()
            else
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
                )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 游戏图标
            Box(modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)) {
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = config.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            // 游戏信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.gameName,
                    style = MaterialTheme.typography.titleSmall,
                    color =
                        if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = config.serviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = config.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThanksDialogCommon(
    title: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    thinks: List<ThanksBean>,
    showDialog: Boolean = false,
) {
    if (showDialog) {
        val context = LocalContext.current
        AlertDialog(
            modifier = Modifier.fillMaxHeight(0.6f),
            onDismissRequest = {},
            title = { Text(text = title) },
            text = {
                LazyColumn {
                    itemsIndexed(thinks) { index, thank ->
                        SettingItem(
                            name = thank.name,
                            description =
                                stringResource(
                                    R.string.setting_thinks_link_desc,
                                    thank.job
                                ),
                            onClick = { context.openUrl(thank.link) }
                        )
                    }
                }
            },
            confirmButton = {
                ExpressiveTextButton(onClick = { onConfirm() }) {
                    Text(stringResource(id = R.string.dialog_button_confirm))
                }
            },
            dismissButton = {
                ExpressiveTextButton(onClick = { onCancel() }) {
                    Text(stringResource(id = R.string.dialog_button_request_close))
                }
            }
        )
    }
}
