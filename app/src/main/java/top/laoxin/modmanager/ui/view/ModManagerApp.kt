package top.laoxin.modmanager.ui.view

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
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


@Composable
fun ModManagerApp() {
    val modViewModel: ModViewModel = viewModel(factory = ModViewModel.Factory)
    val consoleViewModel: ConsoleViewModel = viewModel(factory = ConsoleViewModel.Factory)
    val pagerState = rememberPagerState()
    val configuration = LocalConfiguration.current

    Row {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NavigationRail(
                pagerState = pagerState,
                modViewModel = modViewModel
            )
        }

        Scaffold(
            topBar = {
                when (pagerState.currentPage) {
                    NavigationIndex.CONSOLE.ordinal -> ConsoleTopBar(consoleViewModel)
                    NavigationIndex.MOD.ordinal -> ModTopBar(modViewModel)
                    NavigationIndex.SETTINGS.ordinal -> SettingTopBar()
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
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    NavigationBar(
                        pagerState = pagerState,
                        modViewModel = modViewModel
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                count = NavigationIndex.entries.size,
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
    modViewModel: ModViewModel
) {
    val coroutineScope = rememberCoroutineScope()

    NavigationRail(
        modifier = Modifier
            .defaultMinSize(minWidth = 80.dp)
            .fillMaxHeight()
            .padding(0.dp)
    ) {
        Spacer(Modifier.weight(1f))
        NavigationIndex.entries.forEachIndexed { index, navigationItem ->
            val isSelected = pagerState.currentPage == index
            var lastClickTime by remember { mutableLongStateOf(0L) }

            Spacer(Modifier.height(12.dp))
            NavigationRailItem(
                selected = pagerState.currentPage == index,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if ((currentTime - lastClickTime) < 300) { // 检测双击
                        // 刷新当前页面的逻辑
                        refreshCurrentPage(pagerState.currentPage, modViewModel)
                    } else {
                        modViewModel.exitSelect()
                        if (!isSelected) {
                            coroutineScope.launch {
                                pagerState.scrollToPage(index)
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
                alwaysShowLabel = false // 确保标签只在 isSelected 为 true 时显示
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.weight(1f))
    }
}

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
                    if ((currentTime - lastClickTime) < 300) { // 检测双击
                        // 刷新当前页面的逻辑
                        refreshCurrentPage(pagerState.currentPage, modViewModel)
                    } else {
                        modViewModel.exitSelect()
                        if (!isSelected) {
                            coroutineScope.launch {
                                pagerState.scrollToPage(index)
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
                alwaysShowLabel = false // 确保标签只在 isSelected 为 true 时显示
            )
        }
    }
}

private fun refreshCurrentPage(currentPage: Int, modViewModel: ModViewModel) {
    // 根据当前页面的类型，执行相应的刷新逻辑
    when (currentPage) {
        NavigationIndex.CONSOLE.ordinal -> {
            // 刷新控制台页面的逻辑
        }

        NavigationIndex.MOD.ordinal -> {
            modViewModel.flashMods(false, true)
        }

        NavigationIndex.SETTINGS.ordinal -> {
            // 刷新设置页面逻辑
        }
    }
}


//导航
@Composable
fun NavigationHost(
    navController: NavHostController,
    modViewModel: ModViewModel,
    consoleViewModel: ConsoleViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = NavigationIndex.CONSOLE.name,
        modifier = modifier
    ) {
        composable(route = NavigationIndex.CONSOLE.name) {
            ConsolePage(consoleViewModel)
        }
        composable(route = NavigationIndex.MOD.name) {
            ModPage(modViewModel)
        }
        composable(route = NavigationIndex.SETTINGS.name) {
            SettingPage()
        }
    }
}