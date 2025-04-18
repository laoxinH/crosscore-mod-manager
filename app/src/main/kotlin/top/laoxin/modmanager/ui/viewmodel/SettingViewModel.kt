package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.DownloadGameConfigBean
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.domain.usercase.app.CheckUpdateUserCase
import top.laoxin.modmanager.domain.usercase.setting.DeleteBackupUserCase
import top.laoxin.modmanager.domain.usercase.setting.DeleteCacheUserCase
import top.laoxin.modmanager.domain.usercase.setting.DeleteTempUserCase
import top.laoxin.modmanager.domain.usercase.setting.DownloadGameConfigUserCase
import top.laoxin.modmanager.domain.usercase.setting.FlashGameConfigUserCase
import top.laoxin.modmanager.domain.usercase.setting.SelectGameUserCase
import top.laoxin.modmanager.tools.AppInfoTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.ui.state.SettingUiState
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val deleteBackupUserCase: DeleteBackupUserCase,
    private val deleteCacheUserCase: DeleteCacheUserCase,
    private val deleteTempUserCase: DeleteTempUserCase,
    private val selectGameUserCase: SelectGameUserCase,
    private val flashGameConfigUserCase: FlashGameConfigUserCase,
    private val permissionTools: PermissionTools,
    private val gameInfoManager: GameInfoManager,
    private val appInfoManager: AppInfoTools,
    private val downloadGameConfigUserCase: DownloadGameConfigUserCase,
    private val checkUpdateUserCase: CheckUpdateUserCase,
    private val fileToolsManager: FileToolsManager
) : ViewModel() {
    companion object {
        var gameInfoJob: Job? = null
    }

    private var _requestPermissionPath by mutableStateOf("")
    val requestPermissionPath get() = _requestPermissionPath
    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState = _uiState.asStateFlow()
    private var _gameInfo = mutableStateOf(GameInfoConstant.gameInfoList[0])
    val gameInfo get() = _gameInfo.value

    // 下载地址
    private var _downloadUrl: String? by mutableStateOf("")
    val downloadUrl: String?
        get() = _downloadUrl

    private var _universalUrl: String? by mutableStateOf("")
    val universalUrl: String?
        get() = _universalUrl

    // 更新内容
    private var _updateContent: String? by mutableStateOf("")
    val updateContent: String?
        get() = _updateContent

    init {

        loadGameInfo()
        getVersionName()
    }

    // 删除所有备份
    fun deleteAllBackups() {
        gameInfoJob?.cancel()
        setDeleteBackupDialog(false)
        gameInfoJob = viewModelScope.launch {
            val (code, name) = deleteBackupUserCase()
            when (code) {
                ResultCode.NO_SELECTED_GAME -> {
                    ToastUtils.longCall(R.string.toast_please_select_game)
                }

                ResultCode.HAVE_ENABLE_MODS -> {
                    ToastUtils.longCall(R.string.toast_del_buckup_when_mod_enable)
                }

                ResultCode.SUCCESS -> {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_del_buckup_success,
                            name
                        )
                    )
                }

                ResultCode.FAIL -> {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_del_buckup_failed,
                            name
                        )
                    )
                }
            }
            gameInfoJob?.cancel()
        }

    }

    // 设置删除备份对话框
    fun setDeleteBackupDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(deleteBackupDialog = open)
    }

    // 设置删除备份对话框
    fun setDeleteCacheDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(deleteCacheDialog = open)
    }

    fun deleteCache() {
        setDeleteCacheDialog(false)
        viewModelScope.launch {
            val delCache = deleteCacheUserCase()
            if (delCache) {
                ToastUtils.longCall(R.string.toast_del_cache_success)
            } else {
                ToastUtils.longCall(R.string.toast_del_cache_filed)
            }
        }
    }

    fun openUrl(context: Context, url: String) {
        if (url.isEmpty()) {
            return
        }
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            url.toUri()
        )
        context.startActivity(urlIntent)
    }

    fun deleteTemp() {
        viewModelScope.launch {
            val delTemp: Boolean = deleteTempUserCase()
            if (delTemp) {
                ToastUtils.longCall(R.string.toast_del_temp_success)
            } else {
                ToastUtils.longCall(R.string.toast_del_temp_filed)
            }
        }
    }

    fun showAcknowledgments(b: Boolean) {
        if (b) {
            viewModelScope.launch(Dispatchers.IO) {
                kotlin.runCatching {
                    ModManagerApi.retrofitService.getThanksList()
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
            _uiState.value = _uiState.value.copy(showAcknowledgments = false)
        }
    }

    fun showSwitchGame(b: Boolean) {
        _uiState.value = _uiState.value.copy(showSwitchGame = b)
    }


    // 通过名称软件包名获取软件包信息
    private fun loadGameInfo() {
        _uiState.value = _uiState.value.copy(gameInfoList = gameInfoManager.getGameInfoList())
    }

    //设置游戏信息
    fun setGameInfo(gameInfo: GameInfoBean, isTips: Boolean = false) {
        _gameInfo.value = gameInfo
        if (gameInfo.tips.isNotEmpty() && !isTips) {
            _uiState.update {
                it.copy(showGameTipsDialog = true)
            }
        } else {
            if (permissionTools.checkPermission(gameInfo.gamePath) == PathType.NULL) {
                _requestPermissionPath =
                    permissionTools.getRequestPermissionPath(gameInfo.gamePath)
                _uiState.update {
                    it.copy(openPermissionRequestDialog = true)
                }
                return
            }
            confirmGameInfo(gameInfo)
        }


    }

    private fun confirmGameInfo(gameInfo: GameInfoBean) {

        viewModelScope.launch {
            if (selectGameUserCase(gameInfo)) {
                ToastUtils.longCall(
                    App.get().getString(
                        R.string.toast_setect_game_success,
                        gameInfo.gameName,
                        gameInfo.serviceName
                    )
                )
                showSwitchGame(false)
            } else {
                ToastUtils.longCall(
                    App.get().getString(
                        R.string.toast_set_game_info_failed,
                        gameInfo.gameName,
                        gameInfo.serviceName
                    )
                )
            }

        }
    }

    fun flashGameConfig() {
        gameInfoJob?.cancel()
        gameInfoJob = viewModelScope.launch {
            flashGameConfigUserCase()

        }
    }

    fun checkUpdate() {
        viewModelScope.launch {
            val update = checkUpdateUserCase(BuildConfig.VERSION_NAME)
            if (update.third) {
                _downloadUrl = update.first[0]
                _universalUrl = update.first[1]
                _updateContent = update.second[0]
                setShowUpgradeDialog(true)
            } else {
                ToastUtils.longCall(R.string.toast_no_update)
            }
        }
    }

    // 设置更新弹窗
    fun setShowUpgradeDialog(b: Boolean) {
        _uiState.update {
            it.copy(showUpdateDialog = b)
        }
    }


    // 获取版本号
    fun getVersionName() {
        viewModelScope.launch {
            val version = appInfoManager.getVersionName(appInfoManager.getPackageName())
            _uiState.update {
                it.copy(versionName = version)
            }
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
                it.copy(showDownloadGameConfigDialog = false)
            }
        }
    }

    fun setShowGameTipsDialog(b: Boolean) {
        _uiState.update {
            it.copy(showGameTipsDialog = b)
        }
    }

    fun downloadGameConfig(downloadGameConfigBean: DownloadGameConfigBean) {
        viewModelScope.launch {
            downloadGameConfigUserCase(downloadGameConfigBean)
            ToastUtils.longCall(R.string.toast_download_game_config_success)
        }
    }

    fun requestShizukuPermission() {
        if (permissionTools.isShizukuAvailable) {
            if (permissionTools.hasShizukuPermission()) {
                permissionTools.checkShizukuPermission()
            } else {
                permissionTools.requestShizukuPermission()
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

    fun getPermissionTools(): PermissionTools {
        return permissionTools
    }

    fun getFileToolsManager(): FileToolsManager {
        return fileToolsManager
    }

    fun setAboutPage(b: Boolean) {
        _uiState.update {
            it.copy(showAbout = b)
        }
    }

    fun getAboutPage(): Boolean {
        return _uiState.value.showAbout
    }

}