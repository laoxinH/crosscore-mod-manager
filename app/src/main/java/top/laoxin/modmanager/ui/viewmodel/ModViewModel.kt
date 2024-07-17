package top.laoxin.modmanager.ui.viewmodel

import android.os.FileObserver
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
import top.laoxin.modmanager.database.mods.ModRepository
import top.laoxin.modmanager.database.UserPreferencesRepository
import top.laoxin.modmanager.database.backups.BackupRepository
import top.laoxin.modmanager.exception.NoSelectedGameException
import top.laoxin.modmanager.exception.PasswordErrorException
import top.laoxin.modmanager.exception.PermissionsException
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import java.io.File

class ModViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modRepository: ModRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ModUiState())
//     val uiState: StateFlow<ModUiState> = _uiState.asStateFlow()

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
        var updateAllModsJob: Job? = null
        var updateEnableModsJob: Job? = null
        var updateDisEnableModsJob: Job? = null
        var checkPasswordJob: Job? = null
        var fileObserver: FileObserver? = null
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
    private val userPreferences: StateFlow<UserPreferencesState> = combine(
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
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferencesState()
    )


    val uiState: StateFlow<ModUiState> = combine(
        userTips,
        _uiState
    ) { userTips, uiState ->
        uiState.copy(showUserTipsDialog = userTips)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ModUiState()
    )


    init {
        Log.d("ModViewModel", "init: 初始化${userPreferences.value}")
        //checkPermission()
        viewModelScope.launch {
            userPreferences.collect { it ->

                updateGameInfo(it)
                updateAllMods()
                updateEnableMods()
                updateDisEnableMods()
                startFileObserver(it)
            }

        }


        /*if (PermissionTools.checkShizukuPermission()) {
            init()
        } else {
            // 开启授权提示窗口
            setOpenPermissionRequestDialog(true)

        }*/


    }

    private fun updateGameInfo(userPreferencesState: UserPreferencesState) {
        _gameInfo = GameInfoConstant.gameInfoList[userPreferencesState.selectedGameIndex]
        _gameInfo = _gameInfo.copy(
            modSavePath = ModTools.ROOT_PATH + userPreferencesState.selectedDirectory + _gameInfo.packageName + "/"
        )
    }

    // 更新所有mods
    private fun updateAllMods() {
        updateAllModsJob?.cancel()
        updateAllModsJob = viewModelScope.launch {
            modRepository.getModsByGamePackageName(_gameInfo.packageName).collectLatest { mods ->
                //Log.d("ModViewModel", "updateAllMods: $it")
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
            modRepository.getEnableMods(_gameInfo.packageName).collectLatest { mods ->
                _uiState.update {
                    it.copy(enableModList = mods)
                }
            }
        }
    }

    // 更新已开启mods
    private fun updateDisEnableMods() {
        updateDisEnableModsJob?.cancel()
        updateDisEnableModsJob = viewModelScope.launch {
            modRepository.getDisableMods(_gameInfo.packageName).collectLatest { mods ->
                _uiState.update {
                    it.copy(disableModList = mods)
                }
            }
        }
    }

    // 启动文件夹监听
    private fun startFileObserver(userPreferencesState: UserPreferencesState) {
        fileObserver?.stopWatching()
        fileObserver = object : FileObserver(
            ModTools.ROOT_PATH + userPreferencesState.selectedDirectory + _gameInfo.packageName,
            ALL_EVENTS
        ) {
            override fun onEvent(event: Int, path: String?) {
                val file =
                    File(ModTools.ROOT_PATH + userPreferencesState.selectedDirectory + _gameInfo.packageName).absolutePath + "/"
                when (event) {
                    CREATE -> {
                        onCreateMod(path, file)
                    }

                    DELETE -> {
                        //onDelMod(path, file)
                    }

                    MOVED_FROM -> {
                        //onDelMod(path, file)
                    }

                    MOVED_TO -> {
                        onCreateMod(path, file)
                    }
                    // Add more cases if you want to handle more events
                    else -> return
                }
            }

        }
        fileObserver?.startWatching()
    }


    private fun onDelMod(path: String?, file: String) {
        Log.d("FileObserver", "File $path has been deleted")
        val filepath = File(file, path!!).absolutePath
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            modRepository.getModsByPathAndGamePackageName(filepath, _gameInfo.packageName)
                .collect { mods ->
                    val filter = mods.filter { it.isEnable }
                    delModsList = mods
                    if (filter.isNotEmpty()) {
                        setShowDisEnableModsDialog(true)
                        _uiState.update {
                            it.copy(delEnableModsList = filter)
                        }
                    } else {
                        delMods()
                    }
                    searchJob?.cancel()
                }
        }
    }

    /**
     * 提取文件
     */
    private fun onCreateMod(path: String?, file: String) {
        Log.d("FileObserver", "New file $path has been created")
        if (File(file, path).isDirectory) return
        val filepath = File(file, path).absolutePath
        if (!ArchiveUtil.isArchive(filepath)) return
        viewModelScope.launch {
            val createModTempMap =
                ModTools.createModTempMap(filepath, file,
                    ArchiveUtil.listInArchiveFiles(filepath)
                        .map { mutableListOf(it, it) }
                        .toMutableList(), _gameInfo)
            Log.d("ConsoleViewModel", "onEvent: $createModTempMap")
            val readModBeans = ModTools.readModBeans(
                filepath,
                file,
                createModTempMap,
                _gameInfo.gamePath
            )
            modRepository.insertAll(readModBeans)
        }
    }

    fun flashMods(isInit: Boolean) {
        kotlin.runCatching {
            //检擦是否选择游戏
            checkSelectGame()
            // 检查权限
            checkPermission(_gameInfo.gamePath)
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
        }.onSuccess {
            fileObserver?.stopWatching()
            setLoading(true)
            viewModelScope.launch {
                kotlin.runCatching {
                    val userPreferencesState = userPreferences.value
                    if (userPreferencesState.scanQQDirectory) {
                        // 扫描QQ
                        checkPermission(ScanModPath.MOD_PATH_QQ)
                        setLoadingPath(ScanModPath.MOD_PATH_QQ)
                        ModTools.scanMods(ScanModPath.MOD_PATH_QQ, _gameInfo)
                    }
                    // 扫描系统下载
                    if (userPreferencesState.scanDownload) {
                        setLoadingPath(ScanModPath.MOD_PATH_DOWNLOAD)
                        ModTools.scanMods(ScanModPath.MOD_PATH_DOWNLOAD, _gameInfo)
                    }
                    // 扫描用户选择的目录
                    setLoadingPath(ModTools.ROOT_PATH + userPreferencesState.selectedDirectory)
                    ModTools.scanMods(
                        ModTools.ROOT_PATH + userPreferencesState.selectedDirectory,
                        _gameInfo
                    )
                    // 创建mods
                    val modsScan = ModTools.createMods(
                        _gameInfo.modSavePath,
                        _gameInfo
                    ).toMutableList()
                    // 扫描文件夹中的mod文件
                    if (userPreferencesState.scanDirectoryMods) {
                        modsScan.addAll(
                            ModTools.scanDirectoryMods(
                                _gameInfo.modSavePath,
                                _gameInfo
                            )
                        )
                    }
                    val mods = uiState.value.modList
                    // 对比modsScan和mods，对比是否有新的mod
                    val addMods =
                        modsScan.filter { mod -> mods.none { (it.path == mod.path && it.name == mod.name) } }
                    // 对比modsScan和mods，删除数据库中不存在的mod
                    checkDelMods(mods, modsScan, _gameInfo, isInit)
                    // 去除数据库mods中重复的Mod
                    withContext(Dispatchers.IO) {
                        modRepository.insertAll(addMods)
                        modRepository.updateAll(checkUpdateMods(mods, modsScan))
                    }
                    setLoading(false)
                    fileObserver?.startWatching()
                }.onFailure {
                    ToastUtils.longCall(it.message.toString())
                    setLoading(false)
                    fileObserver?.startWatching()
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

    // 对比是否有更新mod
    private fun checkUpdateMods(
        mods: List<ModBean>,
        modsScan: List<ModBean>
    ): MutableList<ModBean> {
        val updatedMods = mutableListOf<ModBean>()

        for (mod1 in mods) {
            val mod2 = modsScan.find { it.name == mod1.name && it.path == mod1.path }

            if (mod2 != null && !mod1.equalsIgnoreId(mod2) && !mod1.isEncrypted) {
                updatedMods.add(
                    mod2.copy(
                        id = mod1.id,
                        isEnable = mod1.isEnable,
                        password = mod1.password
                    )
                )
            }
        }

        return updatedMods
    }


    // 权限检查


    // 权限检查
    private fun checkPermission(path: String) {
        if (PermissionTools.checkPermission(path) == PathType.NULL) {
            setRequestPermissionPath(PermissionTools.getRequestPermissionPath(path))
            setOpenPermissionRequestDialog(true)
            throw PermissionsException(App.get().getString(R.string.toast_has_no_prim))
        }
        // 抛出未授权异常
    }

    // 检查是否选择游戏
    private fun checkSelectGame() {
        if (_gameInfo.packageName.isEmpty()) {
            throw NoSelectedGameException(App.get().getString(R.string.toast_please_select_game))
        }
    }


    // 打开mod详情
    fun openModDetail(mod: ModBean, showModDetail: Boolean) {
        setModDetail(mod)
        setShowModDetail(showModDetail)
    }

    // 开启或关闭mod
    fun switchMod(modBean: ModBean, b: Boolean, isDel: Boolean = false) {
        kotlin.runCatching {
            checkPermission(modBean.gameModPath!!)
            checkPermission(ModTools.MY_APP_PATH)
        }.onSuccess {
            Log.d("ModViewModel", "enableMod called with: modBean = $modBean, b = $b")
            if (b) {
                enableMod(modBean, isDel)
            } else {
                disableMod(modBean, isDel)
            }
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
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
        viewModelScope.launch {
            kotlin.runCatching {
                val gameModPath = modBean.gameModPath!!

                // 备份游戏文件
                if (_gameInfo.enableBackup) {
                    setTipsText(R.string.tips_backups_game_files)
                    val backups = ModTools.backupGameFiles(
                        gameModPath,
                        modBean,
                        _gameInfo.packageName + "/"
                    )
                    backupRepository.insertAll(backups)
                }
                // 解压mod压缩包
                setTipsText(R.string.tips_unzip_mod)
                var unZipPath: String = ""
                if (modBean.isZipFile) {
                    withContext(Dispatchers.IO) {
                        val decompression = ArchiveUtil.decompression(
                            modBean.path!!,
                            ModTools.MODS_UNZIP_PATH + _gameInfo.packageName + "/" + File(modBean.path).nameWithoutExtension + "/",
                            modBean.password
                        )
                        if (decompression) {
                            unZipPath =
                                ModTools.MODS_UNZIP_PATH + _gameInfo.packageName + "/" + File(
                                    modBean.path
                                ).nameWithoutExtension + "/"
                        } else {
                            throw Exception(
                                App.get().getString(R.string.toast_decompression_failed)
                            )
                        }
                    }
                }
                // 执行特殊操作
                setTipsText(R.string.tips_special_operation)
                ModTools.specialOperationEnable(modBean, _gameInfo.packageName)
                // 复制mod文件
                setTipsText(R.string.tips_copy_mod_to_game)
                ModTools.copyModFiles(modBean, gameModPath, unZipPath)
                modRepository.updateMod(modBean.copy(isEnable = true))
                ToastUtils.longCall(R.string.toast_mod_open_success)
                setShowTips(false)
                setModSwitchEnable(true)
            }.onFailure {
                setShowTips(false)
                setModSwitchEnable(true)
                ToastUtils.longCall(it.message.toString())
            }
        }
    }

    // 关闭mod
    private fun disableMod(modBean: ModBean, isDel: Boolean) {

        setModSwitchEnable(false)
        setTipsText(R.string.tips_close_mod)
        setShowTips(true)
        enableJob?.cancel()
        enableJob = viewModelScope.launch {
            backupRepository.getByModNameAndGamePackageName(modBean.name!!, _gameInfo.packageName)
                .collect { backupBeans ->
                    Log.d("ModViewModel", "disableMod: 开始执行关闭")
                    kotlin.runCatching {
                        // 特殊游戏操作
                        ModTools.specialOperationDisable(backupBeans, _gameInfo.packageName,modBean)
                        // 还原游戏文件
                        setTipsText(R.string.tips_restore_game_files)
                        ModTools.restoreGameFiles(backupBeans)
                        modRepository.updateMod(modBean.copy(isEnable = false))
                        ToastUtils.longCall(R.string.toast_mod_close_success)
                        setShowTips(false)
                        setModSwitchEnable(true)
                        if (isDel) {
                            _uiState.update { it ->
                                it.copy(
                                    delEnableModsList = it.delEnableModsList.filter { it.path != modBean.path }
                                )
                            }
                        }
                        enableJob?.cancel()
                    }.onFailure {
                        setShowTips(false)
                        setModSwitchEnable(true)
                        ToastUtils.longCall(it.message.toString())
                        it.printStackTrace()
                        enableJob?.cancel()
                    }
                }
        }
    }

    fun checkPassword(password: String) {
        kotlin.runCatching {
            checkPermission(ModTools.MY_APP_PATH)
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
        }.onSuccess {
            setModSwitchEnable(false)
            setTipsText(R.string.tips_check_password)
            setShowTips(true)
            checkPasswordJob?.cancel()
            checkPasswordJob = viewModelScope.launch {
                kotlin.runCatching {
                    val modBean = _uiState.value.modDetail
                    if (modBean != null) {
                        setTipsText(R.string.tips_unzip_mod)
                        val unZipPath =
                            ModTools.MODS_UNZIP_PATH + _gameInfo.packageName + "/" + File(modBean.path!!).nameWithoutExtension + "/"
                        var decompression = false
                        withContext(Dispatchers.IO) {
                            decompression = ArchiveUtil.decompression(
                                modBean.path,
                                unZipPath,
                                password
                            )
                            if (!decompression) throw PasswordErrorException(App.get().getString(R.string.toast_password_error))
                        }
                        modRepository.getModsByPathAndGamePackageName(
                            modBean.path,
                            modBean.gamePackageName!!
                        ).collect { mods ->
                            Log.d("ModViewModel", "更新mod: $mods")
                            setTipsText(R.string.tips_read_mod)
                            val newModList: MutableList<ModBean> = mutableListOf()
                            mods.forEach {
                                val newMod = it.copy(password = password)
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
                    }
                }.onFailure {
                    setShowTips(false)
                    setModSwitchEnable(true)
                    if (it.message?.contains("StandaloneCoroutine was cancelled") != true){
                        if (it.message?.contains("top.laoxin.modmanager") == true) {
                            ToastUtils.longCall(R.string.toast_password_error)
                        } else {
                            ToastUtils.longCall(it.message.toString())
                        }
                    }
                    it.printStackTrace()
                    showPasswordDialog(false)
                    checkPasswordJob?.cancel()
                }
                //checkPasswordJob?.cancel()
            }
        }
    }


    fun setShowTips(b: Boolean) {
        _uiState.value = _uiState.value.copy(showTips = b)
    }


    private fun setTipsText(@StringRes s: Int) {
        _uiState.value = _uiState.value.copy(tipsText = s)
    }

    fun setModSwitchEnable(b: Boolean) {
        _uiState.value = _uiState.value.copy(modSwitchEnable = b)
    }

    // 设置请求权限文件夹
    private fun setRequestPermissionPath(path: String) {
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

    private fun setShowDisEnableModsDialog(b: Boolean) {
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


}


