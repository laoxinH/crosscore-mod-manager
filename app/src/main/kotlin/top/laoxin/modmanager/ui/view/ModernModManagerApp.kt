package top.laoxin.modmanager.ui.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.view.components.ModernTopBar
import top.laoxin.modmanager.ui.view.navigation.Route
import top.laoxin.modmanager.ui.view.screens.ConsoleScreen
import top.laoxin.modmanager.ui.view.screens.ModernModScreen
import top.laoxin.modmanager.ui.view.screens.SettingScreen
import top.laoxin.modmanager.ui.viewmodel.ModernModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModernModListViewModel
import top.laoxin.modmanager.ui.view.components.common.GlobalSnackbarHost
import top.laoxin.modmanager.ui.viewmodel.MainViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
import top.laoxin.modmanager.ui.viewmodel.NavigationEvent

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ModernModManagerApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val snackbarManager = viewModel.snackbarManager
    val snackbarHostState = remember { SnackbarHostState() }
    // Navigation 2 Controller
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine current route for UI (TopBar/BottomBar)
    val currentRoute: Route =
        when {
            currentDestination?.hasRoute<Route.Console>() == true -> Route.Console
            currentDestination?.hasRoute<Route.ModList>() == true -> Route.ModList
            currentDestination?.hasRoute<Route.Settings>() == true -> Route.Settings
            else -> Route.Console // Default
        }

    // Hoist Mod ViewModels
    val modListViewModel: ModernModListViewModel = hiltViewModel()
    val modBrowserViewModel: ModernModBrowserViewModel = hiltViewModel()
    val modOperationViewModel: ModOperationViewModel = hiltViewModel()
    val modSearchViewModel: ModSearchViewModel = hiltViewModel()
    val modScanViewModel: ModScanViewModel = hiltViewModel()

    // Observe Global Navigation Events (e.g. from Overlays)
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->

            val route =
                when (event) {
                    is NavigationEvent.NavigateToConsole ->
                        Route.Console

                    is NavigationEvent.NavigateToMod ->
                        Route.ModList

                    is NavigationEvent.NavigateToSettings ->
                        Route.Settings
                }

          /*  Log.d("ModernModManagerApp", "当前导航页面: $route")
            Log.d("ModernModManagerApp", "当前页面: $currentRoute")
            Log.d("ModernModManagerApp","是否为当前页面:${currentRoute::class != route::class}")*/
            // 0. Debounce
          //  if (currentRoute::class != route::class) {
                navController.navigate(route) {
                     when (route) {
                         Route.Console -> popUpTo(Route.Console) { inclusive = true }
                         Route.ModList -> popUpTo(Route.ModList) { inclusive = true }
                         Route.Settings -> popUpTo(Route.Settings) { inclusive = true }
                     }
                    launchSingleTop = true
                    restoreState = true
                }
           // }
        }
    }

    // Unified Navigation Logic (Loop Breaker)
    val onNavigate: (Route) -> Unit = { route ->
        // 0. Debounce
        if (currentRoute::class != route::class) {
            navController.navigate(route) {
                // Loop Breaker Logic
                when (route) {
                     Route.Console -> popUpTo(Route.Console) { inclusive = true }
                     Route.ModList -> popUpTo(Route.ModList) { inclusive = true }
                     Route.Settings -> popUpTo(Route.Settings) { inclusive = true }
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val showBottomBar = viewModel.isBottomBarVisible.value
    
    // Rail Data
    val gameIcon by viewModel.gameIcon.collectAsState()
    val railTitle = when (currentRoute) {
        Route.Console -> stringResource(R.string.console)
        Route.ModList -> stringResource(R.string.mod)
        Route.Settings -> stringResource(R.string.settings)
    }

    if (isLandscape) {
        // Landscape Layout: Row { Rail | Content }
        Row {
            if (showBottomBar) {
                ModernNavigationRail(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    title = railTitle,
                    gameIcon = gameIcon
                )
            }
            Scaffold(
                topBar = {
                    ModernTopBar(
                        currentRoute = currentRoute,
                        modListViewModel = modListViewModel,
                        modBrowserViewModel = modBrowserViewModel,
                        modOperationViewModel = modOperationViewModel,
                        modSearchViewModel = modSearchViewModel,
                        modScanViewModel = modScanViewModel
                    )
                },
                snackbarHost = {
                    GlobalSnackbarHost(
                        snackbarManager = snackbarManager,
                        snackbarHostState = snackbarHostState
                    )
                }
            ) { innerPadding ->
                 // Content (Same as Portrait)
                 NavHostContent(navController, modListViewModel, modBrowserViewModel, modOperationViewModel, modSearchViewModel, modScanViewModel, viewModel , innerPadding)
            }
        }
    } else {
        // Portrait Layout: Standard Scaffold
        Scaffold(
            topBar = {
                ModernTopBar(
                    currentRoute = currentRoute,
                    modListViewModel = modListViewModel,
                    modBrowserViewModel = modBrowserViewModel,
                    modOperationViewModel = modOperationViewModel,
                    modSearchViewModel = modSearchViewModel,
                    modScanViewModel = modScanViewModel
                )
            },
            snackbarHost = {
                GlobalSnackbarHost(
                    snackbarManager = snackbarManager,
                    snackbarHostState = snackbarHostState
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    ModernBottomBar(currentRoute = currentRoute, onNavigate = onNavigate)
                }
            }
        ) { innerPadding ->
             NavHostContent(navController, modListViewModel, modBrowserViewModel, modOperationViewModel, modSearchViewModel, modScanViewModel,viewModel, innerPadding)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun NavHostContent(
    navController: androidx.navigation.NavHostController,
    modListViewModel: ModernModListViewModel,
    modBrowserViewModel: ModernModBrowserViewModel,
    modOperationViewModel: ModOperationViewModel,
    modSearchViewModel: ModSearchViewModel,
    modScanViewModel: ModScanViewModel,
    mainViewModel: MainViewModel,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {

        // Helper for index-based slide using NavBackStackEntry
        fun getIndex(entry: NavBackStackEntry): Int {
            return when {
                entry.destination.hasRoute<Route.Console>() -> 0
                entry.destination.hasRoute<Route.ModList>() -> 1
                entry.destination.hasRoute<Route.Settings>() -> 2
                else -> -1
            }
        }

        // Shared Transition Logic
        // Premium Spec: Slower, smoother
        val premiumTween = tween<IntOffset>(
            durationMillis = 700,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
        )
        val premiumFadeTween = tween<Float>(durationMillis = 700)

        fun getTransition(
            initialIndex: Int,
            targetIndex: Int,
            isEnter: Boolean
        ): EnterTransition {
            return if (initialIndex != -1 && targetIndex != -1) {
                if (targetIndex > initialIndex) {
                    // Moving Right (0->1, 1->2)
                    if (isEnter) slideInHorizontally(premiumTween) { width -> width } + fadeIn(
                        premiumFadeTween
                    )
                    else slideInHorizontally(premiumTween) { width -> -width } + fadeIn(
                        premiumFadeTween
                    )
                } else {
                    // Moving Left (2->1, 1->0)
                    if (isEnter) slideInHorizontally(premiumTween) { width -> -width } + fadeIn(
                        premiumFadeTween
                    )
                    else slideInHorizontally(premiumTween) { width -> width } + fadeIn(
                        premiumFadeTween
                    )
                }
            } else {
                fadeIn(premiumFadeTween)
            }
        }

        NavHost(
            navController = navController,
            startDestination = Route.Console,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                val initialIndex = getIndex(initialState)
                val targetIndex = getIndex(targetState)
                if (targetIndex > initialIndex) {
                    // 0 -> 1: Enter from Right
                    slideInHorizontally(premiumTween) { it } + fadeIn(premiumFadeTween)
                } else {
                    // 1 -> 0: Enter from Left
                    slideInHorizontally(premiumTween) { -it } + fadeIn(premiumFadeTween)
                }
            },
            exitTransition = {
                val initialIndex = getIndex(initialState)
                val targetIndex = getIndex(targetState)
                if (targetIndex > initialIndex) {
                    // 0 -> 1: Exit to Left
                    slideOutHorizontally(premiumTween) { -it } + fadeOut(premiumFadeTween)
                } else {
                    // 1 -> 0: Exit to Right
                    slideOutHorizontally(premiumTween) { it } + fadeOut(premiumFadeTween)
                }
            },
            popEnterTransition = {
                val initialIndex = getIndex(initialState)
                val targetIndex = getIndex(targetState)
                // Pop is usually "Back". 1 -> 0. target(0) < initial(1).
                // Use the same directional logic: compare indices.
                if (targetIndex > initialIndex) {
                    slideInHorizontally(premiumTween) { it } + fadeIn(premiumFadeTween)
                } else {
                    slideInHorizontally(premiumTween) { -it } + fadeIn(premiumFadeTween)
                }
            },
            popExitTransition = {
                val initialIndex = getIndex(initialState)
                val targetIndex = getIndex(targetState)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(premiumTween) { -it } + fadeOut(premiumFadeTween)
                } else {
                    slideOutHorizontally(premiumTween) { it } + fadeOut(premiumFadeTween)
                }
            }
        ) {
            composable<Route.Console> {
                ConsoleScreen()
            }
            composable<Route.ModList> {
                ModernModScreen(
                    modListViewModel = modListViewModel,
                    modBrowserViewModel = modBrowserViewModel,
                    modOperationViewModel = modOperationViewModel,
                    modSearchViewModel = modSearchViewModel,
                    modScanViewModel = modScanViewModel,
                    mainViewModel = mainViewModel
                )
            }
            composable<Route.Settings> {
                SettingScreen()
            }
        }
    }

@Composable
fun ModernBottomBar(currentRoute: Route, onNavigate: (Route) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute is Route.Console,
            onClick = { onNavigate(Route.Console) },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
            label = { Text(stringResource(R.string.console)) }
        )
        NavigationBarItem(
            selected = currentRoute is Route.ModList,
            onClick = { onNavigate(Route.ModList) },
            icon = { Icon(Icons.Filled.ImagesearchRoller, contentDescription = null) },
            label = { Text(stringResource(R.string.mod)) }
        )
        NavigationBarItem(
            selected = currentRoute is Route.Settings,
            onClick = { onNavigate(Route.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings)) }
        )
    }
}
@Composable
fun ModernNavigationRail(
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    title: String,
    gameIcon: androidx.compose.ui.graphics.ImageBitmap?
) {
    androidx.compose.material3.NavigationRail {
        // Header: Page Title
        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        
        // Navigation Items
        NavigationRailItem(
            selected = currentRoute is Route.Console,
            onClick = { onNavigate(Route.Console) },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
            label = { Text(stringResource(R.string.console)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        NavigationRailItem(
            selected = currentRoute is Route.ModList,
            onClick = { onNavigate(Route.ModList) },
            icon = { Icon(Icons.Filled.ImagesearchRoller, contentDescription = null) },
            label = { Text(stringResource(R.string.mod)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        NavigationRailItem(
            selected = currentRoute is Route.Settings,
            onClick = { onNavigate(Route.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings)) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Footer: Game Icon
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            if (gameIcon != null) {
                Image(
                    bitmap = gameIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}
