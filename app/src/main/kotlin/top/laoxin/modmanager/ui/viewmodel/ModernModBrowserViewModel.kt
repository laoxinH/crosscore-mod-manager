package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.usercase.file.GetPathFilesUseCase
import top.laoxin.modmanager.domain.usercase.mod.GetGameAllModsUserCase
import top.laoxin.modmanager.ui.state.ModBrowserUiState
import java.io.File
import javax.inject.Inject

/** Modern 文件浏览器ViewModel Refactored to support Stateless Navigation (Predictive Back) */
@HiltViewModel
class ModernModBrowserViewModel
@Inject
constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    getGameAllModsUserCase: GetGameAllModsUserCase,
    private val getPathFilesUseCase: GetPathFilesUseCase
) : ViewModel() {

    companion object {
        const val TAG = "ModernModBrowserViewModel"
    }

    private val _uiState = MutableStateFlow(ModBrowserUiState())

    // UI State now only holds global preference/data, not navigation state
    val uiState =
            combine(
                _uiState,
                getGameAllModsUserCase(),
                userPreferencesRepository.selectedGame,
                userPreferencesRepository.modListDisplayMode
            ) { uiState, allMods, gameInfo, displayMode ->
                ModBrowserUiState(
                    currentGameModPath =
                        PathConstants.getFullModPath(gameInfo.modSavePath),
                    allMods = allMods,
                    showCategory = uiState.showCategory,
                    isGridView = displayMode == 1,
                    currentBrowsingPath = uiState.currentBrowsingPath
                        ?: PathConstants.getFullModPath(gameInfo.modSavePath),
                    currentMods = uiState.currentMods
                )
            }
                    .stateIn(
                        viewModelScope,
                            SharingStarted.Companion.WhileSubscribed(5000),
                        ModBrowserUiState()
                    )

    fun setCurrentBrowsingPath(path: String) {
        _uiState.update { it.copy(currentBrowsingPath = path) }
        val allMods = _uiState.value.allMods
        val currentPathModsByPath = uiState.value.allMods.filter { mod ->
            File(mod.path).parent == path.trimEnd('/')
        }.filterOnlyUniqueBy { it.path }
        val currentPathModsByVirtualPath = uiState.value.allMods.filter { File(
            it.virtualPaths ?: ""
        ).parent == path.trimEnd('/') }
        val currentPathMods = currentPathModsByPath.ifEmpty { currentPathModsByVirtualPath }
        setCurrentMods(currentPathMods)
    }

    /** 切换列表/网格显示模式 */
    fun toggleDisplayMode() {
        viewModelScope.launch {
            val currentMode = uiState.value.isGridView
            val newMode = if (currentMode) 0 else 1
            userPreferencesRepository.saveModListDisplayMode(newMode)
        }
    }

    /** Get files for a specific path. Returns a Flow to be collected by the UI. */
    fun getFilesFlow(path: String, searchQuery: String? = null): Flow<List<File>> {
        return getPathFilesUseCase(path, searchQuery)
    }

    /** 通过路径严格匹配获取mods */
    fun getModsByPathStrict(path: String, allMods: List<ModBean>): List<ModBean> {
        return allMods.filter { it.path == path }
    }

    /** 通过路径获取mods（包含子路径） */
    fun getModsByPath(path: String, allMods: List<ModBean>): List<ModBean> {
        return allMods.filter { it.path?.contains(path + File.separator) == true }
    }

    /** 通过虚拟路径严格匹配获取mods */
    fun getModsByVirtualPathsStrict(path: String, allMods: List<ModBean>): List<ModBean> {
        return allMods.filter { it.virtualPaths == path }
    }

    /** 通过虚拟路径获取mods（包含子路径） */
    fun getModsByVirtualPaths(path: String, allMods: List<ModBean>): List<ModBean> {
        return allMods.filter { it.virtualPaths?.contains(path + File.separator) == true }
    }
    // Scroll State Cache (Not in UiState to avoid unnecessary recomposition loop)
    private val _scrollStates = mutableMapOf<String, Pair<Int, Int>>()

    fun saveScrollState(path: String, index: Int, offset: Int) {
        _scrollStates[path] = index to offset
    }

    fun getScrollState(path: String): Pair<Int, Int> {
        return _scrollStates[path] ?: (0 to 0)
    }

    fun setCurrentMods(mods: List<ModBean>) {
        Log.d(TAG, "setCurrentMods: ${mods.size}")
        _uiState.update { it.copy(currentMods = mods) }
    }

    fun <T, K> List<T>.filterOnlyUniqueBy(selector: (T) -> K): List<T> {
        val counts = this.groupingBy(selector).eachCount()
        return this.filter { counts[selector(it)] == 1 }
    }
}