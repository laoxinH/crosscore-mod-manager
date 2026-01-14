package top.laoxin.modmanager.ui.view

// import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
// import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.math.abs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.state.SnackbarManager
import top.laoxin.modmanager.ui.theme.MotionDuration
import top.laoxin.modmanager.ui.theme.MotionEasing
import top.laoxin.modmanager.ui.view.common.GlobalSnackbarHost
import top.laoxin.modmanager.ui.view.modView.ModPage
import top.laoxin.modmanager.ui.view.modView.ModTopBar
import top.laoxin.modmanager.ui.view.settingView.SettingPage
import top.laoxin.modmanager.ui.view.settingView.SettingTopBar
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel
import top.laoxin.modmanager.ui.viewmodel.MainViewModel
import top.laoxin.modmanager.ui.viewmodel.NavigationEvent
import top.laoxin.modmanager.ui.viewmodel.ModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel

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
    val mainViewModel: MainViewModel = hiltViewModel()
    val snackbarManager = mainViewModel.snackbarManager

    val pageNavigationState = rememberPageNavigationState()
    val configuration = LocalConfiguration.current

    var hideBottomBar by remember { mutableStateOf(false) }

    // 订阅导航事件
    LaunchedEffect(Unit) {
        mainViewModel.navigationEvent.collect { event ->
            val targetPage = when (event) {
                is NavigationEvent.NavigateToSettings -> NavigationIndex.SETTINGS.ordinal
                is NavigationEvent.NavigateToConsole -> NavigationIndex.CONSOLE.ordinal
                is NavigationEvent.NavigateToMod -> NavigationIndex.MOD.ordinal
            }
            pageNavigationState.onCurrentPageChange(targetPage)
            pageNavigationState.onShouldScrollChange(true)
        }
    }

    // 页面内容
    PageContent(
            pageNavigationState = pageNavigationState,
            configuration = configuration,
            hideBottomBar = hideBottomBar,
            onHideBottomBarChange = { hideBottomBar = it },
            snackbarManager = snackbarManager
    )
}

@Composable
fun rememberPageNavigationState(): PageNavigationState {
    val pageList = NavigationIndex.entries
    var exitTime by rememberSaveable { mutableLongStateOf(0L) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var shouldScroll by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = currentPage, pageCount = { pageList.size })
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
        pageNavigationState: PageNavigationState,
        configuration: Configuration,
        hideBottomBar: Boolean,
        onHideBottomBarChange: (Boolean) -> Unit,
        snackbarManager: SnackbarManager
) {
    // 在页面级别初始化ViewModel
    val settingViewModel: SettingViewModel = hiltViewModel()
    val settingUiState = settingViewModel.uiState.collectAsState().value

    LaunchedEffect(settingUiState.isAboutPage) {
        if (!settingUiState.isAboutPage) {
            onHideBottomBarChange(false)
        }
    }

    Row {
        // 横屏时显示侧边导航
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NavigationRail(
                    currentPage = pageNavigationState.currentPage,
                    onPageSelected = { page ->
                        pageNavigationState.onCurrentPageChange(page)
                        pageNavigationState.onShouldScrollChange(true)
                    }
            )
        }

        MainScaffold(
                pageNavigationState = pageNavigationState,
                configuration = configuration,
                hideBottomBar = hideBottomBar,
                onHideBottomBarChange = onHideBottomBarChange,
                snackbarManager = snackbarManager
        )
    }
}

@Composable
private fun MainScaffold(
        pageNavigationState: PageNavigationState,
        configuration: Configuration,
        hideBottomBar: Boolean,
        onHideBottomBarChange: (Boolean) -> Unit,
        snackbarManager: SnackbarManager
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
            topBar = {
                TopBarContent(
                        currentPage = pageNavigationState.currentPage,
                        configuration = configuration.orientation
                )
            },
            bottomBar = {
                BottomBarContent(
                        configuration = configuration,
                        hideBottomBar = hideBottomBar,
                        currentPage = pageNavigationState.currentPage,
                        onPageSelected = { page ->
                            pageNavigationState.onCurrentPageChange(page)
                            pageNavigationState.onShouldScrollChange(true)
                        }
                )
            },
            snackbarHost = {
                GlobalSnackbarHost(
                        snackbarManager = snackbarManager,
                        snackbarHostState = snackbarHostState
                )
            }
    ) { innerPadding ->
        ScaffoldContent(
                pageNavigationState = pageNavigationState,
                innerPadding = innerPadding,
                onHideBottomBarChange = onHideBottomBarChange
        )
    }
}

