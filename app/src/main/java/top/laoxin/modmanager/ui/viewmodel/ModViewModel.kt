package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.data.mods.ModRepository
import top.laoxin.modmanager.data.UserPreferencesRepository
import top.laoxin.modmanager.data.backups.BackupRepository
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.ZipTools

class ModViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modRepository: ModRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ModUiState())
    // val uiState: StateFlow<ModUiState> = _uiState.asStateFlow()

    // 搜索框内容
    private val _searchText = mutableStateOf("")

    // 互斥锁
    private val mutex = Mutex()

    private var _requestPermissionPath by mutableStateOf("")
    val requestPermissionPath: String
        get() = _requestPermissionPath

    private var _gameInfo = GameInfoConstant.gameInfoList[0]

    // 删除的mods
    private var delModsList = emptyList<ModBean>()


    // mod列表


    // 扫描mod目录列表
    // 从用户配置中读取mod目录
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App)
                ModViewModel(
                    application.userPreferencesRepository,
                    application.container.modRepository,
                    application.container.backupRepository
                )
            }
        }
        var isInitialized = false
        var searchJob: Job? = null
        var enableJob: Job? = null
        var flashModsJob: Job? = null
        var checkPasswordJob: Job? = null
    }

    // 使用热数据流读取用户设置并实时更新
    private val scanQQDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_QQ_DIRECTORY", false)
    private val selectedDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow(
            "SELECTED_DIRECTORY",
            ModTools.DOWNLOAD_MOD_PATH
        )
    private val scanDownloadFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_DOWNLOAD", false)

    // 用户提示
    private val userTips = userPreferencesRepository.getPreferenceFlow("USER_TIPS", true)

    // 选择游戏`
    private val selectedGame = userPreferencesRepository.getPreferenceFlow("SELECTED_GAME", 0)

    // 扫描文件夹中的Mods
    private val scanDirectoryMods =
        userPreferencesRepository.getPreferenceFlow("SCAN_DIRECTORY_MODS", false)


    // 生成用户配置对象
    private val userPreferences: Flow<UserPreferencesState> = combine(
        scanQQDirectoryFlow,
        selectedDirectoryFlow,
        scanDownloadFlow,
        selectedGame,
        scanDirectoryMods
    ) { scanQQDirectory, selectedDirectory, scanDownload, selectedGame, scanDirectoryMods ->
        UserPreferencesState(
            scanQQDirectory = scanQQDirectory,
            selectedDirectory = selectedDirectory,
            scanDownload = scanDownload,
            selectedGameIndex = selectedGame,
            scanDirectoryMods = scanDirectoryMods
        )
    }


    var uiState = _uiState.asStateFlow()

    init {
        //checkPermission()
        viewModelScope.launch {
            userPreferences.collect {
                _gameInfo = GameInfoConstant.gameInfoList[it.selectedGameIndex]
                // 获取全部mods
                val modsFlow =
                    modRepository.getModsByGamePackageName(_gameInfo.packageName)
                // 转化statFlow

                // 获取开启的mods
                val enableModsFlow =
                    modRepository.getEnableMods(_gameInfo.packageName)

                // 获取关闭的mods
                val disableModsFlow =
                    modRepository.getDisableMods(_gameInfo.packageName)
                uiState = combine(
                    _uiState,
                    modsFlow,
                    enableModsFlow,
                    disableModsFlow,
                    userTips
                ) { uiState, mods, enableMods, disableMods, userTips ->
                    // 在这里，你可以定义一个新的ModUiState对象来保存这四个值
                    // 在这个示例中，我将创建一个新的ModUiState对象
                    ModUiState(
                        modList = mods,
                        enableModList = enableMods,
                        disableModList = disableMods,
                        // 其他属性保持不变
                        searchModList = uiState.searchModList,
                        isLoading = uiState.isLoading,
                        openPermissionRequestDialog = uiState.openPermissionRequestDialog,
                        modDetail = uiState.modDetail,
                        showModDetail = uiState.showModDetail,
                        searchBoxVisible = uiState.searchBoxVisible,
                        loadingPath = uiState.loadingPath,
                        selectedMenuItem = uiState.selectedMenuItem,
                        showPasswordDialog = uiState.showPasswordDialog,
                        tipsText = uiState.tipsText,
                        showTips = uiState.showTips,
                        modSwitchEnable = uiState.modSwitchEnable,
                        showUserTipsDialog = userTips,
                        delEnableModsList = uiState.delEnableModsList,
                        showDisEnableModsDialog = uiState.showDisEnableModsDialog
                    )
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    _uiState.value
                )


            }

        }
        if (PermissionTools.checkShizukuPermission()) {
            init()
        } else {
            // 开启授权提示窗口
            setOpenPermissionRequestDialog(true)

        }


    }

    fun flashMods(isInit: Boolean) {

        flashModsJob?.cancel()
        var pathType = PathType.NULL
        setLoading(true)
        flashModsJob = viewModelScope.launch(Dispatchers.IO) {
            userPreferences.collect { it ->

                val gameInfo = GameInfoConstant.gameInfoList[it.selectedGameIndex].copy(
                    modSavePath = ModTools.ROOT_PATH + it.selectedDirectory + GameInfoConstant.gameInfoList[it.selectedGameIndex].packageName + "/"
                )
                _gameInfo = GameInfoConstant.gameInfoList[it.selectedGameIndex]
                if (gameInfo.packageName.isEmpty()) {
                    setLoading(false)
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(R.string.toast_please_select_game)
                    }
                    this@launch.cancel()
                }

                val gamePath = gameInfo.gamePath
                //val requestPermissionPath = PermissionTools.getRequestPermissionPath(gamePath)
                if (PermissionTools.checkPermission(gamePath) == PathType.NULL) {
                    withContext(Dispatchers.Main) {
                        setRequestPermissionPath(gamePath)
                        setOpenPermissionRequestDialog(true)
                    }
                    setLoading(false)
                    this@launch.cancel()
                }

                var modsSelected = mutableListOf<ModBean>()
                if (it.scanQQDirectory) {
                    withContext(Dispatchers.Main) {
                        setLoadingPath(ScanModPath.MOD_PATH_QQ)
                    }
                    pathType = PermissionTools.checkPermission(ScanModPath.MOD_PATH_QQ)
                    if (pathType == PathType.NULL) {
                        withContext(Dispatchers.Main) {
                            setRequestPermissionPath(
                                PermissionTools.getRequestPermissionPath(
                                    ScanModPath.MOD_PATH_QQ
                                )
                            )
                            setOpenPermissionRequestDialog(true)
                        }
                        setLoading(false)
                        this@launch.cancel()
                    }
                    try {
                        ModTools.scanMods(
                            ScanModPath.MOD_PATH_QQ,
                            gameInfo
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            //setLoading(false)
                            ToastUtils.longCall(R.string.toast_scanQQ_failed)
                        }
                    }


                }
                try {
                    ModTools.scanMods(
                        ModTools.ROOT_PATH + it.selectedDirectory,
                        gameInfo
                    )
                } catch (e: Exception) {
                    Log.e("ModViewModel", "flashMods: $e")
                }

                if (it.scanDownload) {
                    withContext(Dispatchers.Main) {
                        setLoadingPath(ScanModPath.MOD_PATH_DOWNLOAD)
                    }
                    ModTools.setModsToolsSpecialPathReadType(PathType.FILE)
                    ModTools.scanMods(
                        ScanModPath.MOD_PATH_DOWNLOAD,
                        gameInfo,
                    )
                }
                withContext(Dispatchers.Main) {
                    setLoadingPath(ModTools.ROOT_PATH + it.selectedDirectory)
                }

                modsSelected =
                    ModTools.createMods(
                        gameInfo.modSavePath,
                        gameInfo,
                    )
                        .toMutableList()


                val modsScan = mutableListOf<ModBean>()
                //modsScan.addAll(modsQQ)
                //modsScan.addAll(modsDownload)
                modsScan.addAll(modsSelected)
                if (it.scanDirectoryMods) {
                    modsScan.addAll(ModTools.scanDirectoryMods(gameInfo.modSavePath, gameInfo))
                }
                // 从数据库中获取所有mods
                Log.d("ModViewModel", "扫描到的mod: $modsScan")
                modRepository.getAllIModsStream().collect { mods ->
                    Log.d("ModViewModel", "数据库中的mod: $mods")
                    // 对比modsScan和mods，对比是否有新的mod
                    val addMods = modsScan.filter { mod -> mods.none { it.path == mod.path } }
                    Log.d("ModViewModel", "添加的mod: $addMods")
                    // 对比modsScan和mods，删除数据库中不存在的mod
                    checkDelMods(mods, modsScan, gameInfo, isInit)
                    // 去除数据库mods中重复的Mod
                    modRepository.insertAll(addMods)

                    setLoading(false)
                    flashModsJob?.cancel()
                }
            }
        }
    }

    private suspend fun checkDelMods(
        mods: List<ModBean>,
        modsScan: MutableList<ModBean>,
        gameInfo: GameInfo,
        init: Boolean
    ) {
        var delMods = mods.filter { mod -> modsScan.none { it.path == mod.path } }
        delMods = delMods.filter { it.gamePackageName == gameInfo.packageName }
        val delEnableMods = delMods.filter { it.isEnable }
        delModsList = delMods
        withContext(Dispatchers.Main) {
            if (delEnableMods.isNotEmpty()) {
                setShowDisEnableModsDialog(true)
                _uiState.update {
                    it.copy(delEnableModsList = delEnableMods)
                }
            } else {
                delMods()
            }
        }

    }


    private fun init() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.collect { userPreference ->
                _gameInfo = GameInfoConstant.gameInfoList[userPreference.selectedGameIndex]
            }
        }
        if (!isInitialized) {
            Log.d("ModViewModel", "init: 初始化")
            //flashMods(true)
            isInitialized = true
        }
    }

    fun setModList(modList: List<ModBean>) {
        _uiState.value = _uiState.value.copy(modList = modList)
    }

    // 设置已开启mod
    fun setEnableMods(modList: List<ModBean>) {
        _uiState.value = _uiState.value.copy(enableModList = modList)
    }

    // 设置已关闭mods
    fun setDisableMods(modList: List<ModBean>) {
        _uiState.value = _uiState.value.copy(disableModList = modList)
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
        this._searchText.value = searchText
        // 取消上一个搜索任务
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            modRepository.search(searchText, _gameInfo.packageName).collect {
                setSearchMods(it)
            }
        }
    }

    // 设置是否显示mod详情
    fun setShowModDetail(showModDetail: Boolean) {
        _uiState.value = _uiState.value.copy(showModDetail = showModDetail)
    }

    // 获取输入框内容
    fun getSearchText(): String {
        return _searchText.value
    }

    // 添加mod到数据库
    suspend fun addMod(mod: ModBean) {
        viewModelScope.launch {
            modRepository.insertMod(mod)
        }
    }

    // 添加所有mod到数据库
    suspend fun addAllMods(mods: List<ModBean>) {
        viewModelScope.launch {
            modRepository.insertAll(mods)
        }
    }

    // 设置正在加载
    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }

    // 设置加载目录
    fun setLoadingPath(loadingPath: String) {
        _uiState.value = _uiState.value.copy(loadingPath = loadingPath)
    }


    suspend fun addMods(mods: List<ModBean>) {
        mutex.withLock {
            val modList = _uiState.value.modList.toMutableList()
            modList.addAll(mods)
            setModList(modList)
        }
    }

    // 添加备份到数据库
    suspend fun addBackup(backup: BackupBean) {
        viewModelScope.launch {
            backupRepository.insert(backup)
        }
    }

    // 清空mods
    fun clearMods() {
        setModList(emptyList())
    }

    // 获取所有mods
    fun getAllMods(): List<ModBean> {
        return _uiState.value.modList
    }


    // 权限检查
    private fun checkPermission() {
        if (!PermissionTools.hasStoragePermission()) {
            setOpenPermissionRequestDialog(true)
        }
    }


    // 打开mod详情
    fun openModDetail(mod: ModBean, showModDetail: Boolean) {
        setModDetail(mod)
        setShowModDetail(showModDetail)
    }

    // 开启或关闭mod
    fun switchMod(modBean: ModBean, b: Boolean, isDel : Boolean = false) {
        if (PermissionTools.checkPermission(ModTools.MY_APP_PATH) == PathType.NULL) {
            setRequestPermissionPath(ModTools.MY_APP_PATH)
            setOpenPermissionRequestDialog(true)
            return
        }

        if (PermissionTools.checkPermission(modBean.gameModPath!!) == PathType.NULL) {
            setRequestPermissionPath(modBean.gameModPath)
            setOpenPermissionRequestDialog(true)
            return
        }

        Log.d("ModViewModel", "enableMod called with: modBean = $modBean, b = $b")
        if (b) {
            enableMod(modBean,isDel)
        } else {
            disableMod(modBean, isDel)
        }
    }

    // 打开密码输入框
    fun showPasswordDialog(b: Boolean) {
        _uiState.value = _uiState.value.copy(showPasswordDialog = b)
    }


    // 开启mod
    private fun enableMod(modBean: ModBean, isDel: Boolean) {
        // 判断modbean是否包含密码
        if (modBean.isEncrypted && modBean.password == null) {
            // 如果包含密码，弹出密码输入框
            setModDetail(modBean)
            showPasswordDialog(true)
            return
        }
        // 设置modSwitchEnable为false
        setModSwitchEnable(false)
        setTipsText(R.string.tips_open_mod)
        setShowTips(true)
        Log.d("ModViewModel", "enableMod: 开始执行开启 : $modBean")
        enableJob?.cancel()
        enableJob = viewModelScope.launch(Dispatchers.IO) {
            // 鉴权
            val gameModPath = modBean.gameModPath!!

            withContext(Dispatchers.Main) {
                setTipsText(R.string.tips_backups_game_files)
            }
            val backups = ModTools.backupGameFiles(
                gameModPath,
                modBean,
                _gameInfo.packageName + "/"
            )
            Log.d("ModViewModel", "开启mod: $backups")
            if (backups.isEmpty()) {
                withContext(Dispatchers.Main) {
                    setShowTips(false)
                    setModSwitchEnable(true)
                    ToastUtils.longCall(R.string.toast_backup_failed)
                    this@launch.cancel()
                }
            }
            backupRepository.insertAll(backups)
            withContext(Dispatchers.Main) {
                setTipsText(R.string.tips_unzip_mod)
            }
            var unZipPath: String? = ""
            if (modBean.isZipFile) {
                unZipPath = ZipTools.unZip(
                    modBean.path!!,
                    ModTools.MODS_UNZIP_PATH + _gameInfo.packageName + "/",
                    modBean.password ?: ""
                )

                if (unZipPath == null) {
                    withContext(Dispatchers.Main) {
                        //setTipsText(R.string.tips_unzip_failed)
                        setShowTips(false)
                        setModSwitchEnable(true)
                        //ToastUtils.longCall(R.string.toast_unzip_failed)
                        //this@launch.cancel()
                    }
                }

            } else {
                unZipPath = ""
            }


            withContext(Dispatchers.Main) {
                setTipsText(R.string.tips_copy_mod_to_game)
            }
            val copyMod: Boolean = ModTools.copyModFiles(
                modBean,
                gameModPath,
                unZipPath!!,
            )
            //copyMod = false
            if (!copyMod) {
                withContext(Dispatchers.Main) {
                    setTipsText(R.string.tips_copy_mod_failed)
                    /*setShowTips(false)
                    setModSwitchEnable(true)
                    ToastUtils.longCall(R.string.toast_copy_failed)
                    this@launch.cancel()*/
                    val unZip = ModTools.copyModsByStream(
                        modBean.path!!,
                        gameModPath,
                        modBean.modFiles!!,
                        modBean.password,
                        PermissionTools.checkPermission(gameModPath)
                    )
                    if (!unZip) {
                        withContext(Dispatchers.Main) {
                            setShowTips(false)
                            setModSwitchEnable(true)
                            ToastUtils.longCall(R.string.toast_copy_failed)
                            this@launch.cancel()
                        }
                    }

                }

            }


            modRepository.updateMod(modBean.copy(isEnable = true))
            withContext(Dispatchers.Main) {
                ToastUtils.longCall(R.string.toast_mod_open_success)
                setShowTips(false)
                setModSwitchEnable(true)
                //updateMod(modBean, true)
            }

            enableJob?.cancel()
            /*modRepository.getAllIModsStream().collect { mods ->
                withContext(Dispatchers.Main) {
                    setModList(mods)
                }
            }*/

        }
    }

    // 关闭mod
    private fun disableMod(modBean: ModBean, isDel: Boolean) {
        setModSwitchEnable(false)
        setTipsText(R.string.tips_close_mod)
        setShowTips(true)
        enableJob?.cancel()
        Log.d("ModViewModel", "disableMod: 开始执行关闭")
        enableJob = viewModelScope.launch(Dispatchers.IO) {
            backupRepository.getByModNameAndGamePackageName(modBean.name!!, _gameInfo.packageName)
                .collect { backupBeans ->
                    if (PermissionTools.checkPermission(backupBeans[0].gameFilePath!!) == PathType.NULL) {
                        withContext(Dispatchers.Main) {
                            setRequestPermissionPath(
                                PermissionTools.getRequestPermissionPath(
                                    backupBeans[0].gameFilePath!!
                                )
                            )
                            setOpenPermissionRequestDialog(true)
                        }
                        setShowTips(false)
                        setModSwitchEnable(true)
                        this@launch.cancel()
                    }
                    withContext(Dispatchers.Main) {
                        setTipsText(R.string.tips_restore_game_files)
                    }
                    val restoreFlag: Boolean = ModTools.restoreGameFiles(
                        backupBeans,
                    )
                    if (!restoreFlag) {
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(R.string.toast_restore_failed)
                            setShowTips(false)
                            setModSwitchEnable(true)
                            this@launch.cancel()
                        }
                    }

                    modRepository.updateMod(modBean.copy(isEnable = false))
                    withContext(Dispatchers.Main) {
                        Log.d("ModViewModel", "disableMod: 开始关闭提示")
                        ToastUtils.longCall(R.string.toast_mod_close_success)
                        setShowTips(false)
                        setModSwitchEnable(true)
                        //updateMod(modBean, false )
                    }
                    if (isDel) {
                        _uiState.update {
                            it.copy(
                                delEnableModsList = it.delEnableModsList.filter { it.path != modBean.path }
                            )
                        }
                    }

                    enableJob?.cancel()
                    /*modRepository.getAllIModsStream().collect { mods ->
                        withContext(Dispatchers.Main) {
                            setModList(mods)
                        }
                    }*/
                }
        }
    }

    fun checkPassword(s: String) {
        if (PermissionTools.checkPermission(ModTools.MY_APP_PATH) == PathType.NULL) {
            setRequestPermissionPath(
                PermissionTools.getRequestPermissionPath(
                    ModTools.MY_APP_PATH
                )
            )
            setOpenPermissionRequestDialog(true)
            return
        }
        checkPasswordJob?.cancel()
        setModSwitchEnable(false)
        setTipsText(R.string.tips_check_password)
        setShowTips(true)
        checkPasswordJob = viewModelScope.launch(Dispatchers.IO) {
            val modBean = _uiState.value.modDetail
            if (modBean != null) {
                withContext(Dispatchers.Main) {
                    setTipsText(R.string.tips_unzip_mod)
                }
                val unZipPath =
                    ZipTools.unZip(
                        modBean.path!!,
                        ModTools.MODS_UNZIP_PATH + _gameInfo.packageName + "/",
                        s
                    )
                if (unZipPath == null) {
                    withContext(Dispatchers.Main) {
                        setShowTips(false)
                        setModSwitchEnable(true)
                        ToastUtils.longCall(R.string.toast_check_failed)
                        return@withContext
                    }
                } else {
                    modRepository.getModsByPathAndGamePackageName(
                        modBean.path,
                        modBean.gamePackageName!!
                    ).collect { mods ->
                        withContext(Dispatchers.Main) {
                            setTipsText(R.string.tips_read_mod)
                        }
                        val newModList: MutableList<ModBean> = mutableListOf()
                        mods.forEach {
                            val newMod = it.copy(password = s)
                            val mod: ModBean = ModTools.readModInfo(unZipPath, newMod)
                            newModList.add(mod)
                        }
                        modRepository.updateAll(newModList)
                        withContext(Dispatchers.Main) {
                            setShowTips(false)
                            setModSwitchEnable(true)
                            ToastUtils.longCall(R.string.toast_password_right)
                            showPasswordDialog(false)
                        }
                        checkPasswordJob?.cancel()
                    }
                    // 读取加密mod信息

                }
            }
            checkPasswordJob?.cancel()
        }


    }


    fun setShowTips(b: Boolean) {
        _uiState.value = _uiState.value.copy(showTips = b)
    }


    fun setTipsText(@StringRes s: Int) {
        _uiState.value = _uiState.value.copy(tipsText = s)
    }

    fun setModSwitchEnable(b: Boolean) {
        _uiState.value = _uiState.value.copy(modSwitchEnable = b)
    }

    // 设置请求权限文件夹
    fun setRequestPermissionPath(path: String) {
        _requestPermissionPath = path
    }

    // 设置用户提示
    fun setUserTipsDialog(b: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "USER_TIPS",
                b
            )
        }
    }

    fun setShowDisEnableModsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDisEnableModsDialog = b)
        }
    }

    fun delMods() {
        viewModelScope.launch {
            modRepository.deleteAll(delModsList)
            setShowDisEnableModsDialog(false)
        }
    }
}


