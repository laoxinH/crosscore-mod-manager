package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.usercase.mod.GetGameAllModsUserCase
import top.laoxin.modmanager.ui.state.ModListFilter
import top.laoxin.modmanager.ui.state.ModListUiState

sealed class ModNavigationEvent {
    data object NavigateBack : ModNavigationEvent()
    data class NavigateToBrowser(val path: String? = null) :
            ModNavigationEvent() // Optional, if we want TopBar to trigger browser
    data object NavigateToList : ModNavigationEvent() // Optional
}

/** Modern Mod列表状态管理ViewModel 仅关注列表数据和多选状态，移除与Tab切换耦合的逻辑 */
@HiltViewModel
class ModernModListViewModel @Inject constructor(
    getGameAllModsUserCase: GetGameAllModsUserCase,
    private val userPreferencesRepository: UserPreferencesRepository
) :
        ViewModel() {

    companion object {
        const val TAG = "ModernModListViewModel"
    }

    private val _uiState = MutableStateFlow(ModListUiState())

    // Navigation Events
    private val _navigationEvent = Channel<ModNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    // 将来自UseCase的数据流和内部UI状态流合并为单一的UiState
    val uiState: StateFlow<ModListUiState> =
            combine(
                            getGameAllModsUserCase(), // 从UseCase获取的响应式Mod列表
                            _uiState,
                            userPreferencesRepository.showCategoryView
                    ) { allMods, uiState, showCategoryView ->
                        // Sort by updateAt descending
                        val sortedMods = allMods.sortedByDescending { it.date }
                        ModListUiState(
                                modList = sortedMods,
                                enableModList = sortedMods.filter { it.isEnable },
                                disableModList = sortedMods.filter { !it.isEnable },
                                isMultiSelect = uiState.isMultiSelect,
                                modsSelected = uiState.modsSelected,
                                isLoading = false,
                                modSwitchEnable = uiState.modSwitchEnable,
                                filter = uiState.filter,
                                isBrowser = showCategoryView
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = ModListUiState(isLoading = true)
                    )

    /** 设置过滤模式 */
    fun setFilter(filter: ModListFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    /** Mod长按事件（进入多选模式） */
    fun modLongClick(modBean: ModBean) {
        if (!_uiState.value.isMultiSelect) {
            _uiState.update { it.copy(isMultiSelect = true) }
            _uiState.update { it.copy(modsSelected = it.modsSelected + modBean.id) }
        }
    }

    /** Mod多选点击事件 */
    fun modMultiSelectClick(modBean: ModBean) {
        _uiState.update { uiState ->
            if (uiState.modsSelected.contains(modBean.id)) {
                uiState.copy(modsSelected = uiState.modsSelected - modBean.id)
            } else {
                uiState.copy(modsSelected = uiState.modsSelected + modBean.id)
            }
        }
    }

    /** 退出多选模式 */
    fun exitSelect() {
        _uiState.update { it.copy(isMultiSelect = false) }
        _uiState.update { it.copy(modsSelected = emptySet()) }
    }

    /** 全选 */
    fun allSelect(mods: List<ModBean>) {
        val allIds = mods.map { it.id }.toSet()
        _uiState.update { it.copy(modsSelected = allIds) }
    }

    /** 取消选择 */
    fun deselect() {
        _uiState.update { it.copy(modsSelected = emptySet()) }
    }

    /** 切换开关状态 */
    fun setModSwitchEnable(bool: Boolean) {
        _uiState.update { it.copy(modSwitchEnable = bool) }
    }

    /** 获取需要切换的Mod列表, 用于切换开关状态 */
    fun getSelectableModsForSwitch(bool: Boolean): List<ModBean?> {
        val ids = _uiState.value.modsSelected
        return ids.map { id ->
            if (bool) {
                uiState.value.modList.find { it.id == id && !it.isEnable }
            } else {
                uiState.value.modList.find { it.id == id && it.isEnable }
            }
        }
    }

    fun removeModSelection(id: Int) {
        _uiState.update { it.copy(modsSelected = it.modsSelected - id) }
    }

    fun setIsBrowser(isBrowser: Boolean) {
        // _uiState.update { it.copy(isBrowser = isBrowser) }
         viewModelScope.launch {
            userPreferencesRepository.saveShowCategoryView(isBrowser)
        }
    }

    fun onBackClick(isRoot : Boolean = true) {
      //  Log.d(TAG, "返回函数触发了: hhh")
        if (_uiState.value.isMultiSelect) {
            exitSelect()
        } else if (!isRoot) {
            _navigationEvent.trySend(ModNavigationEvent.NavigateBack)
        } else{
            return
        }
    }

    fun onNavigateToBrowser(path: String? = null) {
        Log.d(TAG, "onNavigateToBrowser called with path: $path")
        _navigationEvent.trySend(ModNavigationEvent.NavigateToBrowser(path))
    }


    fun onNavigateToList() {
        _navigationEvent.trySend(ModNavigationEvent.NavigateToList)
    }

    // Scroll State Cache
    private val _scrollStates = mutableMapOf<String, Pair<Int, Int>>()

    fun saveScrollState(key: String, index: Int, offset: Int) {
        _scrollStates[key] = index to offset
    }

    fun getScrollState(key: String): Pair<Int, Int> {
        return _scrollStates[key] ?: (0 to 0)
    }
}
