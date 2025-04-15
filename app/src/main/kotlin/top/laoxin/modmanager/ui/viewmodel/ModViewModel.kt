package top.laoxin.modmanager.ui.viewmodel

import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.UserPreferencesKeys
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.domain.usercase.gameinfo.UpdateGameInfoUserCase
import top.laoxin.modmanager.domain.usercase.mod.CheckCanFlashModsUserCase
import top.laoxin.modmanager.domain.usercase.mod.CheckCanSwitchModsUserCase
import top.laoxin.modmanager.domain.usercase.mod.CheckModPasswordUserCase
import top.laoxin.modmanager.domain.usercase.mod.DeleteModUserCase
import top.laoxin.modmanager.domain.usercase.mod.DeleteSelectedModUserCase
import top.laoxin.modmanager.domain.usercase.mod.DisableModsUserCase
import top.laoxin.modmanager.domain.usercase.mod.EnableModsUserCase
import top.laoxin.modmanager.domain.usercase.mod.FlashModDetailUserCase
import top.laoxin.modmanager.domain.usercase.mod.FlashModImageUserCase
import top.laoxin.modmanager.domain.usercase.mod.FlashModsUserCase
import top.laoxin.modmanager.domain.usercase.repository.DeleteModsUserCase
import top.laoxin.modmanager.domain.usercase.repository.GetGameAllModsUserCase
import top.laoxin.modmanager.domain.usercase.repository.GetGameDisEnableUserCase
import top.laoxin.modmanager.domain.usercase.repository.GetGameEnableModsUserCase
import top.laoxin.modmanager.domain.usercase.repository.SearchModsUserCase
import top.laoxin.modmanager.domain.usercase.repository.UpdateModUserCase
import top.laoxin.modmanager.domain.usercase.userpreference.GetUserPreferenceUseCase
import top.laoxin.modmanager.listener.ProgressUpdateListener
import top.laoxin.modmanager.observer.FlashModsObserver
import top.laoxin.modmanager.observer.FlashModsObserverLow
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.observer.FlashObserverInterface
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.BaseSpecialGameTools
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.state.UserPreferencesState
import top.laoxin.modmanager.ui.view.modView.NavigationIndex
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ModViewModel @Inject constructor(
    private val getUserPreferenceUseCase: GetUserPreferenceUseCase,
    private val getGameEnableModsUserCase: GetGameEnableModsUserCase,
    private val getGameAllModsUserCase: GetGameAllModsUserCase,
    private val getGameDisEnableUserCase: GetGameDisEnableUserCase,
    private val checkCanFlashModsUserCase: CheckCanFlashModsUserCase,
    private val flashModsUserCase: FlashModsUserCase,
    private val deleteModsUserCase: DeleteModsUserCase,
    private val checkCanSwitchModsUserCase: CheckCanSwitchModsUserCase,
    private val enableModsUserCase: EnableModsUserCase,
    private val disableModsUserCase: DisableModsUserCase,
    private val checkModPasswordUserCase: CheckModPasswordUserCase,
    private val flashModImageUserCase: FlashModImageUserCase,
    private val flashModDetailUserCase: FlashModDetailUserCase,
    private val searchModsUserCase: SearchModsUserCase,
    private val deleteSelectedModUserCase: DeleteSelectedModUserCase,
    private val deleteModUserCase: DeleteModUserCase,
    private val updateModUserCase: UpdateModUserCase,
    private val updateGameInfoUserCase: UpdateGameInfoUserCase,
    private val flashModsObserverManager: FlashModsObserverManager,
    private val gameInfoManager: GameInfoManager,
    private val appPathsManager: AppPathsManager,
    private val permissionTools: PermissionTools,
    private val fileToolsManager: FileToolsManager,
) : ViewModel(), ProgressUpdateListener, FlashObserverInterface {
    private val _uiState = MutableStateFlow(ModUiState())

    private var _requestPermissionPath by mutableStateOf("")
    val requestPermissionPath: String
        get() = _requestPermissionPath

    private var _gameInfo = GameInfoConstant.gameInfoList[0]

    // 删除的mods
    private var delModsList = emptyList<ModBean>()

    // 当前浏览的压缩包path
    private var _currentZipPath by mutableStateOf("")

    // 当前的虚拟路径
    private var _currentPath by mutableStateOf("")

    // 当前研所包的文件列表
    private var _currentFiles by mutableStateOf(emptyList<File>())

    // 扫描mod目录列表
    // 从用户配置中读取mod目录
    companion object {
        var searchJob: Job? = null
        var enableJob: Job? = null
        var updateAllModsJob: Job? = null
        var updateEnableModsJob: Job? = null
        var updateDisEnableModsJob: Job? = null
        var checkPasswordJob: Job? = null
        var fileObserver: FileObserver? = null
        var flashModsJob: Job? = null
    }

    private val selectedGameFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.SELECTED_GAME, 0)

    private val scanQQDirectoryFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.SCAN_QQ_DIRECTORY, false)

    private val selectedDirectoryFlow =
        getUserPreferenceUseCase(
            UserPreferencesKeys.SELECTED_DIRECTORY,
            appPathsManager.getDownloadModPath()
        )

    private val scanDownloadFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.SCAN_DOWNLOAD, false)

    private val openPermissionRequestDialogFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.OPEN_PERMISSION_REQUEST_DIALOG, false)

    private val scanDirectoryModsFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.SCAN_DIRECTORY_MODS, true)

    private val delUnzipDictionaryFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.DELETE_UNZIP_DIRECTORY, false)

    private val showCategoryViewFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.SHOW_CATEGORY_VIEW, true)

    private val userTipsFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.USER_TIPS, true)

    // 生成用户配置对象
    private val userPreferences: StateFlow<UserPreferencesState> = combine(
        scanQQDirectoryFlow,
        selectedDirectoryFlow,
        scanDownloadFlow,
        selectedGameFlow,
        scanDirectoryModsFlow,
        delUnzipDictionaryFlow,
        showCategoryViewFlow
    ) { values: Array<Any> ->
        UserPreferencesState(
            scanQQDirectory = values[0] as Boolean,
            selectedDirectory = values[1] as String,
            scanDownload = values[2] as Boolean,
            selectedGameIndex = values[3] as Int,
            scanDirectoryMods = values[4] as Boolean,
            delUnzipDictionary = values[5] as Boolean,
            showCategoryView = values[6] as Boolean
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState()
    )

    val uiState: StateFlow<ModUiState> = combine(
        userTipsFlow, _uiState
    ) { userTips, uiState ->
        uiState.copy(showUserTipsDialog = userTips)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ModUiState()
    )

    init {
        Log.d("ModViewModel", "init: 初始化${userPreferences.value}")
        //checkPermission()
        viewModelScope.launch {
            userPreferences.collectLatest { it ->
                updateGameInfo(it)
                updateAllMods()
                updateEnableMods()
                updateDisEnableMods()
                updateProgressUpdateListener()
                startFileObserver(it)
                // 设置当前游戏mod目录
                updateCurrentGameModPath(it)
                // 设置当前的视图
                switchCurrentModsView(it)
            }
        }
    }

    // 更新游戏信息
    private suspend fun updateGameInfo(userPreferencesState: UserPreferencesState) {
        val gameInfo = updateGameInfoUserCase(
            userPreferencesState.selectedGameIndex,
            appPathsManager.getRootPath() + userPreferencesState.selectedDirectory
        )
        gameInfoManager.setGameInfo(gameInfo)
    }

    private fun switchCurrentModsView(it: UserPreferencesState) {
        if (it.showCategoryView) {
            _uiState.update {
                it.copy(modsView = NavigationIndex.MODS_BROWSER)
            }
        } else {
            _uiState.update {
                it.copy(modsView = NavigationIndex.ALL_MODS)
            }
        }
    }

    // 更新当前游戏mod目录
    private fun updateCurrentGameModPath(userPreferences: UserPreferencesState) {
        val gameInfo = gameInfoManager.getGameInfo()
        _uiState.update {
            it.copy(
                currentGameModPath = appPathsManager.getRootPath() +
                        userPreferences.selectedDirectory + gameInfo.packageName,
                currentPath = appPathsManager.getRootPath() +
                        userPreferences.selectedDirectory + gameInfo.packageName
            )
        }
    }

    private fun updateProgressUpdateListener() {
        ArchiveUtil.progressUpdateListener = this
        BaseSpecialGameTools.progressUpdateListener = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FlashModsObserver.flashObserver = this
        } else {
            FlashModsObserverLow.flashObserver = this
        }
    }


    // 更新所有mods
    private fun updateAllMods() {
        updateAllModsJob?.cancel()
        Log.d("ModViewModel", "gameinfo : ${gameInfoManager.getGameInfo()}")
        updateAllModsJob = viewModelScope.launch {
            getGameAllModsUserCase().collectLatest { mods ->
                _uiState.update {
                    it.copy(modList = mods)
                }
            }
        }
    }

    // 更新已开启mods
    private fun updateEnableMods() {
        updateEnableModsJob?.cancel()
        updateEnableModsJob = viewModelScope.launch {
            getGameEnableModsUserCase().collectLatest { mods ->
                _uiState.update {
                    it.copy(enableModList = mods)
                }
            }
        }
    }


    private fun updateDisEnableMods() {
        updateDisEnableModsJob?.cancel()
        updateDisEnableModsJob = viewModelScope.launch {
            getGameDisEnableUserCase().collectLatest { mods ->
                _uiState.update {
                    it.copy(disableModList = mods)
                }
            }
        }
    }

    // 启动文件夹监听
    private fun startFileObserver(userPreferencesState: UserPreferencesState) {
        flashModsObserverManager.openModDictionaryObserver(userPreferencesState.selectedDirectory + gameInfoManager.getGameInfo().packageName)
    }

    fun flashMods(isLoading: Boolean, forceScan: Boolean) {
        flashModsJob?.cancel()
        val result = checkCanFlashModsUserCase()
        when (result.first) {
            ResultCode.NO_SELECTED_GAME -> {
                ToastUtils.longCall(R.string.toast_please_select_game)
            }

            ResultCode.NO_PERMISSION -> {
                setRequestPermissionPath(permissionTools.getRequestPermissionPath(result.second))
                setOpenPermissionRequestDialog(true)
            }

            ResultCode.SUCCESS -> {
                ToastUtils.longCall(R.string.toast_start_scan)
                flashModsJob = viewModelScope.launch {
                    setLoading(isLoading)
                    val flashResult = flashModsUserCase(
                        userPreferencesState = userPreferences.value,
                        mods = _uiState.value.modList,
                        setLoadingPath = { setLoadingPath(it) },
                        forceScan = forceScan
                    )
                    when (flashResult.code) {
                        ResultCode.SUCCESS -> {
                            if (flashResult.delEnableMods.isNotEmpty()) {
                                _uiState.update {
                                    it.copy(delEnableModsList = flashResult.delEnableMods)
                                }
                                delModsList = flashResult.delEnableMods
                                setShowDisEnableModsDialog(true)
                            } else {
                                deleteModsUserCase(flashResult.delMods)
                            }

                            ToastUtils.longCall(
                                App.get().getString(
                                    R.string.toast_flash_complate,
                                    flashResult.newMods.size.toString(),
                                    flashResult.updateMods.size.toString(),
                                    flashResult.delMods.size.toString()
                                )
                            )
                        }

                        ResultCode.FAIL -> {
                            ToastUtils.longCall(R.string.toast_flash_failed)
                        }
                    }
                    setLoading(false)
                }

            }
        }

    }


    // 打开mod详情
    fun openModDetail(mod: ModBean, showModDetail: Boolean) {
        setModDetail(mod)
        setShowModDetail(showModDetail)
    }

    // 开启或关闭mod
    fun switchMod(modBean: ModBean, b: Boolean, isDel: Boolean = false) {
        val checkCanSwitchModsResult = checkCanSwitchModsUserCase()
        when (checkCanSwitchModsResult.first) {
            ResultCode.NO_PERMISSION -> {
                setRequestPermissionPath(
                    permissionTools.getRequestPermissionPath(
                        checkCanSwitchModsResult.second
                    )
                )
                setOpenPermissionRequestDialog(true)
            }

            ResultCode.SUCCESS -> {
                setUnzipProgress("")
                setMultitaskingProgress("")
                viewModelScope.launch {
                    if (b) {
                        enableMod(listOf(modBean))
                    } else {
                        disableMod(listOf(modBean), isDel)
                    }
                }

            }
        }
    }

    // 打开密码输入框
    fun showPasswordDialog(b: Boolean) {
        _uiState.value = _uiState.value.copy(showPasswordDialog = b)
    }

    // 刷新mod预览图
    fun flashModImage(modBean: ModBean) {

        viewModelScope.launch(Dispatchers.IO) {
            flashModImageUserCase(modBean)
        }
    }

    // 刷新readme文件
    private suspend fun flashModDetail(mods: ModBean): ModBean {

        val result = flashModDetailUserCase(mods)
        when (result.code) {
            ResultCode.MOD_NEED_PASSWORD -> {
                setModDetail(result.mod)
                showPasswordDialog(true)
            }
        }
        return result.mod
    }


    fun updateMod(mod: ModBean) {
        viewModelScope.launch {
            updateModUserCase(mod)
        }
    }


    // 开启mod
    private fun enableMod(mods: List<ModBean>) {
        enableJob?.cancel()
        Log.d("ModViewModel", "enableMod: 开始执行开启 : $mods")
        enableJob = viewModelScope.launch {
            setModSwitchEnable(false)
            setTipsText(App.get().getString(R.string.tips_open_mod))
            setShowTips(true)
            val result = enableModsUserCase(
                mods,
                delUnzipDictionary = userPreferences.value.delUnzipDictionary,
                setMultitaskingProgress = { setMultitaskingProgress(it) },
                setUnzipProgress = { setUnzipProgress(it) },
                setTipsText = { setTipsText(it) },
            )

            when (result.code) {
                ResultCode.MOD_NEED_PASSWORD -> {
                    setModDetail(result.passwordMod)
                    showPasswordDialog(true)
                }

                ResultCode.SUCCESS -> {
                    if (result.failMods.isNotEmpty()) {
                        setEnableFailedMods(result.failMods)
                        setShowOpenFailedDialog(true)
                    }
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_swtch_mods_result,
                            result.successMods.size.toString(),
                            result.failMods.size.toString()
                        )
                    )
                }
            }
            setShowTips(false)
            setModSwitchEnable(true)
        }

    }

    fun setShowOpenFailedDialog(b: Boolean) {
        _uiState.update {
            it.copy(showOpenFailedDialog = b)
        }
    }

    // 关闭mod
    fun disableMod(mods: List<ModBean>, isDel: Boolean = false) {
        enableJob?.cancel()
        enableJob = viewModelScope.launch {
            setModSwitchEnable(false)
            setTipsText(App.get().getString(R.string.tips_close_mod))
            setShowTips(true)
            val result = disableModsUserCase(
                mods,
                isDel,
                ::removeDelEnableModsList,
                ::setMultitaskingProgress,
                ::setUnzipProgress,
                ::setTipsText,
            )
            when (result.code) {

                ResultCode.SUCCESS -> {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_swtch_mods_result,
                            result.successMods.size.toString(),
                            result.failMods.size.toString()
                        )
                    )
                }
            }

            setShowTips(false)
            setModSwitchEnable(true)
        }
    }

    // 移除删除列表
    fun removeDelEnableModsList(mod: ModBean) {
        _uiState.update {
            it.copy(delEnableModsList = it.delEnableModsList.filter { it.path != mod.path })
        }
    }

    fun checkPassword(password: String) {
        checkPasswordJob?.cancel()
        checkPasswordJob = viewModelScope.launch {
            setModSwitchEnable(false)
            setShowTips(true)

            // 获取当前MOD详情
            val modDetail = _uiState.value.modDetail ?: return@launch

            try {
                val result = checkModPasswordUserCase(
                    modDetail,
                    password,
                    ::setTipsText
                )

                if (result.first == ResultCode.SUCCESS) {
                    // 密码验证成功，关闭密码对话框
                    showPasswordDialog(false)

                    // 更新本地MOD详情密码
                    val updatedMod = modDetail.copy(password = password)
                    setModDetail(updatedMod)

                    // 刷新MOD详情以显示新内容
                    refreshModDetail()
                }
            } finally {
                // 确保无论成功与否都会执行这些操作
                setShowTips(false)
                setModSwitchEnable(true)
            }
        }
    }

    fun setShowTips(b: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(showTips = b)
            }
        }
    }


    private fun setTipsText(s: String) {
        _uiState.value = _uiState.value.copy(tipsText = s)

    }

    private suspend fun setModSwitchEnable(b: Boolean) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(modSwitchEnable = b)
        }

    }

    // 设置请求权限文件夹
    private fun setRequestPermissionPath(path: String) {
        _requestPermissionPath = path
    }

    private fun setShowDisEnableModsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDisEnableModsDialog = b)
        }
    }

    fun confirmDeleteMods() {
        viewModelScope.launch {
            deleteModsUserCase(delModsList)
            setShowDisEnableModsDialog(false)
        }
    }

    // 设置搜索mods
    fun setSearchMods(modList: List<ModBean>) {
        _uiState.value = _uiState.value.copy(searchModList = modList)
    }

    fun setOpenPermissionRequestDialog(openPermissionRequestDialog: Boolean) {
        _uiState.value =
            _uiState.value.copy(openPermissionRequestDialog = openPermissionRequestDialog)
    }

    // 设置mod详情
    fun setModDetail(modDetail: ModBean) {
        _uiState.value = _uiState.value.copy(modDetail = modDetail)
    }

    // 设置是否打开搜索框
    fun setSearchBoxVisible(searchBoxVisible: Boolean) {
        _uiState.value = _uiState.value.copy(searchBoxVisible = searchBoxVisible)
    }

    // 设置搜索框内容
    fun setSearchText(searchText: String) {
        _uiState.update { it.copy(searchContent = searchText) }
        // 取消上一个搜索任务
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            when (_uiState.value.modsView) {
                NavigationIndex.MODS_BROWSER -> {
                    if (searchText.isEmpty()) {
                        updateFiles(_currentPath)
                    } else {
                        val searchFiles =
                            _uiState.value.currentFiles.filter { it.name.contains(searchText) }
                        _uiState.update { it.copy(currentFiles = searchFiles) }
                    }
                }

                else -> {
                    if (searchText.isEmpty()) {
                        setSearchMods(emptyList())
                    } else {
                        searchModsUserCase(searchText).collect {
                            setSearchMods(it)
                        }
                    }
                }
            }

        }
    }

    // 设置是否显示mod详情
    fun setShowModDetail(showModDetail: Boolean) {
        _uiState.value = _uiState.value.copy(showModDetail = showModDetail)
    }

    // 获取输入框内容
    fun getSearchText(): String {
        return _uiState.value.searchContent
    }


    // 设置正在加载
    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }

    // 设置加载目录
    private fun setLoadingPath(loadingPath: String) {
        _uiState.value = _uiState.value.copy(loadingPath = loadingPath)
    }


    // 设置解压进度
    private fun setUnzipProgress(progress: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(unzipProgress = progress)
        }

    }

    // 设置总进度
    private fun setMultitaskingProgress(progress: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(multitaskingProgress = progress)
        }

    }

    override fun onProgressUpdate(progress: String) {
        Log.d("ModViewModel", "onProgressUpdate: $progress")
        setUnzipProgress(progress)
    }

    override fun onFlash() {
        // 检测到文件变化，自动刷新mods
        Log.d("ModViewModel", "onFlash: 检测到文件变化，自动刷新mods")
        flashMods(true, false)
    }

    fun setModsView(enableMods: NavigationIndex) {
        _uiState.update {
            it.copy(modsView = enableMods)
        }
    }

    fun setBackIconVisible(b: Boolean) {
        _uiState.update {
            it.copy(isBackPathExist = b)
        }
    }

    fun setDoBackFunction(b: Boolean) {
        _uiState.update {
            it.copy(doBackFunction = b)
        }
    }

    fun modLongClick(modBean: ModBean) {
        Log.d("ModViewModel", "长按事件: $modBean")
        if (!_uiState.value.isMultiSelect) {
            val modsSelected = _uiState.value.modsSelected.toMutableList()
            modsSelected.add(modBean.id)
            _uiState.update {
                it.copy(isMultiSelect = true, modsSelected = modsSelected)
            }
        }
    }

    fun modMultiSelectClick(modBean: ModBean) {
        val modsSelected = _uiState.value.modsSelected.toMutableList()
        if (modsSelected.contains(modBean.id)) {
            modsSelected.remove(modBean.id)
        } else {
            modsSelected.add(modBean.id)
        }
        _uiState.update {
            it.copy(modsSelected = modsSelected)
        }
    }

    fun switchSelectMod(modList: List<ModBean>, b: Boolean) {
        if (_uiState.value.modsSelected.isEmpty()) return
        val modsSelected = modList.filter {
            uiState.value.modsSelected.contains(it.id) && it.isEnable != b && (!it.isEncrypted || !it.password.isNullOrEmpty())
        }
        if (modsSelected.isEmpty()) return
        val checkCanSwitchModsResult = checkCanSwitchModsUserCase()
        when (checkCanSwitchModsResult.first) {
            ResultCode.NO_PERMISSION -> {
                setRequestPermissionPath(
                    permissionTools.getRequestPermissionPath(
                        checkCanSwitchModsResult.second
                    )
                )
                setOpenPermissionRequestDialog(true)
            }

            ResultCode.SUCCESS -> {
                setUnzipProgress("")
                setMultitaskingProgress("")
                viewModelScope.launch {
                    if (b) {
                        enableMod(modList)
                    } else {
                        disableMod(modList)
                    }
                }

            }
        }
    }

    fun exitSelect() {
        _uiState.update {
            it.copy(isMultiSelect = false, modsSelected = emptyList())
        }
    }

    fun allSelect(modList: List<ModBean>) {
        _uiState.update { it ->
            it.copy(modsSelected = modList.map { it.id })
        }
    }

    fun deselect() {
        _uiState.update {
            it.copy(modsSelected = emptyList())
        }
    }

    fun delSelectedMods() {
        if (_uiState.value.modsSelected.isEmpty()) return
        if (!_uiState.value.showDelSelectModsDialog) {
            setShowDelSelectModsDialog(true)
        } else {
            setShowDelSelectModsDialog(false)
            viewModelScope.launch {
                val result = deleteSelectedModUserCase(_uiState.value.modsSelected)
                when (result.code) {
                    ResultCode.SUCCESS -> {
                        if (result.delEnableMods.isNotEmpty()) {
                            delModsList = result.delEnableMods
                            _uiState.update {
                                it.copy(delEnableModsList = result.delEnableMods)
                            }
                            setShowDisEnableModsDialog(true)
                        }

                        ToastUtils.longCall(
                            App.get().getString(
                                R.string.toast_del_mods_success,
                                result.delEnableMods.size.toString()
                            )
                        )
                        if (result.delEnableMods.isNotEmpty()) {
                            delModsList = result.delEnableMods
                            _uiState.update {
                                it.copy(delEnableModsList = result.delEnableMods)
                            }
                            setShowDisEnableModsDialog(true)
                        }
                    }
                }
                updateFiles(_currentPath)

            }
        }


    }

    fun setShowDelSelectModsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDelSelectModsDialog = b)
        }
    }

    fun refreshModDetail(): Boolean {
        val modBean = _uiState.value.modDetail ?: return false

        // 启动协程等待解压并读取完成
        viewModelScope.launch {
            val result = flashModDetailUserCase(modBean)
            setModDetail(result.mod)
        }
        return true
    }

    fun deleteMod() {
        if (_uiState.value.modDetail == null) return
        if (!_uiState.value.showDelModDialog) {
            setShowDelModDialog(true)
        } else {
            setShowDelModDialog(false)
            setShowModDetail(false)
            val delMod = _uiState.value.modDetail!!
            viewModelScope.launch {
                val result = deleteModUserCase(delMod)
                if (result.code == ResultCode.SUCCESS) {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_del_mods_success,
                            result.delMods.size.toString()
                        )
                    )
                    delModsList = result.delEnableMods
                    _uiState.update {
                        it.copy(delEnableModsList = result.delEnableMods)
                    }
                    setShowDisEnableModsDialog(true)
                }

                updateFiles(_currentPath)

            }
        }
    }

    fun setShowDelModDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDelModDialog = b)
        }
    }

    // 设置开启失败的mods
    fun setEnableFailedMods(enableFailedMods: List<ModBean>) {
        _uiState.update {
            it.copy(openFailedMods = enableFailedMods)
        }
    }

    // 通过path获取mods
    fun getModsByPath(path: String): List<ModBean> {
        return if (File(path).isDirectory) {
            _uiState.value.modList.filter { it.path?.contains("$path/") == true }
        } else {
            _uiState.value.modList.filter { it.path?.contains(path) == true }
        }

    }

    // 通过path获取mods严格匹配
    fun getModsByPathStrict(path: String): List<ModBean> {
        return _uiState.value.modList.filter { it.path == path }
    }

    private fun getArchiveFiles(path: String) {
        _currentZipPath = path
        // 读取压缩包文件
        val fileHeaders = ArchiveUtil.listInArchiveFiles(path)
        // 拼接路径
        val files = fileHeaders.map {
            File("$path/$it")
        }
        _currentFiles = files

    }

    fun updateFiles(currentPath: String) {
        _currentPath = currentPath
        _uiState.update { it.copy(currentPath = currentPath) }
        Log.d("ModViewModel", "updateFiles: 触发跟新文件列表")
        // 构建当前文件
        viewModelScope.launch {
            if (File(currentPath).isDirectory) {
                _uiState.update { it ->
                    it.copy(
                        currentFiles = File(currentPath).listFiles()?.toList()
                            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                            ?: emptyList()
                    )
                }
            } else {
                if (_currentZipPath == "" || !currentPath.contains(_currentZipPath)) {
                    getArchiveFiles(currentPath)
                }
                _uiState.update { it -> it.copy(currentFiles = _currentFiles.filter { it.parent == currentPath }) }
            }
        }
    }

    fun getModsByVirtualPathsStrict(path: String): List<ModBean> {
        return _uiState.value.modList.filter { it.virtualPaths == path }
    }

    fun getModsByVirtualPaths(path: String): List<ModBean> {
        return _uiState.value.modList.filter { it.virtualPaths?.contains(path) == true }
    }

    fun setCurrentMods(mods: List<ModBean>) {
        _uiState.update {
            it.copy(currentMods = mods)
        }
    }


    fun getAppPathsManager(): AppPathsManager {
        return appPathsManager
    }

    fun getPermissionTools(): PermissionTools {
        return permissionTools
    }

    fun getFileToolsManager(): FileToolsManager {
        return fileToolsManager
    }

    // 设置是否显示强制扫描对话框
    fun setShowForceScanDialog(b: Boolean) {
        _uiState.update {
            it.copy(showForceScanDialog = b)
        }
    }

}

