package top.laoxin.modmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import top.laoxin.modmanager.domain.bean.ModBean

import top.laoxin.modmanager.domain.usercase.mod.GetGameAllModsUserCase
import top.laoxin.modmanager.ui.state.ModListUiState
import javax.inject.Inject

/**
 * Mod列表状态管理ViewModel
 */
@HiltViewModel
class ModListViewModel @Inject constructor(
    getGameAllModsUserCase: GetGameAllModsUserCase
) : ViewModel() {

    companion object {
        const val TAG = "ModListViewModel"
    }

    // 内部状态“开关”，用于处理多选模式
    private val _isMultiSelect = MutableStateFlow(false)
    private val _modsSelected = MutableStateFlow<Set<Int>>(emptySet())
    private val _modSwitchEnable = MutableStateFlow(true)


    // 将来自UseCase的数据流和内部UI状态流合并为单一的UiState
    val uiState: StateFlow<ModListUiState> = combine(
        getGameAllModsUserCase(), // 从UseCase获取的响应式Mod列表
        _isMultiSelect,
        _modsSelected,
        _modSwitchEnable
    ) { allMods, isMultiSelect, selectedIds, modSwitchEnable ->
        ModListUiState(
            modList = allMods,
            enableModList = allMods.filter { it.isEnable },
            disableModList = allMods.filter { !it.isEnable },
            isMultiSelect = isMultiSelect,
            modsSelected = selectedIds,
            isLoading = false, // 当数据流到达时，加载完成
            modSwitchEnable = modSwitchEnable
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ModListUiState(isLoading = true) // 初始状态为加载中
    )

    /**
     * Mod长按事件（进入多选模式）
     */
    fun modLongClick(modBean: ModBean) {
        if (!_isMultiSelect.value) {
            _isMultiSelect.value = true
            _modsSelected.update { it + modBean.id }
        }
    }

    /**
     * Mod多选点击事件
     */
    fun modMultiSelectClick(modBean: ModBean) {
        _modsSelected.update { currentSelected ->
            if (currentSelected.contains(modBean.id)) {
                currentSelected - modBean.id
            } else {
                currentSelected + modBean.id
            }
        }
    }

    /**
     * 退出多选模式
     */
    fun exitSelect() {
        _isMultiSelect.value = false
        _modsSelected.value = emptySet()
    }

    /**
     * 全选
     */
    fun allSelect(mods: List<ModBean>) {
        // 从当前的UiState中获取完整的列表来进行全选
        val allIds = mods.map { it.id }.toSet()
        _modsSelected.value = allIds
    }

    /**
     * 取消选择
     */
    fun deselect() {
        _modsSelected.value = emptySet()
    }

    /**
     * 切换开关状态
     */


    fun setModSwitchEnable(bool: Boolean) {
        _modSwitchEnable.value = bool
    }

    /**
     * 获取需要切换的Mod列表, 用于切换开关状态
     */
    fun getSelectableModsForSwitch(bool: Boolean): List<ModBean?> {
        val ids = _modsSelected.value
        return ids.map { id ->
            if (bool) {
                uiState.value.modList.find { it.id == id && !it.isEnable }
            } else {
                uiState.value.modList.find { it.id == id && it.isEnable }
            }
        }
    }

    fun removeModSelection(id: Int) {
        _modsSelected.update { currentSelected ->

            currentSelected - id

        }
    }
}