@Composable
private fun TopBarContent(currentPage: Int, configuration: Int) {
    when (currentPage) {
        NavigationIndex.CONSOLE.ordinal -> {
            val consoleViewModel: ConsoleViewModel = hiltViewModel()
            ConsoleTopBar(viewModel = consoleViewModel, configuration = configuration)
        }
        NavigationIndex.MOD.ordinal -> {
            // 使用新的拆分ViewModel
            val modScanViewModel: ModScanViewModel = hiltViewModel()
            val modOperationViewModel: ModOperationViewModel = hiltViewModel()
            val modListViewModel: ModListViewModel = hiltViewModel()
            val modDetailViewModel: ModDetailViewModel = hiltViewModel()
            val modSearchViewModel: ModSearchViewModel = hiltViewModel()
            val modBrowserViewModel: ModBrowserViewModel = hiltViewModel()

            ModTopBar(
                   // modDetailViewModel= modDetailViewModel,
                    modListViewModel = modListViewModel,
                    modOperationViewModel = modOperationViewModel,
                    modBrowserViewModel = modBrowserViewModel,
                    modSearchViewModel = modSearchViewModel,
                    modScanViewModel = modScanViewModel,
                    configuration = configuration
            )
        }
        NavigationIndex.SETTINGS.ordinal -> {
            val settingViewModel: SettingViewModel = hiltViewModel()
            SettingTopBar(settingViewModel, configuration = configuration)
        }
    }
}

@Composable
private fun BottomBarContent(
        configuration: Configuration,
        hideBottomBar: Boolean,
        currentPage: Int,
        onPageSelected: (Int) -> Unit
) {
    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .animateContentSize()
                                .height(
                                        if (hideBottomBar) 0.dp
                                        else
                                                80.dp +
                                                        WindowInsets.navigationBars
                                                                .asPaddingValues()
                                                                .calculateBottomPadding()
                                )
        ) {
            if (!hideBottomBar) {
                NavigationBar(currentPage = currentPage, onPageSelected = onPageSelected)
            }
        }
    }
}

@Composable
private fun ScaffoldContent(
        pageNavigationState: PageNavigationState,
        innerPadding: PaddingValues,
        onHideBottomBarChange: (Boolean) -> Unit
) {
    // 处理返回键逻辑
    HandleBackNavigation(pageNavigationState = pageNavigationState)

    // 处理页面滚动
    HandlePageScrolling(pageNavigationState)

    Box(modifier = Modifier.fillMaxSize()) {
        // 显示提示信息 - 现在由各个页面自己处理
        AppContent(
                pagerState = pageNavigationState.pagerState,
                modifier = Modifier.padding(innerPadding).zIndex(0f),
                onHideBottomBar = onHideBottomBarChange
        )
    }
}

@Composable
private fun HandleBackNavigation(pageNavigationState: PageNavigationState) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 获取搜索状态
    val modSearchViewModel: ModSearchViewModel = hiltViewModel()
    val modScanUiState by modSearchViewModel.uiState.collectAsState()
    val searchBoxVisible = modScanUiState.searchBoxVisible

    val isOnModPageWithSearchVisible =
            pageNavigationState.currentPage == NavigationIndex.MOD.ordinal && searchBoxVisible
    val isNotOnConsolePage = pageNavigationState.currentPage != NavigationIndex.CONSOLE.ordinal

    // MOD页面搜索框可见时，隐藏搜索框
    BackHandler(enabled = isOnModPageWithSearchVisible) {
        modSearchViewModel.setSearchBoxVisible(false)
    }

    // 非CONSOLE页面时，返回CONSOLE（排除搜索框可见情况）
    BackHandler(enabled = isNotOnConsolePage && !isOnModPageWithSearchVisible) {
        pageNavigationState.onCurrentPageChange(NavigationIndex.CONSOLE.ordinal)
        pageNavigationState.onShouldScrollChange(true)
    }

    // CONSOLE页面双击退出
    BackHandler(enabled = pageNavigationState.currentPage == NavigationIndex.CONSOLE.ordinal) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - pageNavigationState.exitTime > 2000) {
            Toast.makeText(context, context.getText(R.string.toast_quit_app), Toast.LENGTH_SHORT)
                    .show()
            pageNavigationState.onExitTimeChange(currentTime)
        } else {
            activity?.finish()
        }
    }
}

