package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.constant.UserPreferencesKeys
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.domain.usercase.app.CheckUpdateUserCase
import top.laoxin.modmanager.domain.usercase.app.GetInformationUserCase
import top.laoxin.modmanager.domain.usercase.app.UpdateLogToolUserCase
import top.laoxin.modmanager.domain.usercase.console.SaveSelectModDirectoryUserCase
import top.laoxin.modmanager.domain.usercase.console.SwitchAntiHarmonyUserCase
import top.laoxin.modmanager.domain.usercase.gameinfo.LoadGameConfigUserCase
import top.laoxin.modmanager.domain.usercase.gameinfo.UpdateGameInfoUserCase
import top.laoxin.modmanager.domain.usercase.repository.AddGameToAntiHarmonyUserCase
import top.laoxin.modmanager.domain.usercase.repository.GetAntiHarmonyUserCase
import top.laoxin.modmanager.domain.usercase.repository.GetGameEnableModsCountUserCase
import top.laoxin.modmanager.domain.usercase.repository.GetGameModsCountUserCase
import top.laoxin.modmanager.domain.usercase.userpreference.GetUserPreferenceUseCase
import top.laoxin.modmanager.domain.usercase.userpreference.SaveUserPreferenceUseCase
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.AppInfoTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.ui.state.ConsoleUiState
import top.laoxin.modmanager.ui.state.UserPreferencesState
import java.nio.file.Paths
import javax.inject.Inject

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    getUserPreferenceUseCase: GetUserPreferenceUseCase,
    private val checkUpdateUserCase: CheckUpdateUserCase,
    private val saveUserPreferenceUseCase: SaveUserPreferenceUseCase,
    private val getInformationUserCase: GetInformationUserCase,
    private val updateGameInfoUserCase: UpdateGameInfoUserCase,
    private val loadGameConfigUserCase: LoadGameConfigUserCase,
    private val updateLogToolUserCase: UpdateLogToolUserCase,
    private val getAntiHarmonyUserCase: GetAntiHarmonyUserCase,
    private val addGameToAntiHarmonyUserCase: AddGameToAntiHarmonyUserCase,
    private val saveSelectModDirectoryUserCase: SaveSelectModDirectoryUserCase,
    private val switchAntiHarmonyUserCase: SwitchAntiHarmonyUserCase,
    private val getGameModsCountUserCase: GetGameModsCountUserCase,
    private val getGameEnableModsCountUserCase: GetGameEnableModsCountUserCase,
    private val appPathsManager: AppPathsManager,
    private val gameInfoManager: GameInfoManager,
    private val flashModsObserverManager: FlashModsObserverManager,
    private val fileToolsManager: FileToolsManager,
    private val permissionTools: PermissionTools,
    private val appInfoTools: AppInfoTools,
) : ViewModel() {

    // 请求权限路径
    private var _requestPermissionPath by mutableStateOf("")

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


    val requestPermissionPath: String
        get() = _requestPermissionPath


    private val _uiState = MutableStateFlow<ConsoleUiState>(ConsoleUiState())

    companion object {
        var updateModCountJob: Job? = null
        var updateAntiHarmonyJob: Job? = null
        var updateEnableModCountJob: Job? = null
    }

    private val selectedGameFlow = getUserPreferenceUseCase(UserPreferencesKeys.SELECTED_GAME, 0)
    private val scanQQDirectoryFlow =
        getUserPreferenceUseCase(UserPreferencesKeys.SCAN_QQ_DIRECTORY, false)
    private val selectedDirectoryFlow = getUserPreferenceUseCase(
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


    private val userPreferencesState = combine(
        selectedGameFlow, selectedDirectoryFlow
    ) { selectedGame, selectedDirectory ->
        UserPreferencesState(
            selectedGameIndex = selectedGame, selectedDirectory = selectedDirectory
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState()
    )


    val uiState = combine(
        scanQQDirectoryFlow,
        selectedDirectoryFlow,
        scanDownloadFlow,
        openPermissionRequestDialogFlow,
        scanDirectoryModsFlow,
        delUnzipDictionaryFlow,
        showCategoryViewFlow,
        _uiState
    ) { values ->
        (values[7] as ConsoleUiState).copy(
            scanQQDirectory = values[0] as Boolean,
            selectedDirectory = values[1] as String,
            scanDownload = values[2] as Boolean,
            openPermissionRequestDialog = values[3] as Boolean,
            scanDirectoryMods = values[4] as Boolean,
            delUnzipDictionary = values[5] as Boolean,
            showCategoryView = values[6] as Boolean
        )

    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ConsoleUiState()
    )

    init {
        checkUpdate()
        getNewInfo()
        viewModelScope.launch {
            loadGameConfigUserCase()
            userPreferencesState.collectLatest {
                updateLogToolUserCase(appPathsManager.getRootPath() + it.selectedDirectory)
                updateGameInfo(it)
                //updateModTools(it.selectedDirectory)
                appPathsManager.setModPath(it.selectedDirectory)
                checkInstallMod()
                updateModCount()
                updateAntiHarmony()
                updateEnableModCount()
                flashModsObserverManager.openSelectedDictionaryObserver(it.selectedDirectory)
            }
        }
    }


    private fun checkUpdate() {
        viewModelScope.launch {
            val update = checkUpdateUserCase(BuildConfig.VERSION_NAME)
            if (update.third) {
                _downloadUrl = update.first[0]
                _universalUrl = update.first[1]
                _updateContent = update.second[0]
                setShowUpgradeDialog(true)
            }
        }
    }

    private fun getNewInfo() {
        viewModelScope.launch {
            getInformationUserCase()?.let { info ->
                _uiState.update {
                    it.copy(infoBean = info)
                }
                setShowInfoDialog(true)
            }
        }
    }

    fun setShowInfoDialog(b: Boolean) {
        _uiState.update {
            it.copy(showInfoDialog = b)
        }
    }

    private fun updateAntiHarmony() {
        updateAntiHarmonyJob?.cancel()
        updateAntiHarmonyJob = viewModelScope.launch {
            getAntiHarmonyUserCase().collectLatest { antiHarmonyBean ->
                if (antiHarmonyBean == null) {
                    addGameToAntiHarmonyUserCase()
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

    private fun setScanQQDirectory(scanQQDirectory: Boolean) {
        viewModelScope.launch {
            saveUserPreferenceUseCase(UserPreferencesKeys.SCAN_QQ_DIRECTORY, scanQQDirectory)
        }
    }


    fun setSelectedDirectory(selectedDirectory: String) {
        viewModelScope.launch {
            if (!saveSelectModDirectoryUserCase(selectedDirectory)) {
                ToastUtils.longCall(R.string.toast_this_dir_has_no_prim_android)
            }
        }
    }

    private fun setScanDownload(scanDownload: Boolean) {
        viewModelScope.launch {
            saveUserPreferenceUseCase(UserPreferencesKeys.SCAN_DOWNLOAD, scanDownload)
        }
    }

    fun setOpenPermissionRequestDialog(b: Boolean) {
        viewModelScope.launch {
            saveUserPreferenceUseCase(UserPreferencesKeys.OPEN_PERMISSION_REQUEST_DIALOG, b)
        }
    }


    // 设置扫描文件夹中的Mods
    fun setScanDirectoryMods(scanDirectoryMods: Boolean) {/*  if (gameInfo.isGameFileRepeat){
              ToastUtils.longCall(R.string.toast_game_config_not_suppose)
              return
          }*/
        viewModelScope.launch {
            saveUserPreferenceUseCase(UserPreferencesKeys.SCAN_DIRECTORY_MODS, scanDirectoryMods)
        }
    }

    // 设置请求权路径
    fun setRequestPermissionPath(path: String) {
        Log.d("ConsoleViewModel", "setRequestPermissionPath: $path")
        _requestPermissionPath = path
    }


    // 更新游戏信息
    private suspend fun updateGameInfo(userPreferencesState: UserPreferencesState) {
        val gameInfo = updateGameInfoUserCase(
            userPreferencesState.selectedGameIndex,
            appPathsManager.getRootPath() + userPreferencesState.selectedDirectory
        )
        setGameInfo(gameInfo)
    }


    // 开启扫描QQ目录
    fun openScanQQDirectoryDialog(b: Boolean) {
        setRequestPermissionPath(permissionTools.getRequestPermissionPath(ScanModPath.MOD_PATH_QQ))
        if (permissionTools.checkPermission(ScanModPath.MOD_PATH_QQ) != PathType.NULL) {
            setScanQQDirectory(b)
        } else {
            setOpenPermissionRequestDialog(true)
        }
    }

    // 开启反和谐
    fun openAntiHarmony(flag: Boolean) {
        val gameInfo = gameInfoManager.getGameInfo()
        viewModelScope.launch {
            val result = switchAntiHarmonyUserCase(flag)
            when (result) {
                ResultCode.NOT_SUPPORT -> {
                    ToastUtils.longCall(R.string.toast_game_not_suppose_anti)
                }

                ResultCode.NO_PERMISSION -> {
                    setRequestPermissionPath(permissionTools.getRequestPermissionPath(gameInfo.gamePath))
                    setOpenPermissionRequestDialog(true)
                }

                ResultCode.NO_MY_APP_PERMISSION -> {
                    setRequestPermissionPath(appPathsManager.getMyAppPath())
                    setOpenPermissionRequestDialog(true)
                }

                ResultCode.SUCCESS -> {

                }
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
            Intent.ACTION_VIEW, url.toUri()
        )
        context.startActivity(urlIntent)
    }


    fun openScanDirectoryMods(b: Boolean) {
        if (b) {
            _uiState.update {
                it.copy(showScanDirectoryModsDialog = true)
            }
        } else {
            setScanDirectoryMods(false)
        }
    }

    // 检查是否支持安装mod
    private fun checkInstallMod() {
        val gameInfo = gameInfoManager.getGameInfo()
        val checkPermission = permissionTools.checkPermission(gameInfo.gamePath)
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
        updateModCountJob?.cancel()
        updateModCountJob = viewModelScope.launch {
            getGameModsCountUserCase().collectLatest { count ->
                _uiState.update {
                    it.copy(modCount = count)
                }
            }
        }
    }

    // 更新已开启mod数量
    private fun updateEnableModCount() {
        updateEnableModCountJob?.cancel()
        updateEnableModCountJob = viewModelScope.launch {
            getGameEnableModsCountUserCase().collectLatest { count ->
                _uiState.update {
                    it.copy(enableModCount = count)
                }
            }
        }

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
//    private fun checkUpdate() {
//        viewModelScope.launch {
//            kotlin.runCatching {
//                ModManagerApi.retrofitService.getUpdate()
//            }.onFailure {
//                Log.e("ConsoleViewModel", "checkUpdate: $it")
//            }.onSuccess {
//                if (it.code > ModTools.getVersionCode()) {
//                    Log.d("ConsoleViewModel", "checkUpdate: $it")
//                    _downloadUrl = it.url
//                    _updateContent = it.des
//                    setShowUpgradeDialog(true)
//                }
//            }
//        }
//    }


    fun startGame() {
        appInfoTools.startGame()
    }


    fun setShowDelUnzipDialog(b: Boolean) {
        _uiState.update {
            it.copy(showDeleteUnzipDialog = b)
        }
    }

    fun switchDelUnzip(b: Boolean) {
        if (b && !_uiState.value.showDeleteUnzipDialog) {
            _uiState.update {
                it.copy(showDeleteUnzipDialog = true)
            }
        } else {
            viewModelScope.launch {
                saveUserPreferenceUseCase(UserPreferencesKeys.DELETE_UNZIP_DIRECTORY, b)
            }
            _uiState.update {
                it.copy(showDeleteUnzipDialog = false)
            }
        }
    }

    fun setShowCategoryView(b: Boolean) {
        // 展示分类视图
        viewModelScope.launch {
            saveUserPreferenceUseCase(UserPreferencesKeys.SHOW_CATEGORY_VIEW, b)
        }

    }

    fun getGameIcon(): ImageBitmap {

        return appInfoTools.getAppIcon(gameInfoManager.getGameInfo().packageName)


    }

    fun getPermissionTools(): PermissionTools {
        return permissionTools
    }

    fun getAppPathsManager(): AppPathsManager {
        return appPathsManager
    }

    fun getFileToolsManager(): FileToolsManager {
        return fileToolsManager
4
    }
}



