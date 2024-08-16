package top.laoxin.modmanager.ui.view

import android.content.res.Configuration
import androidx.annotation.StringRes
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentScreen = NavigationIndex.valueOf(
        currentEntry?.destination?.route ?: NavigationIndex.CONSOLE.name
    )
    val configuration = LocalConfiguration.current

    Row {
        // 侧边栏
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NavigationRail(
                navController = navController,
                currentScreen = currentScreen,
                modViewModel = modViewModel,
            )
        }

        // 主内容区域
        Scaffold(
            //顶栏
            topBar = {
                when (currentScreen) {
                    NavigationIndex.CONSOLE -> ConsoleTopBar(consoleViewModel)
                    NavigationIndex.MOD -> ModTopBar(modViewModel)
                    NavigationIndex.SETTINGS -> SettingTopBar()
                }
                val uiState by modViewModel.uiState.collectAsState()
                Tips(
                    text = uiState.tipsText,
                    showTips = uiState.showTips,
                    onDismiss = { modViewModel.setShowTips(false) },
                    uiState = uiState
                )
            },
            //底栏
            bottomBar = {
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    NavigationBar(
                        navController = navController,
                        currentScreen = currentScreen,
                        modViewModel = modViewModel
                    )
                }
            }
        ) { innerPadding ->
            NavigationHost(
                navController = navController,
                modViewModel = modViewModel,
                consoleViewModel = consoleViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

//侧边导航
@Composable
fun NavigationRail(
    navController: NavController,
    currentScreen: NavigationIndex,
    modViewModel: ModViewModel
) {
    NavigationRail(
        modifier = Modifier
            .defaultMinSize(minWidth = 100.dp)
            .fillMaxHeight()
            .padding(0.dp)
    ) {
        Spacer(Modifier.weight(1f)) // 用于将内容垂直居中
        NavigationIndex.entries.forEach { navigationItem ->
            Spacer(Modifier.height(16.dp))
            NavigationRailItem(
                selected = currentScreen == navigationItem,
                onClick = {
                    modViewModel.exitSelect()
                    navController.navigate(navigationItem.name) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(imageVector = navigationItem.icon, contentDescription = null)
                },
                label = {
                    Text(text = stringResource(id = navigationItem.title))
                }
            )
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.weight(1f))
    }
}


//底部导航
@Composable
fun NavigationBar(
    navController: NavController,
    currentScreen: NavigationIndex,
    modViewModel: ModViewModel
) {
    NavigationBar {
        NavigationIndex.entries.forEach { navigationItem ->
            NavigationBarItem(
                selected = currentScreen == navigationItem,
                onClick = {
                    modViewModel.exitSelect()
                    navController.navigate(navigationItem.name) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(imageVector = navigationItem.icon, contentDescription = null)
                },
                label = {
                    Text(text = stringResource(id = navigationItem.title))
                }
            )
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