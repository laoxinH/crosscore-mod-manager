package top.laoxin.modmanager.ui.view

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ImagesearchRoller
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.view.modView.ModPage
import top.laoxin.modmanager.ui.view.modView.ModTopBar
import top.laoxin.modmanager.ui.view.settingView.SettingPage
import top.laoxin.modmanager.ui.view.settingView.SettingTopBar
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel
import kotlin.math.abs

// 通知相关常量
private const val TIPS_NOTIFICATION_ID = 1919810
private const val TIPS_NOTIFICATION_CHANNEL_ID = "tips_channel"
private const val TIPS_NOTIFICATION_ACTION = "top.laoxin.modmanager.ACTION_SHOW_TIPS"

// 导航栏索引
enum class NavigationIndex(
    @param:StringRes val title: Int,
    val icon: ImageVector,
) {
    CONSOLE(R.string.console, Icons.Filled.Dashboard),
    MOD(R.string.mod, Icons.Filled.ImagesearchRoller),
    SETTINGS(R.string.settings, Icons.Filled.Settings)
}

data class PageNavigationState(
    val pageList: List<NavigationIndex>,
    val exitTime: Long,
    val currentPage: Int,
    val shouldScroll: Boolean,
    val pagerState: PagerState,
    val onExitTimeChange: (Long) -> Unit,
    val onCurrentPageChange: (Int) -> Unit,
    val onShouldScrollChange: (Boolean) -> Unit
)

@Composable
fun ModManagerApp() {
    val modViewModel: ModViewModel = viewModel()
    val consoleViewModel: ConsoleViewModel = viewModel()
    val settingViewModel: SettingViewModel = viewModel()
    val configuration = LocalConfiguration.current

    // 提取页面状态管理到单独的组合函数
    val pageNavigationState = rememberPageNavigationState()
    val settingUiState = settingViewModel.uiState.collectAsState().value

    // 管理底部栏显示状态
    var hideBottomBar by remember { mutableStateOf(false) }

    LaunchedEffect(settingUiState.showAbout) {
        if (!settingUiState.showAbout) {
            hideBottomBar = false
        }
    }

    // 页面内容
    PageContent(
        modViewModel = modViewModel,
        consoleViewModel = consoleViewModel,
        settingViewModel = settingViewModel,
        pageNavigationState = pageNavigationState,
        configuration = configuration,
        hideBottomBar = hideBottomBar,
        onHideBottomBarChange = { hideBottomBar = it }
    )
}

@Composable
fun rememberPageNavigationState(): PageNavigationState {
    val pageList = NavigationIndex.entries
    var exitTime by rememberSaveable { mutableLongStateOf(0L) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var shouldScroll by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { pageList.size }
    )
    return PageNavigationState(
        pageList = pageList,
        exitTime = exitTime,
        currentPage = currentPage,
        shouldScroll = shouldScroll,
        pagerState = pagerState,
        onExitTimeChange = { exitTime = it },
        onCurrentPageChange = { currentPage = it },
        onShouldScrollChange = { shouldScroll = it }
    )
}

@Composable
fun PageContent(
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel,
    settingViewModel: SettingViewModel,
    pageNavigationState: PageNavigationState,
    configuration: Configuration,
    hideBottomBar: Boolean,
    onHideBottomBarChange: (Boolean) -> Unit
) {
    // 处理通知相关逻辑
    HandleTipsNotification(modViewModel)

    Row {
        // 横屏时显示侧边导航
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NavigationRail(
                currentPage = pageNavigationState.currentPage,
                onPageSelected = { page ->
                    pageNavigationState.onCurrentPageChange(page)
                    pageNavigationState.onShouldScrollChange(true)
                },
                modViewModel = modViewModel,
                consoleViewModel = consoleViewModel
            )
        }

        MainScaffold(
            modViewModel = modViewModel,
            consoleViewModel = consoleViewModel,
            settingViewModel = settingViewModel,
            pageNavigationState = pageNavigationState,
            configuration = configuration,
            hideBottomBar = hideBottomBar,
            onHideBottomBarChange = onHideBottomBarChange
        )
    }
}

