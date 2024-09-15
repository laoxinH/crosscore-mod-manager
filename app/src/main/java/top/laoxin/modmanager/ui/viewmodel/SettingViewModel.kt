package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.DownloadGameConfigBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.SpecialGame
import top.laoxin.modmanager.database.UserPreferencesRepository
import top.laoxin.modmanager.database.backups.BackupRepository
import top.laoxin.modmanager.database.mods.ModRepository
import top.laoxin.modmanager.network.ModManagerApi
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.ui.state.SettingUiState


class SettingViewModel(
    private val backupRepository: BackupRepository,
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App)
                SettingViewModel(
                    application.container.backupRepository,
                    application.container.modRepository,
                    application.userPreferencesRepository,
                )
            }
        }
        var gameInfoJob: Job? = null
    }

    private var _requestPermissionPath by mutableStateOf("")
    val requestPermissionPath get() = _requestPermissionPath
    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState = _uiState.asStateFlow()
    private var _gameInfo = mutableStateOf(GameInfoConstant.gameInfoList[0])
    val gameInfo get() = _gameInfo.value

    // 更新描述
    private var _updateDescription by mutableStateOf("")
    val updateDescription get() = _updateDescription

    // 下载地址
    private var _downloadUrl by mutableStateOf("")

    val downloadUrl get() = _downloadUrl

    init {

        viewModelScope.launch(Dispatchers.Main.immediate) {
            userPreferencesRepository.getPreferenceFlow("SELECTED_GAME", 0).collect {
                _gameInfo.value = GameInfoConstant.gameInfoList[it]
            }
        }
        getGameInfo()
        getVersionName()
    }

    // 删除所有备份
    fun deleteAllBackups() {
        gameInfoJob?.cancel()
        setDeleteBackupDialog(false)
        gameInfoJob = viewModelScope.launch(Dispatchers.IO) {
            modRepository.getEnableMods(_gameInfo.value.packageName).collect {
                if (it.isNotEmpty()) {
                    // 如果有mod开启则提示
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(R.string.toast_del_buckup_when_mod_enable)
                    }
                    this@launch.cancel()
                } else {
                    val delBackupFile: Boolean = ModTools.deleteBackupFiles(_gameInfo.value)
                    if (delBackupFile) {
                        backupRepository.deleteByGamePackageName(_gameInfo.value.packageName)
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(
                                App.get().getString(
                                    R.string.toast_del_buckup_success,
                                    _gameInfo.value.gameName,
                                )
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(
                                App.get().getString(
                                    R.string.toast_del_buckup_filed,
                                    _gameInfo.value.gameName,
                                )
                            )
                        }
                    }
                }
            }
            gameInfoJob?.cancel()
        }

    }

    // 设置删除备份对话框
    fun setDeleteBackupDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(deleteBackupDialog = open)
    }

    fun deleteCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val delCache = ModTools.deleteCache()
            if (delCache) {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_cache_success)
                }
            } else {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_cache_filed)
                }
            }
        }
    }

    fun openUrl(context: Context, url: String) {
        if (url.isEmpty()) {
            return
        }
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        context.startActivity(urlIntent)
    }

    fun deleteTemp() {
        viewModelScope.launch(Dispatchers.IO) {
            val delTemp: Boolean = ModTools.deleteTempFile()
            if (delTemp) {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_temp_success)
                }
            } else {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_temp_filed)
                }
            }
        }
    }

    fun showAcknowledgments(b: Boolean) {
        if (b) {
            viewModelScope.launch(Dispatchers.IO) {
                kotlin.runCatching {
                    ModManagerApi.retrofitService.getThinksList()
                }.onFailure {
                    Log.e("SettingViewModel", "showAcknowledgments: $it")
                    ToastUtils.longCall("获取感谢名单失败")
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(thinksList = it)
                        _uiState.value = _uiState.value.copy(showAcknowledgments = b)
                    }
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(showAcknowledgments = b)
        }
    }

    fun showSwitchGame(b: Boolean) {
        _uiState.value = _uiState.value.copy(showSwitchGame = b)
    }

    // 设置游戏服务器
    fun setGameServer(serviceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.savePreference("GAME_SERVICE", serviceName)
        }
    }

    // 设置安装位置
    fun setInstallPath(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.savePreference("INSTALL_PATH", path)
        }
    }

    // 设置gameInfoList
    fun setGameInfoList(gameInfoList: List<GameInfoBean>) {
        _uiState.value = _uiState.value.copy(gameInfoList = gameInfoList)
    }

    // 通过名称软件包名获取软件包信息
    private fun getGameInfo() {
        setGameInfoList(GameInfoConstant.gameInfoList)
    }

    //设置游戏信息
    fun setGameInfo(gameInfo: GameInfoBean, isTips: Boolean = false) {
        if (gameInfo.tips.isNotEmpty() && !isTips) {
            _uiState.update {
                it.copy(showGameTipsDialog = true)
            }
            _gameInfo.value = gameInfo
        } else {
            if (PermissionTools.checkPermission(gameInfo.gamePath) == PathType.NULL) {
                _requestPermissionPath = PermissionTools.getRequestPermissionPath(gameInfo.gamePath)
                _uiState.update {
                    it.copy(openPermissionRequestDialog = true)
                }
                return
            }
            confirmGameInfo(gameInfo)
        }


    }

    private fun confirmGameInfo(gameInfo: GameInfoBean) {
        try {
            App.get().packageManager.getPackageInfo(gameInfo.packageName, 0)
            viewModelScope.launch(Dispatchers.IO) {
                userPreferencesRepository.savePreference(
                    "SELECTED_GAME",
                    GameInfoConstant.gameInfoList.indexOf(gameInfo)
                )
                SpecialGame.entries.forEach {
                    if (gameInfo.packageName.contains(it.packageName)) {
                        Log.d("SettingViewModel", "执行特殊选择: $it")
                        it.baseSpecialGameTools.specialOperationSelectGame(gameInfo)
                    }
                }
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_setect_game_success,
                            gameInfo.gameName,
                            gameInfo.serviceName
                        )
                    )
                    showSwitchGame(false)
                }

            }
        } catch (e: Exception) {
            ToastUtils.longCall(
                App.get().getString(
                    R.string.toast_set_game_info_failed,
                    gameInfo.gameName,
                    gameInfo.serviceName
                )
            )

            e.printStackTrace()
        }
    }

    fun flashGameConfig() {
        gameInfoJob?.cancel()
        gameInfoJob = viewModelScope.launch {
            userPreferencesRepository.getPreferenceFlow(
                "SELECTED_DIRECTORY",
                ModTools.DOWNLOAD_MOD_PATH
            ).collectLatest {
                ModTools.readGameConfig(ModTools.ROOT_PATH + it)
                ModTools.updateGameConfig()
                Log.d("设置测试", "切换目录执行")
                gameInfoJob?.cancel()
            }

        }
    }

    // 检测更新
    fun checkUpdate() {
        viewModelScope.launch {
            kotlin.runCatching {
                ModManagerApi.retrofitService.getUpdate()
            }.onFailure {
                Log.e("SettingViewModel", "checkUpdate: $it")
            }.onSuccess {
                Log.d("SettingViewModel", "checkUpdate: $it")
                if (it.code > ModTools.getVersionCode()) {
                    _downloadUrl = it.url
                    _updateDescription = it.des
                    setShowUpgradeDialog(true)
                } else {
                    ToastUtils.longCall(R.string.toast_no_update)
                }
            }
        }
    }

    // 设置更新弹窗
    fun setShowUpgradeDialog(b: Boolean) {
        _uiState.update {
            it.copy(showUpdateDialog = b)
        }
    }

    // 设置版本号
    fun setVersionName(versionName: String) {
        _uiState.update {
            it.copy(versionName = versionName)
        }
    }

    // 获取版本号
    fun getVersionName() {
        viewModelScope.launch {
            ModTools.getVersionName()?.let { setVersionName(it) }
        }
    }

    fun setShowDownloadGameConfig(b: Boolean) {
        if (b) {
            viewModelScope.launch(Dispatchers.IO) {
                kotlin.runCatching {
                    val gameConfigs = ModManagerApi.retrofitService.getGameConfigs()
                    Log.d("SettingViewModel", "getGameConfigs: $gameConfigs")
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                downloadGameConfigList = gameConfigs,
                                showDownloadGameConfigDialog = b
                            )
                        }
                    }

                }.onFailure {
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(R.string.toast_get_game_config_failed)
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(showDownloadGameConfigDialog = b)
            }
        }
    }

    fun setShowGameTipsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showGameTipsDialog = b)
        }
    }

    fun downloadGameConfig(downloadGameConfigBean: DownloadGameConfigBean) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val downloadGameConfig =
                    ModManagerApi.retrofitService.downloadGameConfig(downloadGameConfigBean.packageName)
                ModTools.writeGameConfigFile(downloadGameConfig)
            }.onFailure {
                Log.e("SettingViewModel", "downloadGameConfig: $it")
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_download_game_config_failed)
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_download_game_config_success)
                    // flashGameConfig()
                    ModTools.updateGameConfig()
                }

            }
        }
    }

    fun requestShizukuPermission() {
        if (PermissionTools.isShizukuAvailable) {
            if (PermissionTools.hasShizukuPermission()) {
                PermissionTools.checkShizukuPermission()
            } else {
                PermissionTools.requestShizukuPermission()
            }
        } else {
            ToastUtils.longCall(R.string.toast_shizuku_not_available)
        }
    }

    fun setOpenPermissionRequestDialog(b: Boolean) {
        _uiState.update {
            it.copy(openPermissionRequestDialog = b)
        }
    }

    fun checkInformation() {
        viewModelScope.launch {
            kotlin.runCatching {
                ModManagerApi.retrofitService.getInfo()
            }.onFailure {
                Log.e("SettingViewModel", "checkInformation: $it")
            }.onSuccess { info ->
                _uiState.update {
                    it.copy(infoBean = info)
                }
                setShowInfoDialog(true)
            }
        }

    }

    // 显示信息弹窗
    fun setShowInfoDialog(b: Boolean) {
        _uiState.update {
            it.copy(showNotificationDialog = b)
        }
    }


}