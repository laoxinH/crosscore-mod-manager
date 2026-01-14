package top.laoxin.modmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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
class MainViewModel @Inject constructor(val snackbarManager: SnackbarManager) : ViewModel() {

    // 导航事件流（一次性事件，使用 SharedFlow）
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    /** 触发导航到设置页面 */
    fun navigateToSettings() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToSettings)
        }
    }

    /** 触发导航到控制台页面 */
    fun navigateToConsole() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToConsole)
        }
    }

    /** 触发导航到MOD页面 */
    fun navigateToMod() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToMod)
        }
    }
}
