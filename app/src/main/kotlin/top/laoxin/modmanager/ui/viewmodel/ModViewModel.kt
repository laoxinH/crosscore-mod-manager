package top.laoxin.modmanager.ui.viewmodel

import android.os.Build
import android.os.FileObserver
import android.util.Log
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ScanFileBean
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.database.UserPreferencesRepository
import top.laoxin.modmanager.database.backups.BackupRepository
import top.laoxin.modmanager.database.mods.ModRepository
import top.laoxin.modmanager.database.sacnFile.ScanFileRepository
import top.laoxin.modmanager.exception.NoSelectedGameException
import top.laoxin.modmanager.exception.PasswordErrorException
import top.laoxin.modmanager.exception.PermissionsException
import top.laoxin.modmanager.listener.ProgressUpdateListener
import top.laoxin.modmanager.observer.FlashModsObserver
import top.laoxin.modmanager.observer.FlashModsObserverLow
import top.laoxin.modmanager.observer.FlashObserverInterface
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.ModTools.MODS_IMAGE_PATH
import top.laoxin.modmanager.tools.ModTools.MODS_UNZIP_PATH
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.specialGameTools.BaseSpecialGameTools
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.state.UserPreferencesState
import top.laoxin.modmanager.ui.view.modView.NavigationIndex
import java.io.File

