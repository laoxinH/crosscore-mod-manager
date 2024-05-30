import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.ConsolePage
import top.laoxin.modmanager.ui.view.modview.ModPage
import top.laoxin.modmanager.ui.view.SettingPage
import top.laoxin.modmanager.ui.viewmodel.ModManagerViewModel



// 导航栏索引
enum class NavigationIndex(
    @StringRes val title: Int,
    val icon: ImageVector
) {
    CONSOLE(R.string.console, Icons.Filled.Dashboard),
    MOD(R.string.mod, Icons.Filled.ImagesearchRoller),
    SETTINGS(R.string.settings, Icons.Filled.Settings)
}




@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ModManagerApp() {
    // 数据
    val viewModel: ModManagerViewModel = viewModel()

    // 导航栏
    val navController = rememberNavController()
    // 获取当前导航
    val currentEntry by navController.currentBackStackEntryAsState()
    // 当前导航索引
    val currentScreen = NavigationIndex.valueOf(
        currentEntry?.destination?.route ?: NavigationIndex.CONSOLE.name
    )

    Scaffold(


        //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        //topBar = { MediumTopAppBarExample(scrollBehavior) },
        bottomBar = {
            NavigationBar(
               // containerColor = Color.White
            ) {

                NavigationIndex.entries.forEachIndexed { index, navigationItem ->
                    NavigationBarItem(
                        selected = currentScreen == navigationItem,
                        onClick = {
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


        NavHost(
            navController = navController,
            startDestination = NavigationIndex.CONSOLE.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = NavigationIndex.CONSOLE.name) {
               ConsolePage()
            }

            composable(route = NavigationIndex.MOD.name) {
                ModPage()
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