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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.constant.ModPath
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
            ModTools.DOWNLOAD_MOD_PATH_JC
        )
    private val scanDownloadFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_DOWNLOAD", false)

    // 安装位置
    private val installPath = userPreferencesRepository.getPreferenceFlow("INSTALL_PATH", "")

    // 游戏服务器
    private val gameService = userPreferencesRepository.getPreferenceFlow("GAME_SERVER", "")

    // 用户提示
    private val userTips = userPreferencesRepository.getPreferenceFlow("USER_TIPS", true)


    // 生成用户配置对象
    private val userPreferences: Flow<UserPreferencesState> = combine(
        scanQQDirectoryFlow, selectedDirectoryFlow, scanDownloadFlow, installPath
    ) { scanQQDirectory, selectedDirectory, scanDownload, installPath ->
        UserPreferencesState(
            scanQQDirectory = scanQQDirectory,
            selectedDirectory = selectedDirectory,
            scanDownload = scanDownload,
            installPath = installPath
        )
    }

    // 获取全部mods
    private val modsFlow = modRepository.getAllIModsStream()
    // 转化statFlow

    // 获取开启的mods
    private val enableModsFlow = modRepository.getEnableMods()

    // 获取关闭的mods
    private val disableModsFlow = modRepository.getDisableMods()

    // 搜索mods
    private val searchModsFlow = modRepository.search(_searchText.value)

    /*    // 组合到uiState
        private val uiStateFlow = combine(
            modsFlow, enableModsFlow, disableModsFlow, searchModsFlow
        ) { mods, enableMods, disableMods, searchMods ->
            ModUiState(
                modList = mods,
                enableModList = enableMods,
                disableModList = disableMods,
                searchModList = searchMods
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(10000),
            ModUiState()
        )*/

    val uiState: StateFlow<ModUiState> = combine(
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
            showUserTipsDialog = userTips
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(10000),
        _uiState.value
    )

    init {
        //checkPermission()

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
                if (it.installPath.isEmpty()) {
                    setLoading(false)
                    Log.d("ModViewModel", "flashMods: installPath 为空")
                    this@launch.cancel()
                }
                Log.d("ModViewModel", "flashMods: 开始扫描: ${it.installPath}")
                val gameModPath = it.installPath + "files/Custom/"
                val requestPermissionPath = PermissionTools.getRequestPermissionPath(gameModPath)
                if (PermissionTools.checkPermission(gameModPath) == PathType.NULL) {
                    withContext(Dispatchers.Main) {
                        setRequestPermissionPath(
                            PermissionTools.getRequestPermissionPath(
                                gameModPath
                            )
                        )
                        setOpenPermissionRequestDialog(true)
                    }
                    setLoading(false)
                    this@launch.cancel()
                }

                var modsSelected = mutableListOf<ModBean>()
                if (it.scanQQDirectory) {
                    withContext(Dispatchers.Main) {
                        setLoadingPath(ModPath.MOD_PATH_QQ)
                    }
                    pathType = PermissionTools.checkPermission(ModPath.MOD_PATH_QQ)
                    if (pathType == PathType.NULL) {
                        withContext(Dispatchers.Main) {
                            setRequestPermissionPath(
                                PermissionTools.getRequestPermissionPath(
                                    ModPath.MOD_PATH_QQ
                                )
                            )
                            setOpenPermissionRequestDialog(true)
                        }
                        setLoading(false)
                        this@launch.cancel()
                    }
                    ModTools.scanMods(
                        ModPath.MOD_PATH_QQ,
                        gameModPath,
                        ModTools.ROOT_PATH + it.selectedDirectory,
                        pathType
                    )

                }
                if (it.scanDownload) {
                    withContext(Dispatchers.Main) {
                        setLoadingPath(ModPath.MOD_PATH_DOWNLOAD)
                    }
                    ModTools.scanDownload(
                        ModPath.MOD_PATH_DOWNLOAD,
                        gameModPath,
                        ModTools.ROOT_PATH + it.selectedDirectory,
                        PermissionTools.checkPermission(gameModPath)
                    )
                }
                withContext(Dispatchers.Main) {
                    setLoadingPath(ModTools.ROOT_PATH + it.selectedDirectory)
                }
                modsSelected =
                    ModTools.createMods(
                        ModTools.ROOT_PATH + it.selectedDirectory,
                        gameModPath,
                        ModTools.ROOT_PATH + it.selectedDirectory,
                        PermissionTools.checkPermission(gameModPath)
                    )
                        .toMutableList()
                val modsScan = mutableListOf<ModBean>()
                //modsScan.addAll(modsQQ)
                //modsScan.addAll(modsDownload)
                modsScan.addAll(modsSelected)
                // 从数据库中获取所有mods
                Log.d("ModViewModel", "扫描到的mod: $modsScan")
                modRepository.getAllIModsStream().collect { mods ->
                    Log.d("ModViewModel", "数据库中的mod: $mods")
                    // 对比modsScan和mods，对比是否有新的mod
                    val addMods = modsScan.filter { mod -> mods.none { it.path == mod.path } }
                    Log.d("ModViewModel", "添加的mod: $addMods")
                    // 对比modsScan和mods，删除数据库中不存在的mod
                    val delMods = mods.filter { mod -> modsScan.none { it.path == mod.path } }
                    Log.d("ModViewModel", "删除的mod: $delMods")
                    // 去除数据库mods中重复的Mod
                    modRepository.insertAll(addMods)
                    if (!isInit) modRepository.deleteAll(delMods)
                    setLoading(false)
                    flashModsJob?.cancel()
                }
            }
        }
    }


    private fun init() {
        /*viewModelScope.launch(Dispatchers.IO) {
            uiStateFlow.collect {
                withContext(Dispatchers.Main) {
                    setModList(it.modList)
                    setEnableMods(it.enableModList)
                    setDisableMods(it.disableModList)
                    //setSearchMods(it.searchModList)
                }
            }
        }*/
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
            modRepository.search(searchText).collect {
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
    fun switchMod(modBean: ModBean, b: Boolean) {
        if (PermissionTools.checkPermission(ModTools.MY_APP_PATH) == PathType.NULL){

            setRequestPermissionPath(
                PermissionTools.getRequestPermissionPath(
                    ModTools.MY_APP_PATH
                )
            )
            setOpenPermissionRequestDialog(true)

            setShowTips(false)
            setModSwitchEnable(true)
            return
        }
        Log.d("ModViewModel", "enableMod called with: modBean = $modBean, b = $b")
        if (b) {
            enableMod(modBean)
        } else {
            disableMod(modBean)
        }
    }

    // 打开密码输入框
    fun showPasswordDialog(b: Boolean) {
        _uiState.value = _uiState.value.copy(showPasswordDialog = b)
    }


    // 开启mod
    private fun enableMod(modBean: ModBean) {


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
        Log.d("ModViewModel", "enableMod: 开始执行开启")
        enableJob?.cancel()
        enableJob = viewModelScope.launch(Dispatchers.IO) {
            installPath.collect { gamePath ->
                // 鉴权
                val gameModPath = gamePath + "files/Custom/"
                if (PermissionTools.checkPermission(gameModPath) == PathType.NULL) {
                    withContext(Dispatchers.Main) {
                        setRequestPermissionPath(
                            PermissionTools.getRequestPermissionPath(
                                gameModPath
                            )
                        )
                        setOpenPermissionRequestDialog(true)
                    }
                    setShowTips(false)
                    setModSwitchEnable(true)
                    this@launch.cancel()
                }



                withContext(Dispatchers.Main) {
                    setTipsText(R.string.tips_backups_game_files)
                }
                val backups = ModTools.backupGameFiles(
                    gameModPath,
                    modBean,
                    PermissionTools.checkPermission(gameModPath)
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
                // 解压缩mod文件到零时目录
                val unZipPath: String? = ZipTools.unZip(
                    modBean.path!!,
                    ModTools.MODS_UNZIP_PATH,
                    modBean.password
                )
                if (unZipPath == null) {
                    withContext(Dispatchers.Main) {
                        //setTipsText(R.string.tips_unzip_failed)
                        setShowTips(false)
                        setModSwitchEnable(true)
                        ToastUtils.longCall(R.string.toast_unzip_failed)
                        this@launch.cancel()
                    }
                }


                withContext(Dispatchers.Main) {
                    setTipsText(R.string.tips_copy_mod_to_game)
                }
                val copyMod: Boolean = ModTools.copyModFiles(
                    modBean,
                    gameModPath,
                    unZipPath!!,
                    PermissionTools.checkPermission(gameModPath)
                )
                if (!copyMod) {
                    withContext(Dispatchers.Main) {
                        setTipsText(R.string.tips_copy_mod_failed)
                        /*setShowTips(false)
                        setModSwitchEnable(true)
                        ToastUtils.longCall(R.string.toast_copy_failed)
                        this@launch.cancel()*/
                        val unZip = ModTools.copyModsByStream(
                            modBean.path,
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
    }

    // 关闭mod
    private fun disableMod(modBean: ModBean) {
        setModSwitchEnable(false)
        setTipsText(R.string.tips_close_mod)
        setShowTips(true)
        enableJob?.cancel()
        Log.d("ModViewModel", "disableMod: 开始执行关闭")
        enableJob = viewModelScope.launch(Dispatchers.IO) {
            backupRepository.getByModPath(modBean.name!!).collect { backupBeans ->
                if (PermissionTools.checkPermission(backupBeans[0]?.gamePath!!) == PathType.NULL) {
                    withContext(Dispatchers.Main) {
                        setRequestPermissionPath(
                            PermissionTools.getRequestPermissionPath(
                                backupBeans[0]?.gamePath!!
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
                    PermissionTools.checkPermission(backupBeans[0]?.gamePath!!)
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
                    ZipTools.unZip(modBean.path!!, ModTools.MODS_UNZIP_PATH, s)
                if (unZipPath == null) {
                    withContext(Dispatchers.Main) {
                        setShowTips(false)
                        setModSwitchEnable(true)
                        ToastUtils.longCall(R.string.toast_check_failed)
                        return@withContext
                    }
                } else {
                    modRepository.getByPath(modBean.path!!).collect { mods ->
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

    fun getPermissionShizuku() {
        setOpenPermissionRequestDialog(false)
        if (PermissionTools.isShizukuAvailable) {
            PermissionTools.requestShizukuPermission()
        } else {

            ToastUtils.longCall(R.string.toast_shizuku_not_available)

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
}


