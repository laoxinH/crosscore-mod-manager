package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.domain.service.AppInfoService
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.ui.state.SnackbarManager

/** 导航事件 */
sealed class NavigationEvent {
    /** 导航到设置页面 */
    object NavigateToSettings : NavigationEvent()
    /** 导航到控制台页面 */
    object NavigateToConsole : NavigationEvent()
    /** 导航到MOD页面 */
    object NavigateToMod : NavigationEvent()
}

/** 主 ViewModel，用于持有全局共享的组件 如 SnackbarManager 和导航事件 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val snackbarManager: SnackbarManager,
    private val appInfoService: AppInfoService,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Navigation Events via SharedFlow
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Bottom Bar Visibility
    var isBottomBarVisible = mutableStateOf(true)
        private set

    // Game Icon State
    val gameIcon: StateFlow<ImageBitmap?> =
        userPreferencesRepository.selectedGame.map { gameInfo ->
                withContext(Dispatchers.IO) {
                    appInfoService.getAppIcon(gameInfo.packageName)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun setBottomBarVisibility(visible: Boolean) {
        isBottomBarVisible.value = visible
    }

    fun navigateToSettings() {
        viewModelScope.launch { _navigationEvent.emit(NavigationEvent.NavigateToSettings) }
    }

    fun navigateToConsole() {
        //Log.d("MainViewModel", "Navigating to Console")
        viewModelScope.launch { _navigationEvent.emit(NavigationEvent.NavigateToConsole) }
    }

    fun navigateToMod() {
        viewModelScope.launch { _navigationEvent.emit(NavigationEvent.NavigateToMod) }
    }
}
