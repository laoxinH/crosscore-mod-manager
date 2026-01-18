package top.laoxin.modmanager.ui.view.screens

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.view.components.common.DialogCommon
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import top.laoxin.modmanager.ui.view.components.common.ModernLoadingScreen
import top.laoxin.modmanager.ui.view.components.common.PermissionHandler
import top.laoxin.modmanager.ui.view.components.mod.DecryptProgressOverlay
import top.laoxin.modmanager.ui.view.components.mod.DeleteCheckConfirmDialog
import top.laoxin.modmanager.ui.view.components.mod.DeleteProgressOverlay
import top.laoxin.modmanager.ui.view.components.mod.EnableProgressOverlay
import top.laoxin.modmanager.ui.view.components.mod.ModDetailPartialBottomSheet
import top.laoxin.modmanager.ui.view.components.mod.ModernAllModPage
import top.laoxin.modmanager.ui.view.components.mod.ModernModsBrowser
import top.laoxin.modmanager.ui.view.components.mod.ScanProgressOverlay
import top.laoxin.modmanager.ui.view.navigation.ModBrowserRoute
import top.laoxin.modmanager.ui.view.navigation.ModListRoute
import top.laoxin.modmanager.ui.viewmodel.ModNavigationEvent
import top.laoxin.modmanager.ui.viewmodel.ModernModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModernModListViewModel
import top.laoxin.modmanager.ui.theme.ExpressiveOutlinedTextField
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton
import top.laoxin.modmanager.ui.viewmodel.MainViewModel
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
enum class NavigationIndex(
    @param:StringRes val title: Int,
    val index: Int,
) {
    ALL_MODS(R.string.mod_page_title_all_mods, 0),
    ENABLE_MODS(R.string.mod_page_title_enable_mods, 1),
    DISABLE_MODS(R.string.mod_page_title_disable_mods, 2),
    SEARCH_MODS(R.string.mod_page_title_search_mods, 3),
    MODS_BROWSER(R.string.mod_page_title_mods_browser, 4),
}
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ModernModScreen(
        modListViewModel: ModernModListViewModel = hiltViewModel(),
        modBrowserViewModel: ModernModBrowserViewModel = hiltViewModel(),
        modDetailViewModel: ModDetailViewModel = hiltViewModel(),
        modOperationViewModel: ModOperationViewModel = hiltViewModel(),
        modSearchViewModel: ModSearchViewModel = hiltViewModel(),
        modScanViewModel: ModScanViewModel = hiltViewModel(),
        mainViewModel: MainViewModel = hiltViewModel()
) {

    val modScanUiState by modScanViewModel.uiState.collectAsState()
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modDetailUiState by modDetailViewModel.uiState.collectAsState()

    val navController = rememberNavController()
// 扫描权限请求
    PermissionHandler(
            permissionStateFlow = modScanViewModel.permissionState,
            onPermissionGranted = modScanViewModel::onPermissionGranted,
            onPermissionDenied = modScanViewModel::onPermissionDenied,
            onRequestShizuku = modScanViewModel::requestShizukuPermission,
            isShizukuAvailable = modScanViewModel.isShizukuAvailable()
    )

    PermissionHandler(
            permissionStateFlow = modOperationViewModel.permissionState,
            onPermissionGranted = modOperationViewModel::onPermissionGranted,
            onPermissionDenied = modOperationViewModel::onPermissionDenied,
            onRequestShizuku = modOperationViewModel::requestShizukuPermission,
            isShizukuAvailable = modOperationViewModel.isShizukuAvailable()
    )
    // 拦截返回实践并处理
    /*BackHandler() {
        modListViewModel.onBackClick()
    }*/

    // Navigation Events Listener
    LaunchedEffect(Unit) {
        modListViewModel.navigationEvent.collect { event ->
            when (event) {
                is ModNavigationEvent.NavigateBack -> {
                    val currentEntry = navController.currentBackStackEntry
                    val isBrowser = currentEntry?.destination?.hasRoute<ModBrowserRoute>() == true

                    if (isBrowser) {
                        val currentPath = modBrowserViewModel.uiState.value.currentBrowsingPath
                        val rootPath = modBrowserViewModel.uiState.value.currentGameModPath

                        // Normalizing paths for comparison just in case
                        val isRoot =
                                currentPath == null ||
                                        currentPath == rootPath ||
                                        currentPath.trimEnd('/') == rootPath.trimEnd('/')

                        if (!isRoot) {
                            val parentPath = File(currentPath).parent

                            // Check if previous entry is already the parent (Normal navigation
                            // case)
                            val previousEntry = navController.previousBackStackEntry
                            // We check if previous destination is Browser Route. If so, check path.
                            val previousIsBrowser =
                                    previousEntry?.destination?.hasRoute<ModBrowserRoute>() == true

                            var isStackHealthy = false
                            if (previousIsBrowser) {
                                // Try to parse route. toRoute might throw if args don't match, so
                                // runCatching
                                runCatching {
                                    val route = previousEntry.toRoute<ModBrowserRoute>()
                                    if (route?.path == parentPath) {
                                        isStackHealthy = true
                                    }
                                }
                                        .onFailure {
                                            // ignore, treat as unhealthy
                                        }
                            }

                            if (isStackHealthy) {
                                // The stack is good, normal pop works
                                navController.popBackStack()
                            } else {
                                // The stack is broken (Restored state) or jumped.
                                // Navigate to Parent, replacing `Current` to avoid loops or
                                // confusing history.
                                // Stack: [List] -> [B]. Action: Navigate A (pop B). Result: [List]
                                // -> [A].
                                navController.navigate(ModBrowserRoute(parentPath)) {
                                    // 这是关键：告诉导航组件，在导航前，
                                    // 弹出返回栈，直到找到目标路径的实例。
                                    popUpTo(ModBrowserRoute(parentPath)) {
                                        // inclusive = false (默认) 意味着我们只弹出目标之上的屏幕，
                                        // 而保留目标屏幕本身。
                                        // 这可以防止在返回栈中创建重复的目标实例。
                                        inclusive = false

                                        // 如果你需要保存被弹出屏幕的状态以便后续恢复，可以设为 true
                                        // saveState = true
                                    }
                                    // 如果目标已经在栈顶，避免重复创建
                                    launchSingleTop = true
                                    // 当返回时，恢复之前保存的状态
                                    // restoreState = true

                                }
                                navController.navigate(ModBrowserRoute(parentPath)) {
                                    currentEntry.destination.id.let { id ->
                                        popUpTo(id) { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            // At Root, normal pop to List
                            navController.popBackStack()
                        }
                    } else {
                        // Not in Browser (e.g. in List or Detail?), normal pop
                        navController.popBackStack()
                    }
                }
                is ModNavigationEvent.NavigateToBrowser -> {
                    val targetPath = event.path
                    val rootPath = modBrowserViewModel.uiState.value.currentGameModPath

                    /* Log.d(
                        "ModernModScreen",
                        "Event: NavigateToBrowser. Target: $targetPath, Root: $rootPath"
                    )*/
                    val currentRoute =
                            navController.currentBackStackEntry?.toRoute<ModBrowserRoute>()

                    // Normalize Target: resolve null/empty to root, trim separators
                    val targetNorm =
                            (if (targetPath.isNullOrEmpty()) rootPath else targetPath)
                                    .trimEnd('/')
                                    .trimEnd('\\')
                    Log.d("ModernModScreen", "Normalized Target: $targetNorm")
                    if (currentRoute?.path != targetNorm) {
                        navController.navigate(ModBrowserRoute(targetNorm)) {
                            // 这是关键：告诉导航组件，在导航前，
                            // 弹出返回栈，直到找到目标路径的实例。
                            popUpTo(ModBrowserRoute(targetPath)) {
                                // inclusive = false (默认) 意味着我们只弹出目标之上的屏幕，
                                // 而保留目标屏幕本身。
                                // 这可以防止在返回栈中创建重复的目标实例。
                                inclusive = false

                                // 如果你需要保存被弹出屏幕的状态以便后续恢复，可以设为 true
                                // saveState = true
                            }
                            // 如果目标已经在栈顶，避免重复创建
                            launchSingleTop = true
                            // 当返回时，恢复之前保存的状态
                            // restoreState = true

                        }
                    }
                }
                is ModNavigationEvent.NavigateToList -> {
                    navController.navigate(ModListRoute) {
                        popUpTo(ModListRoute) { inclusive = true }
                    }
                }
            }
        }
    }

    // Intercept System Back (BackHandler)
    // Only intercept if we are not at the absolute root of the NavHost (Start Destination)
    // But since we have custom logic for Browser sub-folders, we always intercept if in Browser.
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Restore Navigation State (Run once)
    // We observe the flow to ensure we have the loaded preference
    val modListUiState by modListViewModel.uiState.collectAsState()
    val modBrowserUiState by modBrowserViewModel.uiState.collectAsState()
    val initialIsBrowser = remember { mutableStateOf<Boolean?>(null) }
    val initialPath =
            remember(
                    modBrowserUiState.currentGameModPath /*, modBrowserUiState.currentBrowsingPath*/
            ) {
                if (modBrowserUiState.currentBrowsingPath?.contains(
                                modBrowserUiState.currentGameModPath.trimEnd('/')
                        ) == true
                ) {
                    modBrowserUiState.currentBrowsingPath
                } else {
                    modBrowserUiState.currentGameModPath
                }
            }

    if (modScanUiState.isLoading /*|| initialPath.isNullOrEmpty()*/) {
        ModernLoadingScreen()
        return
    }
    val startDestination =
            if (modListUiState.isBrowser) ModBrowserRoute(initialPath) else ModListRoute
    // Log.d("ModernModScreen", "初始导航页面: $startDestination")
    LaunchedEffect(modListUiState.isLoading) {
        if (!modListUiState.isLoading && initialIsBrowser.value == null) {
            // Data is loaded, capture the initial preference once
            initialIsBrowser.value = modListUiState.isBrowser
            if (modListUiState.isBrowser) {
                // If we were in browser, restore it.
                // We navigate from List (startDest) to Browser to maintain Back stack [List,
                // Browser]
                navController.navigate(ModBrowserRoute(initialPath))
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable<ModListRoute>(
                    enterTransition = {
                        val fromBrowser = initialState.destination.hasRoute<ModBrowserRoute>()
                        if (fromBrowser) {
                            // View Mode Switch: Browser -> List (Crossfade)
                            fadeIn(animationSpec = tween(300))
                        } else {
                            // Default / Initial
                            fadeIn()
                        }
                    },
                    exitTransition = {
                        val toBrowser = targetState.destination.hasRoute<ModBrowserRoute>()
                        if (toBrowser) {
                            // View Mode Switch: List -> Browser (Crossfade)
                            fadeOut(animationSpec = tween(300))
                        } else {
                            slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                        }
                    },
                    popEnterTransition = {
                        val fromBrowser = initialState.destination.hasRoute<ModBrowserRoute>()
                        if (fromBrowser) {
                            fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn()
                        }
                    },
                    popExitTransition = {
                        val toBrowser = targetState.destination.hasRoute<ModBrowserRoute>()
                        if (toBrowser) {
                            fadeOut(animationSpec = tween(300))
                        } else {
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        }
                    }
            ) {
                // Sync State: We are in List
                LaunchedEffect(Unit) { modListViewModel.setIsBrowser(false) }

                ModernAllModPage(
                        modListViewModel = modListViewModel,
                        modDetailViewModel = modDetailViewModel,
                        modOperationViewModel = modOperationViewModel,
                        modSearchViewModel = modSearchViewModel,
                        modBrowserViewModel = modBrowserViewModel
                )
            }
            composable<ModBrowserRoute>(
                    enterTransition = {
                        val fromList = initialState.destination.hasRoute<ModListRoute>()
                        if (fromList) {
                            // View Mode Switch: List -> Browser (Crossfade)
                            fadeIn(animationSpec = tween(300))
                        } else {
                            // Directory Navigation (Smart Slide)
                            val initialPath =
                                    runCatching { initialState.toRoute<ModBrowserRoute>().path }
                                            .getOrNull()
                            val targetPath =
                                    runCatching { targetState.toRoute<ModBrowserRoute>().path }
                                            .getOrNull()

                            val isGoingUp =
                                    if (initialPath != null && targetPath != null) {
                                        initialPath.startsWith(targetPath) &&
                                                initialPath.length > targetPath.length
                                    } else {
                                        false
                                    }

                            if (isGoingUp) {
                                scaleIn(initialScale = 0.95f) + fadeIn()
                            } else {
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                            }
                        }
                    },
                    exitTransition = {
                        val toList = targetState.destination.hasRoute<ModListRoute>()
                        if (toList) {
                            // View Mode Switch: Browser -> List (Crossfade)
                            fadeOut(animationSpec = tween(300))
                        } else {
                            // Directory Navigation
                            val initialPath =
                                    runCatching { initialState.toRoute<ModBrowserRoute>().path }
                                            .getOrNull()
                            val targetPath =
                                    runCatching { targetState.toRoute<ModBrowserRoute>().path }
                                            .getOrNull()

                            val isGoingUp =
                                    if (initialPath != null && targetPath != null) {
                                        initialPath.startsWith(targetPath) &&
                                                initialPath.length > targetPath.length
                                    } else {
                                        false
                                    }

                            if (isGoingUp) {
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            } else {
                                slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                            }
                        }
                    },
                    popEnterTransition = {
                        val fromList = initialState.destination.hasRoute<ModListRoute>()
                        if (fromList) {
                            fadeIn(animationSpec = tween(300))
                        } else {
                            // Predictive Back / Directory Up
                            scaleIn(initialScale = 0.95f) + fadeIn()
                        }
                    },
                    popExitTransition = {
                        val toList = targetState.destination.hasRoute<ModListRoute>()
                        if (toList) {
                            fadeOut(animationSpec = tween(300))
                        } else {
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        }
                    }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<ModBrowserRoute>()

                // Sync State: We are in Browser
                LaunchedEffect(route) {
                    modListViewModel.setIsBrowser(true)
                    // Persist current browsing path
                    route.path?.let { modBrowserViewModel.setCurrentBrowsingPath(it) }
                }

                // Intercept System Back for Smart Navigation (Directory Up) & Multi-select
                val isMultiSelect = modListViewModel.uiState.collectAsState().value.isMultiSelect
                val rootPath = modBrowserViewModel.uiState.collectAsState().value.currentGameModPath
                val currentPath = route.path ?: rootPath

                // Normalization for comparison
                val isRoot =
                        currentPath == rootPath ||
                                currentPath.trimEnd('/').trimEnd('\\') ==
                                        rootPath.trimEnd('/').trimEnd('\\')

                // Only intercept if we need custom logic (Multi-select OR Sub-directory)

                // Predictive Back Animation State
                var swipeProgress by remember { mutableFloatStateOf(0f) }

                // Animated progress for smooth reset on cancel
                val animatedSwipeProgress by
                        animateFloatAsState(
                                targetValue = swipeProgress,
                                label = "swipeProgress",
                                animationSpec =
                                        spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                        )
                        )

                if (isMultiSelect || isRoot) {
                    BackHandler {
                        Log.d("ModernModScreen", "返回触发=, isRoot=$isRoot,当前路径=$currentPath")
                        if (isMultiSelect) {
                            modListViewModel.exitSelect()
                        } else {
                            mainViewModel.navigateToConsole()
                        }
                    }
                }

                PredictiveBackHandler(!isRoot) { progress ->
                    // Log.d("ModernModScreen", "预测返回触发=, isRoot=$isRoot,当前路径=$currentPath")
                    try {
                        progress.collect { backEvent -> swipeProgress = backEvent.progress }
                        // On Commit:
                        // 1. Snap to 1f (or keep high) to maintain the "shrunk" state during the
                        // exit transition.
                        // We do NOT reset to 0f here. The screen is leaving, so it should stay
                        // shrunk.
                        swipeProgress = 1f

                        // 2. Trigger Navigation
                        modListViewModel.onBackClick(isRoot)
                    } catch (e: CancellationException) {
                        // On Cancel:
                        // Reset progress to 0f. The animateFloatAsState will handle the smooth
                        // recoil.
                        swipeProgress = 0f
                    }
                }

                // Removed the LaunchedEffect(currentPath) reset
                // Reason: When 'currentPath' changes due to pop, we don't want this view (which is
                // exiting)
                // to suddenly snap back to full size (progress=0). Let it die in its shrunk state.

                Box(modifier = Modifier.fillMaxSize()) {
                    // --- Layer 1: Background Preview (Parent Directory) ---
                    // Only visible when swiping back in a subdirectory
                    if (animatedSwipeProgress > 0f && !isRoot) {
                        val previewBackPath = currentPath.let { File(it).parent } ?: rootPath

                        Box(
                                modifier =
                                        Modifier.fillMaxSize().graphicsLayer {
                                            // Parallax Effect: Grow from 95% to 100%
                                            val scale = 0.95f + (animatedSwipeProgress * 0.05f)
                                            scaleX = scale
                                            scaleY = scale

                                            // Fade in from 70% to 100%
                                            alpha = 0.7f + (animatedSwipeProgress * 0.3f)
                                        }
                        ) {
                            ModernModsBrowser(
                                    initialPath = previewBackPath,
                                    modBrowserViewModel = modBrowserViewModel,
                                    modListViewModel = modListViewModel,
                                    modDetailViewModel = modDetailViewModel,
                                    modOperationViewModel = modOperationViewModel,
                                    modSearchViewModel = modSearchViewModel,
                                    onNavigateToPath = { /* Disable interaction in preview */}
                            )

                            // Overlay to darken the background slightly
                            Box(
                                    modifier = Modifier.fillMaxSize()
                                    // .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f * (1f - animatedSwipeProgress)))
                                    )
                        }
                    }

                    // --- Layer 2: Foreground (Current Directory) ---
                    Box(
                            modifier =
                                    Modifier.fillMaxSize().graphicsLayer {
                                        val scale =
                                                1f - (animatedSwipeProgress * 0.1f) // Shrink to 90%
                                        scaleX = scale
                                        scaleY = scale

                                        // Slight slide to right
                                        translationX = animatedSwipeProgress * size.width * 0.15f

                                        // Round corners
                                        val cornerSize = 32.dp.toPx() * animatedSwipeProgress
                                        shape = RoundedCornerShape(cornerSize)
                                        clip = true

                                        // Fade slightly
                                        alpha = 1f - (animatedSwipeProgress * 0.2f)
                                    }
                    ) {
                        ModernModsBrowser(
                                initialPath = route.path,
                                modBrowserViewModel = modBrowserViewModel,
                                modListViewModel = modListViewModel,
                                modDetailViewModel = modDetailViewModel,
                                modOperationViewModel = modOperationViewModel,
                                modSearchViewModel = modSearchViewModel,
                                onNavigateToPath = { path ->
                                    navController.navigate(ModBrowserRoute(path))
                                }
                        )
                    }
                }
            }
        }

        // Global Overlays (Persist across navigation)
        ScanProgressOverlay(
                progressState = modScanUiState.scanProgress,
                onCancel = { modScanViewModel.cancelScan() },
                onDismiss = { modScanViewModel.dismissScanResult() },
                onGoSettings = {
                    modScanViewModel.dismissScanResult()
                    mainViewModel.navigateToSettings()
                },
                onGrantPermission = modScanViewModel::requestPermissionFromError,
                onDisableMod = { mod ->
                    modOperationViewModel.switchSelectMods(listOf(mod), false, silent = true)
                },
                onDisableAllMods = { mods ->
                    modOperationViewModel.switchSelectMods(mods, false, silent = true)
                    modScanViewModel.dismissScanResult()
                },
                onSwitchToBackground = { modScanViewModel.switchToBackground() }
        )

        EnableProgressOverlay(
                progressState = modOperationUiState.enableProgress,
                onCancel = { modOperationViewModel.cancelOperation() },
                onDismiss = { modOperationViewModel.dismissEnableProgress() },
                onGoSettings = {
                    modOperationViewModel.dismissEnableProgress()
                    mainViewModel.navigateToSettings()
                },
                onGrantPermission = modOperationViewModel::requestPermissionFromEnableError,
                onDisableMod = { mod ->
                    modOperationViewModel.switchSelectMods(listOf(mod), false, silent = true)
                },
                onRemoveFromSelection = { mod -> modListViewModel.removeModSelection(mod.id) }
        )

        DecryptProgressOverlay(
                progressState = modOperationUiState.decryptProgress,
                onCancel = { modOperationViewModel.cancelDecrypt() },
                onConfirm = { modOperationViewModel.confirmDecryptSuccess() },
                onDismiss = { modOperationViewModel.dismissDecryptProgress() }
        )

        // MOD 删除进度覆盖层
        DeleteProgressOverlay(
                progressState = modOperationUiState.deleteProgress,
                onCancel = { modOperationViewModel.cancelDelete() },
                onDismiss = { modOperationViewModel.dismissDeleteProgress() },
                onDisableMod = { mod ->
                    modOperationViewModel.switchSelectMods(
                            listOf(mod),
                            enable = false,
                            silent = true
                    )
                },
                onDisableAllMods = { mods ->
                    modOperationViewModel.switchSelectMods(mods, enable = false, silent = true)
                }
        )

        // 删除检测结果覆盖层
        DeleteCheckConfirmDialog(
                checkState = modOperationUiState.deleteCheckState,
                onCancel = { modOperationViewModel.dismissDeleteCheck() },
                onSkipIntegrated = { modOperationViewModel.confirmDeleteSkipIntegrated() },
                onDeleteAll = { modOperationViewModel.confirmDeleteAll() },
                onDisableMod = { mod ->
                    modOperationViewModel.switchSelectMods(
                            listOf(mod),
                            enable = false,
                            silent = true
                    )
                },
                onDisableAllMods = { mods ->
                    modOperationViewModel.switchSelectMods(mods, enable = false, silent = true)
                },
        )

        // Dialogs

        ForceUpdateDialog(
                showDialog = modScanUiState.showForceScanDialog,
                modScanViewModel = modScanViewModel
        )

        /*            if (modOperationUiState.showDisEnableModsDialog) {
             val selectedMods = modListViewModel.getSelectableModsForSwitch(false)
            DisEnableModsDialog(
                showDialog = modOperationUiState.showDisEnableModsDialog,
                mods = selectedMods,
                switchMod = { mod, enable -> modOperationViewModel.switchMod(mod, enable) },
                onConfirmRequest = {
                    modOperationViewModel.switchSelectMods(selectedMods, false)
                    modOperationViewModel.setShowDisEnableModsDialog(false)
                },
                modDetailViewModel = modDetailViewModel
            )
        }*/

        if (modOperationUiState.showPasswordDialog) {
            PasswordInputDialog(
                    mod = modOperationUiState.passwordRequestMod,
                    errorMessage = modOperationUiState.passwordError,
                    onDismiss = { modOperationViewModel.dismissPasswordDialog() },
                    onSubmit = { password -> modOperationViewModel.submitPassword(password) }
            )
        }

        if (modOperationUiState.showDelSelectModsDialog) {
            DeleteCheckConfirmDialog(
                    checkState = modOperationUiState.deleteCheckState,
                    onCancel = { modOperationViewModel.dismissDeleteCheck() },
                    onSkipIntegrated = { modOperationViewModel.confirmDeleteSkipIntegrated() },
                    onDeleteAll = { modOperationViewModel.confirmDeleteAll() },
                    onDisableMod = { mod ->
                        modOperationViewModel.switchSelectMods(
                                listOf(mod),
                                enable = false,
                                silent = true
                        )
                    },
                    onDisableAllMods = { mods ->
                        modOperationViewModel.switchSelectMods(mods, enable = false, silent = true)
                    },
            )
        }

        // Mod Detail Bottom Sheet
        val showModDetail = modDetailUiState.isShown
        if (showModDetail) {
            modDetailUiState.mod?.let { mod ->
                ModDetailPartialBottomSheet(
                        showDialog = showModDetail,
                        mod = mod,
                        modDetailViewModel = modDetailViewModel,
                        modScanViewModel = modScanViewModel,
                        modOperationViewModel = modOperationViewModel,
                        onDismiss = { modDetailViewModel.setShowModDetail(false) }
                )
            }
        }
    }
}
@Composable
fun PasswordInputDialog(
    mod: ModBean? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.password_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                ExpressiveOutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.password_dialog_label)) },
                    isError = isError || !errorMessage.isNullOrEmpty(),
                    supportingText = {
                        if (!errorMessage.isNullOrEmpty()) {
                            Text(errorMessage)
                        }
                    }
                )
            }
        },
        confirmButton = {
            ExpressiveTextButton(onClick = { onSubmit(password) }) {
                Text(text = stringResource(id = R.string.dialog_button_confirm))
            }
        },
        dismissButton = {
            ExpressiveTextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dialog_button_request_close))
            }
        }
    )
}

@Composable
fun ForceUpdateDialog(showDialog: Boolean, modScanViewModel: ModScanViewModel) {
    if (showDialog) {
        DialogCommon(
            title = stringResource(id = R.string.console_scan_directory_mods),
            content = stringResource(id = R.string.mod_page_force_update_mod_warning),
            onConfirm = {
                modScanViewModel.flashMods(isLoading = true, forceScan = true)
                modScanViewModel.setShowForceScanDialog(false)
            },
            onCancel = { modScanViewModel.setShowForceScanDialog(false) },
            showDialog = showDialog
        )
    }
}