@Composable
private fun HandleTipsNotification(modViewModel: ModViewModel) {
    val context = LocalContext.current
    val modViewUiState = modViewModel.uiState.collectAsState().value

    LaunchedEffect(
        modViewUiState.tipsText,
        modViewUiState.unzipProgress,
        modViewUiState.multitaskingProgress,
        modViewUiState.showTips,
        modViewUiState.isSnackbarHidden
    ) {
        val tipsStart = if (modViewUiState.unzipProgress.isNotEmpty()) {
            "${modViewUiState.tipsText} : ${modViewUiState.unzipProgress}"
        } else {
            modViewUiState.tipsText
        }

        val tipsEnd = if (modViewUiState.multitaskingProgress.isNotEmpty()) {
            context.getString(R.string.mod_top_bar_tips, modViewUiState.multitaskingProgress)
        } else {
            ""
        }

        val tipsContent = "$tipsStart $tipsEnd"
        val hasActiveOperation =
            modViewUiState.unzipProgress.isNotEmpty() || modViewUiState.multitaskingProgress.isNotEmpty()

        if (hasActiveOperation) {
            sendTipsNotification(context, tipsContent)
        } else {
            cancelTipsNotification(context)
            if (modViewUiState.showTips) {
                modViewModel.setShowTips(false)
                modViewModel.setSnackbarHidden(false)
            }
        }
    }
}

@Composable
private fun MainScaffold(
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel,
    settingViewModel: SettingViewModel,
    pageNavigationState: PageNavigationState,
    configuration: Configuration,
    hideBottomBar: Boolean,
    onHideBottomBarChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopBarContent(
                modViewModel = modViewModel,
                consoleViewModel = consoleViewModel,
                settingViewModel = settingViewModel,
                currentPage = pageNavigationState.currentPage,
                configuration = configuration.orientation
            )
        },
        bottomBar = {
            BottomBarContent(
                configuration = configuration,
                hideBottomBar = hideBottomBar,
                currentPage = pageNavigationState.currentPage,
                modViewModel = modViewModel,
                onPageSelected = { page ->
                    pageNavigationState.onCurrentPageChange(page)
                    pageNavigationState.onShouldScrollChange(true)
                }
            )
        }
    ) { innerPadding ->
        ScaffoldContent(
            modViewModel = modViewModel,
            consoleViewModel = consoleViewModel,
            settingViewModel = settingViewModel,
            pageNavigationState = pageNavigationState,
            innerPadding = innerPadding,
            onHideBottomBarChange = onHideBottomBarChange
        )
    }
}

@Composable
private fun TopBarContent(
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel,
    settingViewModel: SettingViewModel,
    currentPage: Int,
    configuration: Int
) {
    when (currentPage) {
        NavigationIndex.CONSOLE.ordinal -> ConsoleTopBar(
            consoleViewModel,
            configuration = configuration
        )

        NavigationIndex.MOD.ordinal -> ModTopBar(
            modViewModel,
            configuration = configuration
        )

        NavigationIndex.SETTINGS.ordinal -> SettingTopBar(
            settingViewModel,
            configuration = configuration
        )
    }
}

@Composable
private fun BottomBarContent(
    configuration: Configuration,
    hideBottomBar: Boolean,
    currentPage: Int,
    modViewModel: ModViewModel,
    onPageSelected: (Int) -> Unit
) {
    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .height(
                    if (hideBottomBar) 0.dp
                    else 80.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
        ) {
            if (!hideBottomBar) {
                NavigationBar(
                    currentPage = currentPage,
                    modViewModel = modViewModel,
                    onPageSelected = onPageSelected
                )
            }
        }
    }
}

@Composable
private fun ScaffoldContent(
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel,
    settingViewModel: SettingViewModel,
    pageNavigationState: PageNavigationState,
    innerPadding: PaddingValues,
    onHideBottomBarChange: (Boolean) -> Unit
) {
    val modViewUiState = modViewModel.uiState.collectAsState().value

    // 处理返回键逻辑
    HandleBackNavigation(
        pageNavigationState = pageNavigationState,
        modViewModel = modViewModel,
        modViewUiState = modViewUiState
    )

    // 处理页面滚动
    HandlePageScrolling(pageNavigationState)

    // 注册广播接收器
    HandleBroadcastReceiver(modViewModel)

    Box(modifier = Modifier.fillMaxSize()) {
        // 显示提示信息
        if (modViewUiState.showTips && !modViewUiState.isSnackbarHidden) {
            ProcessTips(
                text = modViewUiState.tipsText,
                setSnackbarHidden = { hidden ->
                    modViewModel.setSnackbarHidden(hidden)
                },
                uiState = modViewUiState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = innerPadding.calculateTopPadding())
                    .zIndex(10f)
            )
        }

        AppContent(
            pagerState = pageNavigationState.pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .zIndex(0f),
            consoleViewModel = consoleViewModel,
            modViewModel = modViewModel,
            settingViewModel = settingViewModel,
            onHideBottomBar = onHideBottomBarChange
        )
    }
}