@Composable
private fun HandlePageScrolling(pageNavigationState: PageNavigationState) {
    val pagerState = pageNavigationState.pagerState

    // 点击导航栏时触发页面切换
    LaunchedEffect(pageNavigationState.currentPage, pageNavigationState.shouldScroll) {
        if (pageNavigationState.shouldScroll) {
            val targetPage = pageNavigationState.currentPage
            val currentPagerPage = pagerState.currentPage
            if (targetPage != currentPagerPage) {
                pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec =
                                tween(
                                        durationMillis =
                                                abs(currentPagerPage - targetPage) * 100 +
                                                        MotionDuration.Medium,
                                        easing = MotionEasing.Emphasized
                                )
                )
            }
            pageNavigationState.onShouldScrollChange(false)
        }
    }

    // 使用 settledPage 而不是 currentPage，避免动画过程中的中间状态
    val settledPage by remember { derivedStateOf { pagerState.settledPage } }

    // 只在页面完全停止后才同步状态
    LaunchedEffect(settledPage) {
        if (!pageNavigationState.shouldScroll && pageNavigationState.currentPage != settledPage) {
            pageNavigationState.onCurrentPageChange(settledPage)
        }
    }
}

@Composable
fun AppContent(
        pagerState: PagerState,
        modifier: Modifier = Modifier,
        onHideBottomBar: (Boolean) -> Unit
) {
    HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = 0,
            key = { page -> "page_$page" }
    ) { page ->
        when (page) {
            NavigationIndex.CONSOLE.ordinal -> {
                val consoleViewModel: ConsoleViewModel = hiltViewModel()
                ConsolePage(viewModel = consoleViewModel)
            }
            NavigationIndex.MOD.ordinal -> {
                // 使用新的拆分ViewModel
                val modScanViewModel: ModScanViewModel = hiltViewModel()
                val modOperationViewModel: ModOperationViewModel = hiltViewModel()
                val modListViewModel: ModListViewModel = hiltViewModel()
                val modDetailViewModel: ModDetailViewModel = hiltViewModel()
                val modSearchViewModel: ModSearchViewModel = hiltViewModel()
                val modBrowserViewModel: ModBrowserViewModel = hiltViewModel()

                ModPage(
                        modScanViewModel = modScanViewModel,
                        modOperationViewModel = modOperationViewModel,
                        modListViewModel = modListViewModel,
                        modDetailViewModel = modDetailViewModel,
                        modSearchViewModel = modSearchViewModel,
                        modBrowserViewModel = modBrowserViewModel
                )
            }
            NavigationIndex.SETTINGS.ordinal -> {
                val settingViewModel: SettingViewModel = hiltViewModel()
                SettingPage(settingViewModel, onHideBottomBar)
            }
        }
    }
}

