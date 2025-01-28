package top.laoxin.modmanager.ui.view

import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.runtime.*
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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.view.modView.ModPage
import top.laoxin.modmanager.ui.view.modView.ModTopBar
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel
import top.laoxin.modmanager.ui.viewmodel.VersionViewModel
import kotlin.math.abs

// 导航栏索引
enum class NavigationIndex(
    @StringRes val title: Int,
    val icon: ImageVector,
) {
    CONSOLE(R.string.console, Icons.Filled.Dashboard),
    MOD(R.string.mod, Icons.Filled.ImagesearchRoller),
    SETTINGS(R.string.settings, Icons.Filled.Settings)
}

@Composable
fun ModManagerApp() {
    val modViewModel: ModViewModel = viewModel(factory = ModViewModel.Factory)
    val versionViewModel: VersionViewModel = viewModel(factory = VersionViewModel.Factory)
    val consoleViewModel: ConsoleViewModel =
        viewModel(factory = ConsoleViewModel.Factory(versionViewModel))
    val settingViewModel: SettingViewModel =
        viewModel(factory = SettingViewModel.Factory(versionViewModel))
    val pageList = NavigationIndex.entries
    val configuration = LocalConfiguration.current

    var exitTime by remember { mutableLongStateOf(0L) }
    var currentPage by remember { mutableIntStateOf(0) }
    var shouldScroll by remember { mutableStateOf(false) }

    Row {
        // 在横向模式下显示侧边导航栏
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NavigationRail(
                currentPage = currentPage,
                onPageSelected = { page ->
                    currentPage = page
                    shouldScroll = true
                },
                modViewModel = modViewModel,
                consoleViewModel = consoleViewModel
            )
        }

        Scaffold(
            // 根据当前页面显示不同的顶部工具栏
            topBar = {
                when (currentPage) {
                    NavigationIndex.CONSOLE.ordinal -> ConsoleTopBar(
                        consoleViewModel,
                        configuration = configuration.orientation
                    )

                    NavigationIndex.MOD.ordinal -> ModTopBar(
                        modViewModel,
                        configuration = configuration.orientation
                    )

                    NavigationIndex.SETTINGS.ordinal -> SettingTopBar(
                        settingViewModel,
                        configuration = configuration.orientation
                    )
                }
            },
            // 在纵向模式下显示底部导航栏
            bottomBar = {
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    NavigationBar(
                        currentPage = currentPage,
                        modViewModel = modViewModel,
                        onPageSelected = { page ->
                            currentPage = page
                            shouldScroll = true
                        }
                    )
                }
            }
        ) { innerPadding ->
            val context = LocalContext.current
            val exitToast: Toast =
                remember {
                    Toast.makeText(
                        context,
                        context.getText(R.string.toast_quit_app),
                        Toast.LENGTH_SHORT
                    )
                }
            val activity = context as? Activity
            val uiState = modViewModel.uiState.collectAsState().value
            val pagerState = rememberPagerState(
                initialPage = currentPage,
                pageCount = { pageList.size }
            )

            // 在 ConsolePage 显示退出确认
            BackHandler(enabled = currentPage == NavigationIndex.CONSOLE.ordinal) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - exitTime > 2000) {
                    exitToast.show()
                    exitTime = currentTime
                } else {
                    exitToast.cancel()
                    activity?.finish()
                }
            }

            // 在其他页面显示返回键返回到 ConsolePage
            BackHandler(enabled = currentPage != NavigationIndex.CONSOLE.ordinal) {
                currentPage = NavigationIndex.CONSOLE.ordinal
                shouldScroll = true
            }

            // 依赖 pagerState 的滚动状态自动更新 currentPage
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    if (!shouldScroll) {
                        currentPage = page
                    }
                }
            }

            // 监听 currentPage 变化并触发页面滚动
            LaunchedEffect(currentPage) {
                if (shouldScroll) {
                    pagerState.animateScrollToPage(
                        page = currentPage,
                        animationSpec = tween(
                            durationMillis = abs(pagerState.currentPage - currentPage) * 100 + 200,
                            easing = FastOutSlowInEasing
                        )
                    )
                    shouldScroll = false
                }
            }

            // 页面内容
            Box(modifier = Modifier.fillMaxSize()) {
                // 显示进度
                if (uiState.showTips) {
                    ProcessTips(
                        text = uiState.tipsText,
                        onDismiss = { modViewModel.setShowTips(false) },
                        uiState = uiState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding())
                            .zIndex(10f)
                    )
                }

                // 每个页面显示的内容
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .padding(innerPadding)
                        .zIndex(0f)
                ) { page ->
                    when (page) {
                        NavigationIndex.CONSOLE.ordinal -> ConsolePage(consoleViewModel)
                        NavigationIndex.MOD.ordinal -> ModPage(modViewModel)
                        NavigationIndex.SETTINGS.ordinal -> SettingPage(settingViewModel)
                    }
                }
            }
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
                        if ((currentTime - lastClickTime) < 300 && isSelected) {
                            refreshCurrentPage(currentPage, modViewModel)
                        } else {
                            // 非双击或者是切换页面时，执行页面切换
                            modViewModel.exitSelect()
                            if (!isSelected) {
                                onPageSelected(index)
                            }
                        }
                        lastClickTime = currentTime
                    },
                    icon = {
                        // 显示图标
                        Icon(imageVector = navigationItem.icon, contentDescription = null)
                    },
                    label = {
                        // 标签文字，仅在选中时显示
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
                    if ((currentTime - lastClickTime) < 300 && isSelected) {
                        refreshCurrentPage(currentPage, modViewModel)
                    } else {
                        // 非双击或者是切换页面时，执行页面切换
                        modViewModel.exitSelect()
                        if (!isSelected) {
                            onPageSelected(index)
                        }
                    }
                    lastClickTime = currentTime
                },
                icon = {
                    // 显示图标
                    Icon(imageVector = navigationItem.icon, contentDescription = null)
                },
                label = {
                    // 标签文字，仅在选中时显示
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
    // 根据当前页面的类型，执行相应的刷新逻辑
    when (currentPage) {
        NavigationIndex.CONSOLE.ordinal -> {}

        NavigationIndex.MOD.ordinal -> {
            modViewModel.flashMods(true)
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
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                ).also { bitmap ->
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
    onDismiss: () -> Unit,
    uiState: ModUiState,
    modifier: Modifier
) {
    // 构建提示文本
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

    // 显示提示
    Box(modifier.fillMaxWidth(0.7f)) {
        Snackbar(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            action = {
                Button(
                    onClick = onDismiss,
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
                text = "$tipsStart $tipsEnd",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }

}
