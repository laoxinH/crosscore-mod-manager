import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ImagesearchRoller
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ConsolePage
import top.laoxin.modmanager.ui.view.ConsoleTopBar
import top.laoxin.modmanager.ui.view.modview.ModPage
import top.laoxin.modmanager.ui.view.SettingPage
import top.laoxin.modmanager.ui.view.SettingTopBar
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




@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ModManagerApp() {

    val modViewModel: ModViewModel = viewModel(
        factory = ModViewModel.Factory
    )

    val consoleViewModel : ConsoleViewModel = viewModel(
        factory = ConsoleViewModel.Factory
    )
    // 导航栏
    val navController = rememberNavController()
    // 获取当前导航
    val currentEntry by navController.currentBackStackEntryAsState()
    // 当前导航索引
    val currentScreen = NavigationIndex.valueOf(
        currentEntry?.destination?.route ?: NavigationIndex.CONSOLE.name
    )
    Log.d("ModManagerApp", "currentScreen: $currentScreen")

    Scaffold(
        topBar = {
            when (currentScreen) {
                NavigationIndex.CONSOLE -> ConsoleTopBar(consoleViewModel)
                NavigationIndex.MOD -> ModTopBar(modViewModel)
                NavigationIndex.SETTINGS -> SettingTopBar()
            }
            val  uiState by modViewModel.uiState.collectAsState()
            Tips(
                text = uiState.tipsText,
                showTips = uiState.showTips,
                onDismiss = { modViewModel.setShowTips(false) },
                uiState = uiState
            )

            // 显示一条分割线

        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.Transparent,
            ){
                NavigationIndex.entries.forEachIndexed { index, navigationItem ->
                    NavigationBarItem(
                        selected = currentScreen == navigationItem,
                        onClick = {
                            modViewModel.exitSelect()
                            navController.navigate(navigationItem.name){
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = { // 图标
                            Icon(
                                imageVector = navigationItem.icon,
                                contentDescription = null
                            )
                        },
                        label = { // 文字
                            Text(text = stringResource(id = navigationItem.title) )
                        },
                        alwaysShowLabel = true,
                        /*colors = NavigationBarItemDefaults.colors( // 颜色配置
                            selectedIconColor = Color(0xff149ee7),
                            selectedTextColor = Color(0xff149ee7),
                            unselectedIconColor = Color(0xff999999),
                            unselectedTextColor = Color(0xff999999)
                        )*/
                    )
                }
            }
        }
    )
    { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .shadow(4.dp, shape = RectangleShape, clip = false)
                .background(Color.Gray)
        )
        NavHost(
            navController = navController,
            startDestination = NavigationIndex.CONSOLE.name,
            modifier = Modifier.padding(innerPadding)
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
        /*               when (uiState.currentNavigationIndex) {
                            0 -> {
                                ConsolePage()
                            }
                            1 -> {
                                LazyColumn(
                                    //state = lazyListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    items(100) {
                                        Text(
                                            text = "Itecem $it",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                               // ModPage()
                            }
                            2 -> {
                                Text(text = "Settings")
                            }
                        }*/

    }
}



@Preview(showBackground = true)
@Composable
fun PreviewBottomNavigationBar() {
    ModManagerTheme {
        ModManagerApp()
    }

}