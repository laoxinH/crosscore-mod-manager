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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.view.modview.ModPage
import top.laoxin.modmanager.ui.view.modview.ModTopBar
import top.laoxin.modmanager.ui.view.modview.Tips
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel
import top.laoxin.modmanager.ui.viewmodel.ModViewModel


// 导航栏索引
enum class NavigationIndex(
    @StringRes val title: Int,
    val icon: ImageVector,
) {
    CONSOLE(R.string.console, Icons.Filled.Dashboard),
    MOD(R.string.mod, Icons.Filled.ImagesearchRoller),
    SETTINGS(R.string.settings, Icons.Filled.Settings)
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModManagerApp() {
    val modViewModel: ModViewModel = viewModel(factory = ModViewModel.Factory)
    val consoleViewModel: ConsoleViewModel = viewModel(factory = ConsoleViewModel.Factory)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { NavigationIndex.entries.size }
    )
    val configuration = LocalConfiguration.current

    val scope = rememberCoroutineScope()
    var exitTime by remember { mutableLongStateOf(0L) }

    Row {
        // 根据屏幕方向选择布局
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NavigationRail(
                pagerState = pagerState,
                modViewModel = modViewModel,
                consoleViewModel = consoleViewModel
            )
        }

        Scaffold(
            topBar = {
                // 根据当前页面显示不同的顶部工具栏
                when (pagerState.currentPage) {
                    NavigationIndex.CONSOLE.ordinal -> ConsoleTopBar(
                        consoleViewModel,
                        configuration = configuration.orientation
                    )

                    NavigationIndex.MOD.ordinal -> ModTopBar(
                        modViewModel,
                        configuration = configuration.orientation
                    )

                    NavigationIndex.SETTINGS.ordinal -> SettingTopBar(configuration = configuration.orientation)
                }
                val uiState by modViewModel.uiState.collectAsState()
                Tips(
                    text = uiState.tipsText,
                    showTips = uiState.showTips,
                    onDismiss = { modViewModel.setShowTips(false) },
                    uiState = uiState
                )
            },
            bottomBar = {
                // 在纵向模式下显示底部导航栏
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    NavigationBar(
                        pagerState = pagerState,
                        modViewModel = modViewModel
                    )
                }
            }
        ) { innerPadding ->
            val context = LocalContext.current
            val exitToast: Toast =
                remember {
                    Toast.makeText(
                        context,
                        context.getText(R.string.toast_quit_qpp),
                        Toast.LENGTH_SHORT
                    )
                }
            val activity = context as? Activity

            BackHandler(enabled = pagerState.currentPage == NavigationIndex.CONSOLE.ordinal) {
                // 在 ConsolePage 显示退出确认
                val currentTime = System.currentTimeMillis()
                if (currentTime - exitTime > 2000) {
                    exitToast.show()
                    exitTime = currentTime
                } else {
                    // 安全关闭应用
                    exitToast.cancel()
                    activity?.finish()
                }
            }

            BackHandler(enabled = pagerState.currentPage != NavigationIndex.CONSOLE.ordinal) {
                // 返回到 ConsolePage
                scope.launch {
                    pagerState.animateScrollToPage(NavigationIndex.CONSOLE.ordinal)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding)
            ) { page ->
                when (page) {
                    NavigationIndex.CONSOLE.ordinal -> ConsolePage(consoleViewModel)
                    NavigationIndex.MOD.ordinal -> ModPage(modViewModel)
                    NavigationIndex.SETTINGS.ordinal -> SettingPage()
                }
            }
        }
    }
}

//侧边导航
@Composable
fun NavigationRail(
    pagerState: PagerState,
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val packageName = consoleViewModel.uiState.collectAsState().value.gameInfo.packageName
    val gameIcon = remember(packageName) {
        getGameIcon(packageName)
    }

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .padding(0.dp),
        // containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        // 顶部的当前页面名称
        val currentPageName =
            stringResource(id = NavigationIndex.entries[pagerState.currentPage].title)
        Text(
            text = currentPageName,
            modifier = Modifier.padding(16.dp)
        )
        Column {

            NavigationIndex.entries.forEachIndexed { index, navigationItem ->
                val isSelected = pagerState.currentPage == index

                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if ((currentTime - lastClickTime) < 300 && isSelected) {
                            refreshCurrentPage(pagerState.currentPage, modViewModel)
                        } else {
                            modViewModel.exitSelect()
                            if (!isSelected) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
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
            modifier = Modifier
                .padding(bottom = 16.dp)
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
    pagerState: PagerState,
    modViewModel: ModViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var lastClickTime by remember { mutableLongStateOf(0L) }

    NavigationBar {
        NavigationIndex.entries.forEachIndexed { index, navigationItem ->
            val isSelected = pagerState.currentPage == index

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if ((currentTime - lastClickTime) < 300 && isSelected) { // 检测双击
                        refreshCurrentPage(pagerState.currentPage, modViewModel)
                    } else {
                        modViewModel.exitSelect()
                        if (!isSelected) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
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