@Composable
private fun HandleBackNavigation(
    pageNavigationState: PageNavigationState,
    modViewModel: ModViewModel,
    modViewUiState: ModUiState
) {
    val context = LocalContext.current
    val exitToast: Toast = remember {
        Toast.makeText(
            context,
            context.getText(R.string.toast_quit_app),
            Toast.LENGTH_SHORT
        )
    }
    val activity = context as? Activity

    BackHandler(enabled = pageNavigationState.currentPage == NavigationIndex.CONSOLE.ordinal) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - pageNavigationState.exitTime > 2000) {
            exitToast.show()
            pageNavigationState.onExitTimeChange(currentTime)
        } else {
            exitToast.cancel()
            activity?.finish()
        }
    }

    if (pageNavigationState.currentPage == NavigationIndex.MOD.ordinal && modViewUiState.searchBoxVisible) {
        BackHandler {
            modViewModel.setSearchBoxVisible(false)
        }
    } else if (pageNavigationState.currentPage != NavigationIndex.CONSOLE.ordinal) {
        BackHandler {
            pageNavigationState.onCurrentPageChange(NavigationIndex.CONSOLE.ordinal)
            pageNavigationState.onShouldScrollChange(true)
        }
    }
}

@Composable
private fun HandlePageScrolling(pageNavigationState: PageNavigationState) {
    LaunchedEffect(pageNavigationState.currentPage, pageNavigationState.shouldScroll) {
        if (pageNavigationState.shouldScroll) {
            delay(10)
            val targetPage = pageNavigationState.currentPage
            val currentPagerPage = pageNavigationState.pagerState.currentPage
            if (targetPage != currentPagerPage) {
                pageNavigationState.pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = tween(
                        durationMillis = abs(currentPagerPage - targetPage) * 100 + 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            pageNavigationState.onShouldScrollChange(false)
        }
    }

    val currentPagerPage by remember {
        derivedStateOf { pageNavigationState.pagerState.currentPage }
    }

    LaunchedEffect(currentPagerPage) {
        if (!pageNavigationState.shouldScroll && pageNavigationState.currentPage != currentPagerPage) {
            pageNavigationState.onCurrentPageChange(currentPagerPage)
        }
    }
}

@Composable
private fun HandleBroadcastReceiver(modViewModel: ModViewModel) {
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TIPS_NOTIFICATION_ACTION) {
                    val currentState = modViewModel.uiState.value
                    val hasActiveOperation = currentState.unzipProgress.isNotEmpty() ||
                            currentState.multitaskingProgress.isNotEmpty()

                    if (hasActiveOperation) {
                        modViewModel.setShowTips(true)
                        modViewModel.setSnackbarHidden(false)
                    }
                }
            }
        }
        val filter = IntentFilter(TIPS_NOTIFICATION_ACTION)

        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun AppContent(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    consoleViewModel: ConsoleViewModel,
    modViewModel: ModViewModel,
    settingViewModel: SettingViewModel,
    onHideBottomBar: (Boolean) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        key = { page -> "page_$page" }
    ) { page ->
        when (page) {
            NavigationIndex.CONSOLE.ordinal -> ConsolePage(consoleViewModel)
            NavigationIndex.MOD.ordinal -> ModPage(modViewModel)
            NavigationIndex.SETTINGS.ordinal -> SettingPage(settingViewModel, onHideBottomBar)
        }
    }
}