class ModViewModel(
    userPreferencesRepository: UserPreferencesRepository,
    private val modRepository: ModRepository,
    private val backupRepository: BackupRepository,
    private val scanFileRepository: ScanFileRepository
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App)
                ModViewModel(
                    application.userPreferencesRepository,
                    application.container.modRepository,
                    application.container.backupRepository,
                    application.container.scanFileRepository
                )
            }
        }

        var searchJob: Job? = null
        var enableJob: Job? = null
        var updateAllModsJob: Job? = null
        var updateEnableModsJob: Job? = null
        var updateDisEnableModsJob: Job? = null
        var checkPasswordJob: Job? = null
        var fileObserver: FileObserver? = null
        var flashModsJob: Job? = null
    }

    // 使用热数据流读取用户设置并实时更新
    private val scanQQDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_QQ_DIRECTORY", false)
    private val selectedDirectoryFlow = userPreferencesRepository.getPreferenceFlow(
        "SELECTED_DIRECTORY", ModTools.DOWNLOAD_MOD_PATH
    )
    private val scanDownloadFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_DOWNLOAD", false)

    // 用户提示
    private val userTips = userPreferencesRepository.getPreferenceFlow("USER_TIPS", true)

    // 选择游戏
    private val selectedGame = userPreferencesRepository.getPreferenceFlow("SELECTED_GAME", 0)

    // 扫描文件夹中的Mods
    private val scanDirectoryMods =
        userPreferencesRepository.getPreferenceFlow("SCAN_DIRECTORY_MODS", false)

    private val delUnzipDictionaryFlow =
        userPreferencesRepository.getPreferenceFlow("DELETE_UNZIP_DIRECTORY", false)

    // 展示分类视图
    private val showCategoryView =
        userPreferencesRepository.getPreferenceFlow("SHOW_CATEGORY_VIEW", false)

    // 生成用户配置对象
    private val userPreferences: StateFlow<UserPreferencesState> = combine(
        scanQQDirectoryFlow,
        selectedDirectoryFlow,
        scanDownloadFlow,
        selectedGame,
        scanDirectoryMods,
        delUnzipDictionaryFlow,
        showCategoryView
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
        userTips, _uiState
    ) { userTips, uiState ->
        uiState.copy(showUserTipsDialog = userTips)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ModUiState()
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
                updateProgressUpdateListener()
                startFileObserver(it)
                // 设置当前游戏mod目录
                updateCurrentGameModPath(it)
                // 设置当前的视图
                upDateCurrentModsView(it)
            }
        }
    }

    private fun upDateCurrentModsView(it: UserPreferencesState) {
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
        _uiState.update {
            it.copy(
                currentGameModPath = ModTools.ROOT_PATH +
                        userPreferences.selectedDirectory + _gameInfo.packageName,
                currentPath = ModTools.ROOT_PATH +
                        userPreferences.selectedDirectory + _gameInfo.packageName
            )
        }
    }

    private fun updateProgressUpdateListener() {
        ArchiveUtil.progressUpdateListener = this
        ModTools.progressUpdateListener = this
        BaseSpecialGameTools.progressUpdateListener = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FlashModsObserver.flashObserver = this
        } else {
            FlashModsObserverLow.flashObserver = this
        }
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
        fileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FlashModsObserver(ModTools.ROOT_PATH + userPreferencesState.selectedDirectory + _gameInfo.packageName)
        } else {
            FlashModsObserverLow(ModTools.ROOT_PATH + userPreferencesState.selectedDirectory + _gameInfo.packageName)
        }
        fileObserver?.startWatching()
    }


    fun flashMods(isLoading: Boolean) {
        flashModsJob?.cancel()
        kotlin.runCatching {
            //检擦是否选择游戏
            checkSelectGame()
            // 检查权限
            checkPermission(_gameInfo.gamePath)
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
        }.onSuccess {

            fileObserver?.stopWatching()
            setLoading(isLoading)
            flashModsJob = viewModelScope.launch(Dispatchers.IO) {
                kotlin.runCatching {
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall("开始扫描")
                    }
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
                        ModTools.ROOT_PATH + userPreferencesState.selectedDirectory, _gameInfo
                    )
                    // 读取scanfiles
                    var scanFiles = scanFileRepository.getAll().first()
                    // 判断scanfiles中的文件是否被删除
                    scanFiles.filter {
                        !File(it.path).exists() || File(it.path).lastModified() != it.modifyTime || File(
                            it.path
                        ).length() != it.size
                    }.forEach {
                        Log.d("ModViewModel", "文件已删除: $it")
                        scanFileRepository.delete(it)
                    }
                    scanFiles =
                        scanFiles.filter { File(it.path).exists() && File(it.path).lastModified() == it.modifyTime }

                    // 创建mods
                    val modsScan = ModTools.scanArchiveMods(
                        _gameInfo.modSavePath, _gameInfo, scanFiles, this@ModViewModel
                    ).toMutableList()
                    // 扫描文件夹中的mod文件
                    if (userPreferencesState.scanDirectoryMods) {
                        modsScan.addAll(
                            ModTools.scanDirectoryMods(
                                _gameInfo.modSavePath, _gameInfo
                            )
                        )
                    }
                    val mods = uiState.value.modList
                    // 在mods中找到和scanFiles中同路径的mod
                    val mods1 = mods.filter { mod ->
                        scanFiles.any { it.path == mod.path }
                    }
                    modsScan.addAll(mods1)

                    Log.d("ModViewModel", "已有的mods: $mods")
                    // 对比modsScan和mods，对比是否有新的mod
                    val addMods = ModTools.getNewMods(mods, modsScan)
                    Log.d("ModViewModel", "添加: $addMods")
                    // 对比modsScan和mods，删除数据库中不存在的mod
                    checkDelMods(mods, modsScan)
                    // 去除数据库mods中重复的Mod
                    modRepository.insertAll(addMods)
                    val updateMods = checkUpdateMods(mods, modsScan)
                    modRepository.updateAll(updateMods)
                    withContext(Dispatchers.Main) {
                        LogTools.logRecord(updateMods.toString())
                        ToastUtils.longCall(
                            App.get().getString(
                                R.string.toast_flash_complate,
                                addMods.size.toString(),
                                updateMods.size.toString(),
                                delModsList.size.toString()
                            )
                        )
                    }
                    setLoading(false)
                    fileObserver?.startWatching()
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(it.message.toString())
                        it.printStackTrace()
                        setLoading(false)
                    }
                    fileObserver?.startWatching()
                }
            }
        }
    }

    private suspend fun checkDelMods(
        mods: List<ModBean>,
        modsScan: MutableList<ModBean>,
    ) {
        val delMods = mutableListOf<ModBean>()
        mods.forEach {
            val mod = it.isDelete(modsScan)
            if (mod != null) {
                delMods.add(mod)
            }
        }
        Log.d("ModViewModel", "删除: $delMods")
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
        mods: List<ModBean>, modsScan: List<ModBean>
    ): MutableList<ModBean> {
        val updatedMods = mutableListOf<ModBean>()
        for (mod in mods) {
            val mod1 = mod.isUpdate(modsScan)
            if (mod1 != null) {
                updatedMods.add(mod1)
            }
        }
        Log.d("ModViewModel", "更新: $updatedMods")
        return updatedMods
    }

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
        setUnzipProgress("")
        setMultitaskingProgress("")
        kotlin.runCatching {
            checkPermission(modBean.gameModPath!!)
            checkPermission(ModTools.MY_APP_PATH)
        }.onSuccess {
            Log.d("ModViewModel", "enableMod called with: modBean = $modBean, b = $b")
            if (b) {
                enableMod(listOf(modBean))

            } else {
                disableMod(listOf(modBean), isDel)
            }
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
        }
    }

    // 打开密码输入框
    fun showPasswordDialog(b: Boolean) {
        _uiState.value = _uiState.value.copy(showPasswordDialog = b)
    }

    // 刷新mod预览图
    fun flashModImage(mods: ModBean) {
        val modBean = mods
        // 判断modbean是否包含密码
        if (modBean.isEncrypted && modBean.password == null) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (modBean.isZipFile) {
                try {
                    ArchiveUtil.extractSpecificFile(
                        modBean.path!!,
                        modBean.images.orEmpty().map {
                            it.substringAfter(MODS_IMAGE_PATH + File(modBean.path).nameWithoutExtension + "/")
                        },
                        MODS_IMAGE_PATH + File(modBean.path).nameWithoutExtension,
                        modBean.password,
                        false
                    )
                } catch (_: Exception) {
                    throw Exception(
                        App.get().getString(R.string.toast_decompression_failed)
                    )
                }
            }
        }
    }

    // 刷新readme文件
    suspend fun flashModDetail(mods: ModBean): ModBean {
        val modBean = mods
        val infoMap = mutableMapOf<String, String>()
        var path = ""

        // 判断是否需要密码
        if (modBean.isEncrypted && modBean.password == null) {
            setModDetail(modBean)
            showPasswordDialog(true)
        }

        return withContext(Dispatchers.IO) {
            // 判断是否是压缩包
            if (modBean.isZipFile) {
                try {
                    // 解压操作
                    if (!modBean.fileReadmePath.isNullOrEmpty()) {
                        ArchiveUtil.extractSpecificFile(
                            modBean.path!!,
                            listOf(modBean.fileReadmePath!!),
                            MODS_UNZIP_PATH + File(modBean.path).nameWithoutExtension,
                            modBean.password,
                            false
                        )
                    } else if (!modBean.readmePath.isNullOrEmpty()) {
                        ArchiveUtil.extractSpecificFile(
                            modBean.path!!,
                            listOf(modBean.readmePath!!),
                            MODS_UNZIP_PATH + File(modBean.path).nameWithoutExtension,
                            modBean.password,
                            false
                        )
                    }
                } catch (e: Exception) {
                    LogTools.logRecord("解压或读取文件失败: ${e.message}")
                }
            }

            // 设置文件路径
            path = getModReadmePath(modBean)

            // 读取 README 文件
            val readmeFile = File(path)
            if (readmeFile.exists()) {
                val reader = readmeFile.bufferedReader()
                val lines = reader.readLines()
                for (line in lines) {
                    val parts = line.split("：")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        infoMap[key] = value
                    }
                }
            }
            LogTools.logRecord("info:$infoMap")

            // 返回更新后的 modBean
            modBean.copy(
                name = infoMap["名称"],
                description = infoMap["描述"],
                author = infoMap["作者"],
                version = infoMap["版本"]
            )
        }
    }

    // 获取 README 路径的逻辑
    fun getModReadmePath(modBean: ModBean): String {
        return if (modBean.isZipFile) {
            if (modBean.readmePath != null) {
                MODS_UNZIP_PATH + File(modBean.path!!).nameWithoutExtension + '/' + modBean.readmePath!!
            } else if (modBean.fileReadmePath != null) {
                MODS_UNZIP_PATH + File(modBean.path!!).nameWithoutExtension + '/' + modBean.fileReadmePath!!
            } else {
                ""
            }
        } else {
            modBean.readmePath ?: modBean.fileReadmePath ?: ""
        }
    }

    fun updateMod(mod: ModBean) {
        viewModelScope.launch {
            modRepository.updateMod(mod)
        }
    }


    // 开启mod
    private fun enableMod(mods: List<ModBean>) {
        val modBean = mods[0]
        // 判断modbean是否包含密码
        if (modBean.isEncrypted && modBean.password == null) {
            // 如果包含密码，弹出密码输入框
            setModDetail(modBean)
            showPasswordDialog(true)
            return
        }
        // 成功开启的mod数量
        var successCount = 0
        // 开启失败的mod数量
        var failCount = 0
        // 开启失败的mods
        val enableFailedMods = mutableListOf<ModBean>()
        Log.d("ModViewModel", "enableMod: 开始执行开启 : $modBean")
        viewModelScope.launch(Dispatchers.IO) {
            setModSwitchEnable(false)
            setTipsText(App.get().getString(R.string.tips_open_mod))
            setShowTips(true)
            setMultitaskingProgress("0/${mods.size}")
            mods.forEachIndexed { index, modBean ->
                kotlin.runCatching {
                    val gameModPath = modBean.gameModPath!!
                    // 备份游戏文件
                    if (_gameInfo.enableBackup) {
                        setTipsText(App.get().getString(R.string.tips_backups_game_files))
                        val backups = ModTools.backupGameFiles(
                            gameModPath, modBean, _gameInfo.packageName + "/"
                        )
                        backupRepository.insertAll(backups)
                    }
                    // 解压mod压缩包
                    setTipsText(
                        "${
                            App.get().getString(R.string.tips_unzip_mod)
                        } ${modBean.name}"
                    )
                    var unZipPath: String = ""
                    if (modBean.isZipFile) {

                        val decompression = ArchiveUtil.decompression(
                            modBean.path!!,
                            MODS_UNZIP_PATH + _gameInfo.packageName + "/" + File(modBean.path).nameWithoutExtension + "/",
                            modBean.password,
                        )
                        if (decompression) {
                            unZipPath =
                                MODS_UNZIP_PATH + _gameInfo.packageName + "/" + File(
                                    modBean.path
                                ).nameWithoutExtension + "/"
                        } else {
                            throw Exception(
                                App.get().getString(R.string.toast_decompression_failed)
                            )
                        }
                    }
                    // 执行特殊操作
                    setTipsText(App.get().getString(R.string.tips_special_operation))
                    ModTools.specialOperationEnable(modBean, _gameInfo.packageName)
                    // 复制mod文件
                    setTipsText(App.get().getString(R.string.tips_copy_mod_to_game))
                    if (!ModTools.copyModFiles(modBean, gameModPath, unZipPath)) {
                        setTipsText(App.get().getString(R.string.tips_copy_mod_failed))
                        ModTools.copyModsByStream(
                            modBean.path!!, gameModPath, modBean.modFiles!!, modBean.password
                        )
                    }
                    modRepository.updateMod(modBean.copy(isEnable = true))
                    setMultitaskingProgress("${index + 1}/${mods.size}")
                    successCount++


                }.onFailure {
                    failCount++
                    enableFailedMods.add(modBean)
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(it.message)
                    }
                    ModTools.deleteTempFile()
                    LogTools.logRecord("开启mod失败:${modBean.name}--$it")
                }
            }
            if (userPreferences.value.delUnzipDictionary) {
                ModTools.deleteTempFile()
            }
            withContext(Dispatchers.Main) {
                setShowTips(false)
                setModSwitchEnable(true)
                if (enableFailedMods.isNotEmpty()) {
                    setEnableFailedMods(enableFailedMods)
                    setShowOpenFailedDialog(true)
                }
                ToastUtils.longCall(
                    App.get().getString(
                        R.string.toast_swtch_mods_result,
                        successCount.toString(),
                        failCount.toString()
                    )
                )
            }

        }
    }

    fun setShowOpenFailedDialog(b: Boolean) {
        _uiState.update {
            it.copy(showOpenFailedDialog = b)
        }
    }

    // 关闭mod
    fun disableMod(mods: List<ModBean>, isDel: Boolean) {
        enableJob?.cancel()
        enableJob = viewModelScope.launch(Dispatchers.IO) {
            setModSwitchEnable(false)
            setTipsText(App.get().getString(R.string.tips_close_mod))
            setShowTips(true)
            // 成功开启的mod数量
            var successCount = 0
            // 开启失败的mod数量
            var failCount = 0
            setMultitaskingProgress("0/${mods.size}")
            mods.forEachIndexed { index, modBean ->
                val backupBeans = backupRepository.getByModNameAndGamePackageName(
                    modBean.name!!, _gameInfo.packageName
                ).first()
                Log.d("ModViewModel", "disableMod: 开始执行关闭$backupBeans")
                kotlin.runCatching {
                    setTipsText(App.get().getString(R.string.tips_special_operation))
                    // 特殊游戏操作
                    ModTools.specialOperationDisable(
                        backupBeans, _gameInfo.packageName, modBean
                    )
                    // 还原游戏文件
                    setTipsText(App.get().getString(R.string.tips_restore_game_files))
                    ModTools.restoreGameFiles(backupBeans)
                    modRepository.updateMod(modBean.copy(isEnable = false))
                    setMultitaskingProgress("${index + 1}/${mods.size}")
                    if (isDel) {
                        _uiState.update { it ->
                            it.copy(delEnableModsList = it.delEnableModsList.filter { it.path != modBean.path })
                        }
                    }
                    successCount++
                }.onFailure {
                    failCount++
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(it.message.toString())
                    }
                    LogTools.logRecord("关闭mod失败:${modBean.name}--$it")
                    it.printStackTrace()
                }

            }
            setShowTips(false)
            setModSwitchEnable(true)
            withContext(Dispatchers.Main) {
                ToastUtils.longCall(
                    App.get().getString(
                        R.string.toast_swtch_mods_result,
                        successCount.toString(),
                        failCount.toString()
                    )
                )
            }

        }
    }

    fun checkPassword(password: String) {
        kotlin.runCatching {
            checkPermission(ModTools.MY_APP_PATH)
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
        }.onSuccess {
            checkPasswordJob?.cancel()
            checkPasswordJob = viewModelScope.launch(Dispatchers.IO) {
                setModSwitchEnable(false)
                setTipsText(App.get().getString(R.string.tips_check_password))
                setShowTips(true)
                kotlin.runCatching {
                    val modBean = _uiState.value.modDetail
                    if (modBean != null) {
                        setTipsText(
                            "${App.get().getString(R.string.tips_unzip_mod)} ${modBean.name}"
                        )
                        val unZipPath =
                            MODS_UNZIP_PATH + _gameInfo.packageName + "/" + File(modBean.path!!).nameWithoutExtension + "/"
                        val decompression = ArchiveUtil.decompression(
                            modBean.path, unZipPath, password, true
                        )
                        if (!decompression) throw PasswordErrorException(
                            App.get().getString(R.string.toast_password_error)
                        )
                        modRepository.getModsByPathAndGamePackageName(
                            modBean.path, modBean.gamePackageName!!
                        ).collect { mods ->
                            Log.d("ModViewModel", "更新mod: $mods")
                            setTipsText(App.get().getString(R.string.tips_special_operation))
                            val newModList: MutableList<ModBean> = mutableListOf()
                            mods.forEach {
                                val newMod = it.copy(password = password)
                                val mod: ModBean = ModTools.readModInfo(unZipPath, newMod)
                                newModList.add(mod)
                            }
                            modRepository.updateAll(newModList)

                            setShowTips(false)
                            setModSwitchEnable(true)
                            withContext(Dispatchers.Main) {
                                ToastUtils.longCall(R.string.toast_password_right)
                            }
                            showPasswordDialog(false)
                            checkPasswordJob?.cancel()
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        setShowTips(false)
                        setModSwitchEnable(true)
                        if (it.message?.contains("StandaloneCoroutine was cancelled") != true) {
                            if (it.message?.contains("top.laoxin.modmanager") == true) {
                                ToastUtils.longCall(R.string.toast_password_error)
                            } else {
                                ToastUtils.longCall(it.message.toString())
                            }
                        }
                        LogTools.logRecord("校验密码失败:${_uiState.value.modDetail?.name}--$it")
                        it.printStackTrace()
                        showPasswordDialog(false)
                        checkPasswordJob?.cancel()
                    }

                }
                //checkPasswordJob?.cancel()
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


    private suspend fun setTipsText(s: String) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(tipsText = s)
        }

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

// 设置用户提示
//    fun setUserTipsDialog(b: Boolean) {
//        viewModelScope.launch {
//            userPreferencesRepository.savePreference(
//                "USER_TIPS", b
//            )
//        }
//    }

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
                        modRepository.search(searchText, _gameInfo.packageName).collect {
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
    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }

    // 设置加载目录
    private suspend fun setLoadingPath(loadingPath: String) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(loadingPath = loadingPath)
        }

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
        flashMods(true)
    }

    fun setModsView(enableMods: NavigationIndex) {
        _uiState.update {
            it.copy(modsView = enableMods)
        }
    }

    fun setBackIconVisiable(b: Boolean) {
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
        setUnzipProgress("")
        setMultitaskingProgress("")
        kotlin.runCatching {
            checkPermission(modsSelected[0].gameModPath!!)
            checkPermission(ModTools.MY_APP_PATH)
        }.onSuccess {
            if (b) {
                enableMod(modsSelected)
            } else {
                disableMod(modsSelected, false)
            }
        }.onFailure {
            ToastUtils.longCall(it.message.toString())
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
            viewModelScope.launch(Dispatchers.IO) {
                fileObserver?.stopWatching()
                val delMods = modRepository.getModsByIds(_uiState.value.modsSelected).first()
                // 排除包含多个mod文件的压缩包
                val singleFileMods =
                    delMods.filter { modRepository.getModsCountByPath(it.path!!).first() == 1 }
                val singleFileDisableMods = singleFileMods.filter { !it.isEnable }
                val enableMods = singleFileMods.filter { it.isEnable }
                ModTools.deleteMods(singleFileMods)
                modRepository.deleteAll(singleFileDisableMods)
                if (enableMods.isNotEmpty()) {
                    delModsList = enableMods

                    _uiState.update {
                        it.copy(delEnableModsList = enableMods)
                    }
                    setShowDisEnableModsDialog(true)
                }
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_del_mods_success, singleFileMods.size.toString()
                        )
                    )
                }
                fileObserver?.startWatching()
                updateFiles(_currentPath)

            }
        }


    }

    fun setShowDelSelectModsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDelSelectModsDialog = b)
        }
    }

    fun refreshModDetail() {
        val modBean = _uiState.value.modDetail!!

        // 启动协程等待解压并读取完成
        viewModelScope.launch {
            // 有 README 文件
            if (modBean.readmePath != null || modBean.fileReadmePath != null) {

                val newModBean = flashModDetail(modBean)

                LogTools.logRecord("old:$modBean")
                LogTools.logRecord("new:$newModBean")

                setModDetail(newModBean)

                // 更新数据库
                modRepository.updateMod(newModBean)

                LogTools.logRecord("use:" + modRepository.getModById(modBean.id).toString())
            } else {
                // 无 README 文件
                val newModBean = modBean.copy(
                    name = modBean.path?.substringAfterLast("/")?.substringBeforeLast(".")
                            + "(${
                        modBean.virtualPaths?.removePrefix(modBean.path ?: "")?.trimStart('/')
                    })".replace("/", "|"),
                    description = App.get().getString(R.string.mod_bean_no_readme),
                    author = App.get().getString(R.string.mod_bean_no_author),
                    version = "1.0"
                )

                setModDetail(newModBean)

                // 更新数据库
                modRepository.updateMod(newModBean)

                LogTools.logRecord("use:" + modRepository.getModById(modBean.id).toString())
            }
        }
    }

    fun deleteMod() {
        if (_uiState.value.modDetail == null) return
        if (!_uiState.value.showDelModDialog) {
            setShowDelModDialog(true)
        } else {
            setShowDelModDialog(false)
            setShowModDetail(false)
            val delMod = _uiState.value.modDetail!!
            viewModelScope.launch(Dispatchers.IO) {
                fileObserver?.stopWatching()
                val delMods = modRepository.getModsByPathAndGamePackageName(
                    delMod.path!!, delMod.gamePackageName!!
                ).first()
                // 排除包含多个mod文件的压缩包
                val disableMods = delMods.filter { !it.isEnable }
                val enableMods = delMods.filter { it.isEnable }
                ModTools.deleteMods(delMods)
                modRepository.deleteAll(disableMods)
                if (enableMods.isNotEmpty()) {
                    delModsList = enableMods
                    _uiState.update {
                        it.copy(delEnableModsList = enableMods)
                    }
                    setShowDisEnableModsDialog(true)
                }
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(
                        App.get()
                            .getString(R.string.toast_del_mods_success, delMods.size.toString())
                    )
                }
                fileObserver?.startWatching()
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

    // 向scanfile表中插入数据
    fun insertScanFile(scanFile: ScanFileBean) {
        viewModelScope.launch {
            scanFileRepository.insert(scanFile)
        }
    }

}