// 侧边导航
@Composable
fun NavigationRail(currentPage: Int, onPageSelected: (Int) -> Unit) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var isNavigating by remember { mutableStateOf(false) }

    // 获取游戏信息用于显示图标
    val consoleViewModel: ConsoleViewModel = hiltViewModel()
    val packageName = consoleViewModel.uiState.collectAsState().value.gameInfo.packageName
    val gameIcon = remember(packageName) { getGameIcon(packageName) }

    // 获取ModListViewModel用于退出多选模式
    val modListViewModel: ModListViewModel = hiltViewModel()

    NavigationRail(modifier = Modifier.fillMaxHeight().width(90.dp).padding(0.dp)) {
        val currentPageName = stringResource(id = NavigationIndex.entries[currentPage].title)
        Text(text = currentPageName, modifier = Modifier.padding(16.dp))
        Column {
            NavigationIndex.entries.forEachIndexed { index, navigationItem ->
                val isSelected = currentPage == index

                NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            val timeSinceLastClick = currentTime - lastClickTime

                            // 双击刷新当前页面
                            if (isSelected && timeSinceLastClick < 300) {
                                refreshCurrentPage(currentPage)
                                lastClickTime = currentTime
                                return@NavigationRailItem
                            }

                            // 防抖：防止快速点击和动画进行中的点击
                            if (!isNavigating && !isSelected && timeSinceLastClick > 100) {
                                isNavigating = true
                                // 退出多选模式
                                if (currentPage == NavigationIndex.MOD.ordinal) {
                                    modListViewModel.exitSelect()
                                }
                                onPageSelected(index)

                                // 动画时间后重置导航状态
                                MainScope().launch {
                                    delay(
                                            abs(currentPage - index) * 100L +
                                                    MotionDuration.Medium.toLong()
                                    )
                                    isNavigating = false
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
                            ) { Text(text = stringResource(id = navigationItem.title)) }
                        },
                        alwaysShowLabel = false
                )
                Spacer(modifier = Modifier.padding(10.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            gameIcon?.let {
                Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).padding(8.dp)
                )
            }
        }
    }
}

// 底部导航
@Composable
fun NavigationBar(currentPage: Int, onPageSelected: (Int) -> Unit) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var isNavigating by remember { mutableStateOf(false) }

    // 获取ModListViewModel用于退出多选模式
    val modListViewModel: ModListViewModel = hiltViewModel()

    NavigationBar {
        NavigationIndex.entries.forEachIndexed { index, navigationItem ->
            val isSelected = currentPage == index

            NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        val timeSinceLastClick = currentTime - lastClickTime

                        // 双击刷新当前页面
                        if (isSelected && timeSinceLastClick < 300) {
                            refreshCurrentPage(currentPage)
                            lastClickTime = currentTime
                            return@NavigationBarItem
                        }

                        // 防抖：防止快速点击和动画进行中的点击
                        if (!isNavigating && !isSelected && timeSinceLastClick > 100) {
                            isNavigating = true
                            // 退出多选模式
                            if (currentPage == NavigationIndex.MOD.ordinal) {
                                modListViewModel.exitSelect()
                            }
                            onPageSelected(index)

                            // 动画时间后重置导航状态
                            MainScope().launch {
                                delay(
                                        abs(currentPage - index) * 100L +
                                                MotionDuration.Medium.toLong()
                                )
                                isNavigating = false
                            }
                        }

                        lastClickTime = currentTime
                    },
                    icon = { Icon(imageVector = navigationItem.icon, contentDescription = null) },
                    label = {
                        AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(),
                                exit = fadeOut()
                        ) { Text(text = stringResource(id = navigationItem.title)) }
                    },
                    alwaysShowLabel = false
            )
        }
    }
}

private fun refreshCurrentPage(currentPage: Int) {
    when (currentPage) {
        NavigationIndex.CONSOLE.ordinal -> {}
        NavigationIndex.MOD.ordinal -> {
            // 通过事件总线触发刷新，而不是直接调用ViewModel
            // 这里可以发送刷新事件，由ModScanViewModel监听处理
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
        val bitmap =
                when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    is AdaptiveIconDrawable -> {
                        createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).also {
                                bitmap ->
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

private fun sendTipsNotification(context: Context, content: String) {
    val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel =
            NotificationChannel(
                    TIPS_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.console_info_title),
                    NotificationManager.IMPORTANCE_DEFAULT
            )
    // 设置通知不发出声音和震动，避免频繁更新时的干扰
    channel.setSound(null, null)
    channel.enableVibration(false)
    notificationManager.createNotificationChannel(channel)

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val contentPendingIntent =
            PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

    val actionIntent =
            Intent(TIPS_NOTIFICATION_ACTION).apply {
                putExtra("timestamp", System.currentTimeMillis())
            }
    val actionPendingIntent =
            PendingIntent.getBroadcast(
                    context,
                    1,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

    val notification =
            NotificationCompat.Builder(context, TIPS_NOTIFICATION_CHANNEL_ID)
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