//侧边导航
@Composable
fun NavigationRail(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val packageName = consoleViewModel.uiState.collectAsState().value.gameInfo.packageName
    val gameIcon = remember(packageName) {
        getGameIcon(packageName)
    }

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(90.dp)
            .padding(0.dp)
    ) {
        val currentPageName = stringResource(id = NavigationIndex.entries[currentPage].title)
        Text(
            text = currentPageName,
            modifier = Modifier.padding(16.dp)
        )
        Column {
            NavigationIndex.entries.forEachIndexed { index, navigationItem ->
                val isSelected = currentPage == index

                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (isSelected && (currentTime - lastClickTime) < 300) {
                            refreshCurrentPage(currentPage, modViewModel)
                        } else {
                            modViewModel.exitSelect()
                            if (!isSelected) {
                                onPageSelected(index)
                            }
                        }
                        lastClickTime = currentTime
                    },
                    icon = {
                        Icon(imageVector = navigationItem.icon, contentDescription = null)
                    },
                    label = {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(text = stringResource(id = navigationItem.title))
                        }
                    },
                    alwaysShowLabel = false
                )
                Spacer(modifier = Modifier.padding(10.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            gameIcon?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}

// 底部导航
@Composable
fun NavigationBar(
    currentPage: Int,
    modViewModel: ModViewModel,
    onPageSelected: (Int) -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    NavigationBar {
        NavigationIndex.entries.forEachIndexed { index, navigationItem ->
            val isSelected = currentPage == index

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (isSelected && (currentTime - lastClickTime) < 300) {
                        refreshCurrentPage(currentPage, modViewModel)
                    } else {
                        modViewModel.exitSelect()
                        if (!isSelected) {
                            onPageSelected(index)
                        }
                    }
                    lastClickTime = currentTime
                },
                icon = {
                    Icon(imageVector = navigationItem.icon, contentDescription = null)
                },
                label = {
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(text = stringResource(id = navigationItem.title))
                    }
                },
                alwaysShowLabel = false
            )
        }
    }
}

private fun refreshCurrentPage(currentPage: Int, modViewModel: ModViewModel) {
    when (currentPage) {
        NavigationIndex.CONSOLE.ordinal -> {}

        NavigationIndex.MOD.ordinal -> {
            modViewModel.flashMods(true, false)
        }

        NavigationIndex.SETTINGS.ordinal -> {}
    }
}

// 获取应用图标
fun getGameIcon(packageName: String): ImageBitmap? {
    var packageName = packageName
    if (packageName.isEmpty() || packageName == "null") {
        packageName = App.get().packageName
    }
    try {
        val packageInfo = App.get().packageManager.getPackageInfo(packageName, 0)
        var drawable = packageInfo.applicationInfo?.loadIcon(App.get().packageManager)
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is AdaptiveIconDrawable -> {
                createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).also { bitmap ->
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }

            else -> {
                val context = App.get()
                drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
                drawable.toBitmap()
            }
        }
        return bitmap.asImageBitmap()
    } catch (_: PackageManager.NameNotFoundException) {
        val context = App.get()
        val drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
        val bitmap = drawable.toBitmap()
        return bitmap.asImageBitmap()
    }
}

@Composable
fun ProcessTips(
    text: String,
    setSnackbarHidden: (Boolean) -> Unit,
    uiState: ModUiState,
    modifier: Modifier
) {
    val tipsStart =
        if (uiState.unzipProgress.isNotEmpty()) {
            "$text : ${uiState.unzipProgress}"
        } else {
            text
        }

    val tipsEnd =
        if (uiState.multitaskingProgress.isNotEmpty()) {
            stringResource(R.string.mod_top_bar_tips, uiState.multitaskingProgress)
        } else {
            ""
        }

    val tipsContent = "$tipsStart $tipsEnd"

    Box(modifier.fillMaxWidth(0.7f)) {
        Snackbar(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            action = {
                Button(
                    onClick = {
                        // 用户点击隐藏按钮，只隐藏Snackbar，不影响通知
                        setSnackbarHidden(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(4.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tips_btn_close),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        ) {
            Text(
                text = tipsContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun sendTipsNotification(context: Context, content: String) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        TIPS_NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.console_info_title),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    // 设置通知不发出声音和震动，避免频繁更新时的干扰
    channel.setSound(null, null)
    channel.enableVibration(false)
    notificationManager.createNotificationChannel(channel)

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val contentPendingIntent = PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val actionIntent = Intent(TIPS_NOTIFICATION_ACTION).apply {
        putExtra("timestamp", System.currentTimeMillis())
    }
    val actionPendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        actionIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, TIPS_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(context.getString(R.string.console_info_title))
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setAutoCancel(false)
        .setOngoing(true)
        .setContentIntent(contentPendingIntent)
        .addAction(
            R.drawable.app_icon,
            context.getString(R.string.show_snackbar),
            actionPendingIntent
        )
        .setOnlyAlertOnce(true)
        .build()

    notificationManager.notify(TIPS_NOTIFICATION_ID, notification)
}

// 取消通知
private fun cancelTipsNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(TIPS_NOTIFICATION_ID)
}
