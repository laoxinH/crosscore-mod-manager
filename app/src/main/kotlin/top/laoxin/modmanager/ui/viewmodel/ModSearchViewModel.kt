package top.laoxin.modmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.domain.usercase.mod.SearchModsUserCase
import top.laoxin.modmanager.ui.state.ModSearchUiState
import javax.inject.Inject

/**
 * 搜索功能ViewModel
 */
@HiltViewModel
class ModSearchViewModel @Inject constructor(
    private val searchModsUserCase: SearchModsUserCase
) : ViewModel() {

    companion object {
        const val TAG = "ModSearchViewModel"
    }

    private val _uiState = MutableStateFlow(ModSearchUiState())
    val uiState: StateFlow<ModSearchUiState> = _uiState.asStateFlow()

    // 内部查询流，用于防抖
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotEmpty()) {
                        performSearch(query)
                    } else {
                        _uiState.update { it.copy(searchModList = emptyList(), isSearching = false) }
                    }
                }
        }
    }

    /**
     * 设置搜索框可见性
     */
    fun setSearchBoxVisible(visible: Boolean) {
        _uiState.update { it.copy(searchBoxVisible = visible) }
        if (!visible) {
            clearSearch()
        }
    }

    /**
     * 设置搜索内容
     */
    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchContent = text) }
        _searchQuery.value = text // 触发搜索流
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchContent = "", searchModList = emptyList(), isSearching = false) }
        _searchQuery.value = ""
    }

    /**
     * 执行搜索
     */
    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        try {
            searchModsUserCase(query).collectLatest { mods ->
                _uiState.update { it.copy(searchModList = mods) }
            }
        } finally {
            _uiState.update { it.copy(isSearching = false) }
        }
    }
}
