package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.ModPath
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.data.UserPreferencesRepository
import top.laoxin.modmanager.data.mods.ModRepository
import top.laoxin.modmanager.tools.FileTools
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils


class ConsoleViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modRepository: ModRepository
) : ViewModel() {


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as App)
                ConsoleViewModel(
                    application.userPreferencesRepository,
                    application.container.modRepository
                )
            }
        }
        var gameInfoJob: Job? = null
    }

    // private val _uiState = MutableStateFlow(ConsoleUiState())
    //val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()
    // 使用热数据流读取用户设置并实时更新
    private val antiHarmonyFlow = userPreferencesRepository.getPreferenceFlow("ANTI_HARMONY", false)
    private val scanQQDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_QQ_DIRECTORY", false)
    private val selectedDirectoryFlow =
        userPreferencesRepository.getPreferenceFlow(
            "SELECTED_DIRECTORY",
            ModTools.DOWNLOAD_MOD_PATH_JC
        )
    private val scanDownloadFlow =
        userPreferencesRepository.getPreferenceFlow("SCAN_DOWNLOAD", false)
    private val openPermissionRequestDialogFlow =
        userPreferencesRepository.getPreferenceFlow("OPEN_PERMISSION_REQUEST_DIALOG", false)

    // 安装位置
    private val installPath = userPreferencesRepository.getPreferenceFlow("INSTALL_PATH", "")

    // mod数量
    private val modCountFlow = modRepository.getModCount()

    // 已开启mod数量
    private val enableModCountFlow = modRepository.getEnableModCount()

    // 游戏服务器
    private val gameServiceFlow = userPreferencesRepository.getPreferenceFlow("GAME_SERVICE", "")

    private val flowList = listOf(

        antiHarmonyFlow,
        scanQQDirectoryFlow,
        selectedDirectoryFlow,
        scanDownloadFlow,
        openPermissionRequestDialogFlow,
        modCountFlow,
        enableModCountFlow,

    )
    val uiState = combine(flowList) { values ->
        ConsoleUiState(
            antiHarmony = values[0] as Boolean,
            scanQQDirectory = values[1] as Boolean,
            selectedDirectory = values[2] as String,
            scanDownload = values[3] as Boolean,
            openPermissionRequestDialog = values[4] as Boolean,
            modCount = values[5] as Int,
            enableModCount = values[6] as Int,

        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConsoleUiState()
    )


    // 是否可以安装mod
    private var _canInstallMod = false
    val canInstallMod: Boolean
        get() = _canInstallMod

    private lateinit var _gameInfo: GameInfo
    val gameInfo: GameInfo
        get() = _gameInfo

    private var _requestPermissionPath by mutableStateOf("")
    val requestPermissionPath: String
        get() = _requestPermissionPath

    init {
        getGameInfo()
        gameInfoJob = viewModelScope.launch {
            installPath.collect{
                if (it.isEmpty()) {
                    _canInstallMod = false
                }else if (PermissionTools.checkPermission(it) != PathType.NULL) {
                    _canInstallMod = true
                }
                gameInfoJob?.cancel()
            }
        }
    }


    private fun setAntiHarmony(antiHarmony: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "ANTI_HARMONY",
                antiHarmony
            )
        }

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
            /*var path = selectedDirectory.subSequence(14, selectedDirectory.length)
            path = "/$path/"*/
            userPreferencesRepository.savePreference(
                "SELECTED_DIRECTORY",
                "/$selectedDirectory/"
            )
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

    // 设置安装位置
    private fun setInstallPath(installPath: String) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "INSTALL_PATH",
                installPath
            )
        }
    }

    // 设置游戏服务器
    private fun setGameService(gameService: String) {
        viewModelScope.launch {
            userPreferencesRepository.savePreference(
                "GAME_SERVICE",
                gameService
            )
        }
    }
    // 设置请求权路径
    fun setRequestPermissionPath(path: String) {
        Log.d("ConsoleViewModel", "setRequestPermissionPath: $path")
        _requestPermissionPath = path
    }

    fun getPermissionShizuku(
        messageOk: String,
        messageNo: String,
        messageNoShizuku: String,
    ) {
        this.setOpenPermissionRequestDialog(false)
        if (PermissionTools.isShizukuAvailable) {
            PermissionTools.requestShizukuPermission()
        } else {
            ToastUtils.longCall(messageNoShizuku)
            //this.setScanQQDirectory(false)

        }
    }


    // 检查shizuku权限
    private fun checkShizukuPermission(): Boolean {
        return PermissionTools.checkShizukuPermission()
    }

    // 通过名称软件包名获取软件包信息
    private fun getGameInfo() {
        gameInfoJob?.cancel()
        _gameInfo = GameInfo(
            "未安装",
            "未安装",
            "未安装",
            "未安装",
            (App.get()
                .getDrawable(top.laoxin.modmanager.R.drawable.app_icon) as BitmapDrawable).bitmap.asImageBitmap()
        )
        gameInfoJob = viewModelScope.launch(Dispatchers.IO) {

            gameServiceFlow.collect { gameService ->
                Log.d("ConsoleViewModel", "getGameInfo: $gameService")
                if (gameService.isNotEmpty()) {
                    GameInfoConstant.entries.forEach {
                        if (it.serviceName == gameService) {
                            val packageName = it.packageName
                            try {
                                val packageInfo =
                                    App.get().packageManager.getPackageInfo(packageName, 0)
                                if (packageInfo != null) {
                                    withContext(Dispatchers.Main) {
                                        _gameInfo = GameInfo(
                                            it.gameName,
                                            packageName,
                                            packageInfo.versionName ?: "未知",
                                            it.serviceName,
                                            getGameIcon(packageInfo)    // 获取游戏图标
                                        )
                                    }
                                }
                            } catch (e: PackageManager.NameNotFoundException) {
                                e.printStackTrace()
                            }
                            setInstallPath(it.gamePath)
                        }
                    }
                } else {
                    GameInfoConstant.entries.forEach {
                        val packageName = it.packageName
                        try {
                            val packageInfo =
                                App.get().packageManager.getPackageInfo(packageName, 0)
                            if (packageInfo != null) {
                                withContext(Dispatchers.Main) {
                                    _gameInfo = GameInfo(
                                        it.gameName,
                                        packageName,
                                        packageInfo.versionName ?: "未知",
                                        it.serviceName,
                                        getGameIcon(packageInfo)
                                    )
                                }
                                setInstallPath(it.gamePath)
                                setGameService(it.serviceName)
                            }
                        } catch (e: PackageManager.NameNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }
                //gameInfoJob?.cancel()
            }
        }
    }

    // 开启扫描QQ目录
    fun openScanQQDirectoryDialog(b: Boolean) {
        when (App.osVersion) {
            OSVersion.OS_11 -> {
                setRequestPermissionPath(ModPath.ANDROID_DATA)
            }
            OSVersion.OS_13 -> {
                setRequestPermissionPath(PermissionTools.getRequestPermissionPath(ModPath.MOD_PATH_QQ))
            }
            else -> {
                setRequestPermissionPath(ModPath.MOD_PATH_QQ)
            }
        }
        if (PermissionTools.checkPermission(ModPath.MOD_PATH_QQ) != PathType.NULL){
            setScanQQDirectory(b)
            FileTools.getFileListByFile(ModTools.MY_APP_PATH)

        } else {
            setOpenPermissionRequestDialog(true)
        }
    }

    // 开启反和谐
    fun openAntiHarmony(flag: Boolean) {
        viewModelScope.launch {
            val path = "${ModTools.ROOT_PATH}/Android/data/${gameInfo.packageName}/files/"
            when (App.osVersion) {
                OSVersion.OS_11 -> {
                    setRequestPermissionPath(ModPath.ANDROID_DATA)
                }
                OSVersion.OS_13 -> {
                    setRequestPermissionPath(PermissionTools.getRequestPermissionPath(path))
                }
                else -> {
                    setRequestPermissionPath(path)
                }
            }
            val pathType = PermissionTools.checkPermission(path)
            if (pathType != PathType.NULL) {
                if (flag) {
                    setAntiHarmony(true)
                    ModTools.writeAntiHarmonyFile("1", gameInfo.packageName,pathType)
                } else {
                    setAntiHarmony(false)
                    ModTools.writeAntiHarmonyFile("0",gameInfo.packageName ,pathType)
                }
            } else {
                setOpenPermissionRequestDialog(true)
            }
        }
    }

    // 开启扫描下载目录
    fun openScanDownloadDirectoryDialog(b: Boolean) {
        setScanDownload(b)
    }




    fun openUrl(context: Context, url: String) {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        context.startActivity(urlIntent)
    }

    // 通过包名读取应用信息
    private fun getGameIcon(packageInfo: PackageInfo): ImageBitmap {
        val drawable = packageInfo.applicationInfo.loadIcon(App.get().packageManager)
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

            else -> throw IllegalArgumentException("Unsupported drawable type")
        }
        return bitmap.asImageBitmap()

    }
}



