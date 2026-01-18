package top.laoxin.modmanager.ui.view.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.laoxin.modmanager.ui.view.components.console.ConsoleTopBar
import top.laoxin.modmanager.ui.view.components.mod.ModernModTopBar
import top.laoxin.modmanager.ui.view.components.setting.SettingTopBar
import top.laoxin.modmanager.ui.view.navigation.Route
import top.laoxin.modmanager.ui.viewmodel.ModernModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModernModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel

@Composable
fun ModernTopBar(
    currentRoute: Route, 
    modifier: Modifier = Modifier,
    modListViewModel: ModernModListViewModel? = null,
    modBrowserViewModel: ModernModBrowserViewModel? = null,
    modOperationViewModel: ModOperationViewModel? = null,
    modSearchViewModel: ModSearchViewModel? = null,
    modScanViewModel: ModScanViewModel? = null
) {
    val configuration = LocalConfiguration.current.orientation

    AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                // Simple fade and slide transition for TopBar
                (fadeIn() + slideInVertically { -it / 2 }) togetherWith
                        (fadeOut() + slideOutVertically { -it / 2 })
            },
            label = "TopBarTransition",
            modifier = modifier.fillMaxWidth()
    ) { route ->
        when (route) {
            is Route.Console -> {
                val viewModel: ConsoleViewModel = hiltViewModel()
                ConsoleTopBar(viewModel = viewModel, configuration = configuration)
            }
            is Route.ModList -> {
                val listViewModel = modListViewModel ?: hiltViewModel()
                val browserViewModel = modBrowserViewModel ?: hiltViewModel()
                val operationViewModel = modOperationViewModel ?: hiltViewModel()
                val searchViewModel = modSearchViewModel ?: hiltViewModel()
                val scanViewModel = modScanViewModel ?: hiltViewModel()

                // Intercept System Back for MultiSelect (User Request & Failsafe)
                val isMultiSelect = listViewModel.uiState.collectAsState().value.isMultiSelect
                BackHandler(enabled = isMultiSelect) {
                    listViewModel.onBackClick()
                }

                ModernModTopBar(
                    modListViewModel = listViewModel,
                    modBrowserViewModel = browserViewModel,
                    modOperationViewModel = operationViewModel,
                    modSearchViewModel = searchViewModel,
                    modScanViewModel = scanViewModel,
                    configuration = configuration
                )
            }
            is Route.Settings -> {
                val viewModel: SettingViewModel = hiltViewModel()
                SettingTopBar(viewModel = viewModel, configuration = configuration)
            }
        }
    }
}
