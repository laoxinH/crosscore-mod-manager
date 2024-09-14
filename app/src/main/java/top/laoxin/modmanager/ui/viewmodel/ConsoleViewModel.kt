package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.FileObserver
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.AntiHarmonyBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.constant.SpecialGame
import top.laoxin.modmanager.database.UserPreferencesRepository
import top.laoxin.modmanager.database.antiHarmony.AntiHarmonyRepository
import top.laoxin.modmanager.database.mods.ModRepository
import top.laoxin.modmanager.network.ModManagerApi
import top.laoxin.modmanager.observer.FlashModsObserver
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.ui.state.ConsoleUiState
import top.laoxin.modmanager.ui.state.UserPreferencesState
import top.laoxin.modmanager.userservice.gamestart.ProjectSnowStartService
import java.io.File


class ConsoleViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modRepository: ModRepository,
    private val antiHarmonyRepository: AntiHarmonyRepository
) : ViewModel() {

    // 请求权限路径
    private var _requestPermissionPath by mutableStateOf("")

    // 下载地址
    private var _downloadUrl by mutableStateOf("")
    val downloadUrl: String
        get() = _downloadUrl
    val requestPermissionPath: String
        get() = _requestPermissionPath

    // 更新类容
    private var _updateContent by mutableStateOf("")
    val updateContent: String
        get() = _updateContent


    private val _uiState = MutableStateFlow<ConsoleUiState>(ConsoleUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as App)
                ConsoleViewModel(
                    application.userPreferencesRepository,
                    application.container.modRepository,
                    application.container.antiHarmonyRepository
                )
            }
        }
        var gameInfoJob: Job? = null
        var updateModCountJob: Job? = null
        var updateAntiHarmonyJob: Job? = null
        var updateEnableModCountJob: Job? = null
        var fileObserver: FileObserver? = null
    }


    // 选择的游戏
    private val selectedGameFlow = userPreferencesRepository.getPreferenceFlow("SELECTED_GAME", 0)

    private val scanQQDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_QQ_DIRECTORY", false)
    private val selectedDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow(
            "SELECTED_DIRECTORY",
            ModTools.DOWNLOAD_MOD_PATH
        )
    private val scanDownloadFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_DOWNLOAD", false)
    private val openPermissionRequestDialogFlow =
        userPreferencesRepository.getPreferenceFlow("OPEN_PERMISSION_REQUEST_DIALOG", false)
    private val scanDirectoryModsFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_DIRECTORY_MODS", false)
    private val delUnzipDictionaryFlow =
        userPreferencesRepository.getPreferenceFlow("DELETE_UNZIP_DIRECTORY", false)
    private val userPreferencesState = combine(
        selectedGameFlow,
        selectedDirectoryFlow
    ) { selectedGame, selectedDirectory ->
        UserPreferencesState(
            selectedGameIndex = selectedGame,
            selectedDirectory = selectedDirectory
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferencesState()
    )


    val uiState = combine(
        scanQQDirectoryFlow,
        selectedDirectoryFlow,
        scanDownloadFlow,
        openPermissionRequestDialogFlow,
        scanDirectoryModsFlow,
        delUnzipDictionaryFlow,
        _uiState
    ) { values ->
        (values[6] as ConsoleUiState).copy(
            scanQQDirectory = values[0] as Boolean,
            selectedDirectory = values[1] as String,
            scanDownload = values[2] as Boolean,
            openPermissionRequestDialog = values[3] as Boolean,
            scanDirectoryMods = values[4] as Boolean,
            delUnzipDictionary = values[5] as Boolean,
        )

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConsoleUiState()
    )

    init {
        checkUpdate()
        getNewInfo()
        ModTools.updateGameConfig()
        viewModelScope.launch {
            userPreferencesState.collectLatest {
                updateLogTools(it)
                updateGameInfo(it)
                checkInstallMod()
                updateModCount()
                updateAntiHarmony()
                updateEnableModCount()
                openDictionaryObserver(it)
            }
        }
    }

    private fun getNewInfo() {
        viewModelScope.launch {
            kotlin.runCatching {
                ModManagerApi.retrofitService.getInfo()
            }.onFailure {
                Log.e("ConsoleViewModel", "信息提示: $it")
            }.onSuccess { info ->
                Log.d("ConsoleViewModel", "信息提示: ${info}")
                if (info.version > ModTools.getInfoVersion()) {
                    _uiState.update {
                        it.copy(infoBean = info)
                    }
                    setShowInfoDialog(true)
                    ModTools.setInfoVersion(info.version)
                }
            }
        }
    }

    fun setShowInfoDialog(b: Boolean) {
        _uiState.update {
            it.copy(showInfoDialog = b)
        }

    }

    private fun updateAntiHarmony() {
        val gameInfo = getGameInfo()
        updateAntiHarmonyJob?.cancel()
        updateAntiHarmonyJob = viewModelScope.launch {
            antiHarmonyRepository.getByGamePackageName(gameInfo.packageName)
                .collectLatest { antiHarmonyBean ->
                    if (antiHarmonyBean == null) {
                        antiHarmonyRepository.insert(
                            AntiHarmonyBean(
                                gamePackageName = gameInfo.packageName,
                                isEnable = false
                            )
                        )
                        _uiState.update {
                            it.copy(antiHarmony = false)
                        }
                    } else {
                        _uiState.update {
                            it.copy(antiHarmony = antiHarmonyBean.isEnable)
                        }
                    }
                }
        }

    }

    private fun openDictionaryObserver(userPreferencesState: UserPreferencesState) {
        fileObserver?.stopWatching()

        fileObserver =
            FlashModsObserver(ModTools.ROOT_PATH + userPreferencesState.selectedDirectory)
        fileObserver?.startWatching()
    }

    private fun setScanQQDirectory(scanQQDirectory: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "SCAN_QQ_DIRECTORY",
                scanQQDirectory
            )
        }
    }


    fun setSelectedDirectory(selectedDirectory: String) {
        viewModelScope.launch {
            try {
                val file = File(
                    (ModTools.ROOT_PATH + "/$selectedDirectory/" + ModTools.GAME_CONFIG).replace(
                        "tree",
                        ""
                    ).replace("//", "/")
                )
                if (!file.absolutePath.contains("${ModTools.ROOT_PATH}/Android")) {
                    file.mkdirs()
                    userPreferencesRepository.savePreference(
                        "SELECTED_DIRECTORY",
                        "/$selectedDirectory/".replace("tree", "").replace("//", "/")
                    )
                } else {
                    ToastUtils.longCall(R.string.toast_this_dir_has_no_prim_android)
                }
            } catch (e: Exception) {
                ToastUtils.longCall(R.string.toast_this_dir_has_no_prim)
                Log.e("ConsoleViewModel", "setSelectedDirectory: $e")
            }

        }
    }

    private fun setScanDownload(scanDownload: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "SCAN_DOWNLOAD",
                scanDownload
            )
        }
    }

    fun setOpenPermissionRequestDialog(openPermissionRequestDialog: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "OPEN_PERMISSION_REQUEST_DIALOG",
                openPermissionRequestDialog
            )
        }
    }


    // 设置扫描文件夹中的Mods
    fun setScanDirectoryMods(scanDirectoryMods: Boolean) {
        /*  if (gameInfo.isGameFileRepeat){
              ToastUtils.longCall(R.string.toast_game_config_not_suppose)
              return
          }*/
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "SCAN_DIRECTORY_MODS",
                scanDirectoryMods
            )
        }
    }

    // 设置请求权路径
    fun setRequestPermissionPath(path: String) {
        Log.d("ConsoleViewModel", "setRequestPermissionPath: $path")
        _requestPermissionPath = path
    }


    // 通过名称软件包名获取软件包信息
    private suspend fun updateGameInfo(userPreferencesState: UserPreferencesState) {
        val gameInfo = GameInfoConstant.gameInfoList[userPreferencesState.selectedGameIndex]
        ModTools.createModsDirectory(gameInfo, userPreferencesState.selectedDirectory)
        Log.d("ConsoleViewModel", "getGameInfo: $gameInfo")
        if (gameInfo.packageName.isNotEmpty()) {
            try {
                val packageInfo =
                    App.get().packageManager.getPackageInfo(gameInfo.packageName, 0)
                if (packageInfo != null) {
                    withContext(Dispatchers.Main) {
                        setGameInfo(
                            gameInfo.copy(
                                version = packageInfo.versionName ?: "未知",
                            )
                        )
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }


    // 开启扫描QQ目录
    fun openScanQQDirectoryDialog(b: Boolean) {
        setRequestPermissionPath(PermissionTools.getRequestPermissionPath(ScanModPath.MOD_PATH_QQ))
        if (PermissionTools.checkPermission(ScanModPath.MOD_PATH_QQ) != PathType.NULL) {
            setScanQQDirectory(b)
        } else {
            setOpenPermissionRequestDialog(true)
        }
    }

    // 开启反和谐
    fun openAntiHarmony(flag: Boolean) {
        val gameInfo = getGameInfo()
        if (gameInfo.antiHarmonyFile.isEmpty() || gameInfo.antiHarmonyContent.isEmpty()) {
            ToastUtils.longCall(R.string.toast_game_not_suppose_anti)
            return
        }
        val pathType = PermissionTools.checkPermission(gameInfo.gamePath)
        if (pathType == PathType.NULL) {
            setRequestPermissionPath(PermissionTools.getRequestPermissionPath(gameInfo.gamePath))
            setOpenPermissionRequestDialog(true)
            return
        } else if (pathType == PathType.DOCUMENT) {
            setRequestPermissionPath(ModTools.MY_APP_PATH)
            val myPathType = PermissionTools.checkPermission(ModTools.MY_APP_PATH)
            if (myPathType == PathType.NULL) {
                setOpenPermissionRequestDialog(true)
                return
            }
        }
        viewModelScope.launch {
            ModTools.setModsToolsSpecialPathReadType(pathType)

            if (flag) {

                val antiHarmony = ModTools.antiHarmony(gameInfo, true)
                antiHarmonyRepository.updateByGamePackageName(gameInfo.packageName, antiHarmony)
            } else {
                antiHarmonyRepository.updateByGamePackageName(gameInfo.packageName, false)
                ModTools.antiHarmony(gameInfo, false)
            }

        }
    }

    // 开启扫描下载目录
    fun openScanDownloadDirectoryDialog(b: Boolean) {
        setScanDownload(b)
    }


    // 设置showScanDirectoryModsDialog
    fun setShowScanDirectoryModsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showScanDirectoryModsDialog = b)
        }
    }


    fun openUrl(context: Context, url: String) {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        context.startActivity(urlIntent)
    }

    // 通过包名读取应用信息
    fun getGameIcon(packageName: String): ImageBitmap {
        try {
            val packageInfo = App.get().packageManager.getPackageInfo(packageName, 0)
            var drawable = packageInfo.applicationInfo?.loadIcon(App.get().packageManager)
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                is AdaptiveIconDrawable -> {
                    Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    ).also { bitmap ->
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                    }
                }

                else -> {
                    val context = App.get()
                    drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
                    drawable.toBitmap()
                }
            }
            return bitmap.asImageBitmap()
        } catch (e: PackageManager.NameNotFoundException) {
            val context = App.get()
            val drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
            val bitmap = drawable.toBitmap()
            return bitmap.asImageBitmap()
        }
    }

    fun openScanDirectoryMods(state: Boolean) {
        if (state) {
            _uiState.update {
                it.copy(showScanDirectoryModsDialog = true)
            }
        } else {
            setScanDirectoryMods(false)
        }
    }

    // 检查是否支持安装mod
    private fun checkInstallMod() {
        val gameInfo = getGameInfo()
        val checkPermission = PermissionTools.checkPermission(gameInfo.gamePath)
        if (checkPermission != PathType.NULL && gameInfo.gamePath.isNotEmpty()) {
            Log.d("ConsoleViewModel", "checkInstallMod: true")
            _uiState.update {
                it.copy(canInstallMod = true)
            }
        } else {
            _uiState.update {
                it.copy(canInstallMod = false)
            }
        }
    }

    // 更新mod数量
    private fun updateModCount() {
        val gameInfo = getGameInfo()
        updateModCountJob?.cancel()
        updateModCountJob = viewModelScope.launch {
            modRepository.getModsCountByGamePackageName(gameInfo.packageName)
                .collectLatest { count ->
                    _uiState.update {
                        it.copy(modCount = count)
                    }
                }
        }
    }

    // 更新已开启mod数量
    private fun updateEnableModCount() {
        val gameInfo = getGameInfo()
        updateEnableModCountJob?.cancel()
        updateEnableModCountJob = viewModelScope.launch {
            modRepository.getEnableModsCountByGamePackageName(gameInfo.packageName)
                .collectLatest { count ->
                    _uiState.update {
                        it.copy(enableModCount = count)
                    }
                }
        }

    }

    // 获取游戏信息
    private fun getGameInfo(): GameInfoBean {
        return _uiState.value.gameInfo
    }

    // 设置游戏信息
    private fun setGameInfo(gameInfo: GameInfoBean) {
        _uiState.update {
            it.copy(gameInfo = gameInfo)
        }
    }

    // 设置显示升级弹窗
    fun setShowUpgradeDialog(b: Boolean) {
        _uiState.update {
            it.copy(showUpgradeDialog = b)
        }
    }

    //检测软件更新
    private fun checkUpdate() {
        viewModelScope.launch {
            kotlin.runCatching {
                ModManagerApi.retrofitService.getUpdate()
            }.onFailure {
                Log.e("ConsoleViewModel", "checkUpdate: $it")
            }.onSuccess {
                if (it.code > ModTools.getVersionCode()) {
                    Log.d("ConsoleViewModel", "checkUpdate: ${it}")
                    _downloadUrl = it.url
                    _updateContent = it.des
                    setShowUpgradeDialog(true)
                }
            }
        }
    }


    fun startGameService() {
        val intent = Intent(App.get(), ProjectSnowStartService::class.java)
        intent.putExtras(
            Bundle().apply {
                putParcelable("game_info", getGameInfo())
            }
        )
        App.get().stopService(intent)
        App.get().startForegroundService(intent)
    }

    fun startGame() {
        // 通过包名获取应用信息
        val gameInfo = getGameInfo()
        val intent = App.get().packageManager.getLaunchIntentForPackage(gameInfo.packageName)
        if (intent != null) {
            for (entry in SpecialGame.entries) {
                if (gameInfo.packageName.contains(entry.packageName) && entry.needGameService) {
                    startGameService()
                }
            }
            App.get().startActivity(intent)
        } else {
            ToastUtils.longCall(R.string.toast_game_not_found)
        }
    }

    // 更新日志工具
    private fun updateLogTools(userPreferencesState: UserPreferencesState) {
        LogTools.setLogPath(ModTools.ROOT_PATH + userPreferencesState.selectedDirectory)
    }

    fun setShowDelUnzipDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDeleteUnzipDialog = b)
        }
    }

    fun openDelUnzipDialog(it: Boolean) {
        if (it && !_uiState.value.showDeleteUnzipDialog) {
            setShowDelUnzipDialog(true)
        } else {
            viewModelScope.launch {
                userPreferencesRepository.savePreference("DELETE_UNZIP_DIRECTORY", it)
            }
        }
    }
}



