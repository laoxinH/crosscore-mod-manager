package top.laoxin.modmanager.ui.viewmodel

// import top.laoxin.modmanager.tools.ArchiveUtil
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.usercase.mod.GetGameAllModsUserCase
import top.laoxin.modmanager.ui.state.ModBrowserUiState
import top.laoxin.modmanager.ui.view.modView.NavigationIndex

/** 文件浏览器ViewModel */
@HiltViewModel
class ModBrowserViewModel
@Inject
constructor(
        // private val gameInfoManager: GameInfoManager,
        private val userPreferencesRepository: UserPreferencesRepository,
        getGameAllModsUserCase: GetGameAllModsUserCase,
        private val archiveService: ArchiveService
) : ViewModel() {

    companion object {
        const val TAG = "ModBrowserViewModel"
    }
    /*private val userPreferencesState: StateFlow<UserPreferencesState> =
    combine(
        userPreferencesRepository.selectedGame,
        userPreferencesRepository.scanQQDirectory,
        userPreferencesRepository.selectedDirectory,
        userPreferencesRepository.scanDownload,
        combine(
            userPreferencesRepository.scanDirectoryMods,
            userPreferencesRepository.deleteUnzipDirectory,
            userPreferencesRepository.showCategoryView
        ) { scanMods, delUnzip, category ->
            Triple(scanMods, delUnzip, category)
        }
    ) { game, qq, dir, download, lastThree ->
        UserPreferencesState(
            selectedGame = game,
            scanQQDirectory = qq,
            selectedDirectory = dir,
            scanDownload = download,
            scanDirectoryMods = lastThree.first,
            delUnzipDictionary = lastThree.second,
            showCategoryView = lastThree.third
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesState()
        )*/

    private val _uiState = MutableStateFlow(ModBrowserUiState())
    // val uiState: StateFlow<ModBrowserUiState> = _uiState.asStateFlow()

    val uiState =
            combine(
                            _uiState,
                            getGameAllModsUserCase(),
                            userPreferencesRepository.selectedGame,
                userPreferencesRepository.modsViewIndex,
                userPreferencesRepository.modListDisplayMode
                    ) { uiState, allMods, gameInfo, modsViewIndex, displayMode ->
                        ModBrowserUiState(
                                allMods = allMods,
                                modsView = NavigationIndex.entries.find { it.index == modsViewIndex }
                                    ?: NavigationIndex.MODS_BROWSER,
                                showCategory = uiState.showCategory,
                                currentGameModPath =
                                        PathConstants.getFullModPath(gameInfo.modSavePath),
                                currentPath = uiState.currentPath,
                                currentFiles = uiState.currentFiles,
                                currentMods = uiState.currentMods,
                                isBackPathExist = uiState.isBackPathExist,
                                doBackFunction = uiState.doBackFunction,
                                isGridView = displayMode == 1
                        )
                    }
                    .stateIn(
                            viewModelScope,
                            SharingStarted.WhileSubscribed(5000),
                            ModBrowserUiState()
                    )

    init {


        viewModelScope.launch {
            userPreferencesRepository.selectedGame.collectLatest { gameInfoBean ->
                // Log.d(TAG, "init: ${gameInfoBean.modSavePath}")
                _uiState.update {
                    it.copy(currentPath = PathConstants.getFullModPath(gameInfoBean.modSavePath))
                }
            }
        }


    }

    // 当前浏览的压缩包path
    var currentZipPath = ""

    // 当前研所包的文件列表
    var currentArchiveFiles = emptyList<File>()

    /** 获取当前游戏目录 */
    // fun getCurrentGameModPath(): String {}
    /** 设置Mods视图类型 */
    fun setModsView(view: NavigationIndex) {
        _uiState.update { it.copy(modsView = view) }
        // 保存到用户偏好
        viewModelScope.launch { userPreferencesRepository.saveModsViewIndex(view.index) }
    }
    
    /** 切换列表/网格显示模式 */
    fun toggleDisplayMode() {
        viewModelScope.launch {
            val currentMode = userPreferencesRepository.modListDisplayMode.first()
            val newMode = if (currentMode == 0) 1 else 0
            userPreferencesRepository.saveModListDisplayMode(newMode)
        }
    }

    /** 设置当前游戏Mod路径 */
    fun setCurrentPath(path: String) {
        _uiState.update { it.copy(currentPath = path) }
    }

    /** 设置返回按钮可见性 */
    fun setBackIconVisible(visible: Boolean) {
        Log.d(TAG, "设置点击返回按钮可见性: $visible")
        _uiState.update { it.copy(isBackPathExist = visible) }
    }

    /** 设置执行返回操作 */
    fun setDoBackFunction(doBack: Boolean) {
        _uiState.update { it.copy(doBackFunction = doBack) }
    }

    /** 更新文件列表 */
    fun updateFiles(currentPath: String, searchQuery: String? = null) {
        _uiState.update { it.copy(currentPath = currentPath) }

        viewModelScope.launch {
            var filesToShow =
                    if (File(currentPath).isDirectory) {
                        // 普通目录
                        File(currentPath)
                                .listFiles()
                                ?.toList()
                                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                                ?: emptyList()
                    } else {
                        // 压缩包
                        if (currentZipPath.isEmpty() || !currentPath.contains(currentZipPath)) {
                            currentZipPath = currentPath
                            getArchiveFiles(currentPath)
                        }
                        currentArchiveFiles.filter {
                            Log.d(TAG, "updateFiles: ${it.parent}--$currentPath")
                            it.parent == currentPath
                        }
                    }

            if (!searchQuery.isNullOrEmpty()) {
                filesToShow =
                        filesToShow.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
            }
            Log.d(TAG, "updateFiles----------: $filesToShow")

            _uiState.update { it.copy(currentFiles = filesToShow) }
        }
    }

    /** 获取压缩包文件列表 */
    private suspend fun getArchiveFiles(path: String): List<File> {

        // 读取压缩包文件
        archiveService
                .listFiles(path)
                .onSuccess { fileHeaders ->
                    // 拼接路径
                    val files = fileHeaders.map { File("$path/$it") }
                    // Log.d(TAG, "getArchiveFiles: $files")
                    currentArchiveFiles = files
                    return files
                }
                .onError {}

        return emptyList()
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

    /** 设置当前Mods */
    fun setCurrentMods(mods: List<ModBean>) {
        // Log.d(TAG, "setCurrentMods: ${mods.size}")
        _uiState.update { it.copy(currentMods = mods) }
    }
}
