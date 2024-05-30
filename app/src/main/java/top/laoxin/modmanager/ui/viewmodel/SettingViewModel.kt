package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.data.UserPreferencesRepository
import top.laoxin.modmanager.data.backups.BackupRepository
import top.laoxin.modmanager.data.mods.ModRepository
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.ToastUtils

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

    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        getGameInfo()
    }

    // 删除所有备份
    fun deleteAllBackups() {
        setDeleteBackupDialog(false)
        viewModelScope.launch(Dispatchers.IO) {
            modRepository.getEnableModCount().collect {
                if (it > 0) {
                    // 如果有mod开启则提示
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(R.string.toast_del_buckup_when_mod_enable)
                    }
                    this@launch.cancel()
                } else {
                    backupRepository.deleteAllBackups()
                    val delBackupFile: Boolean = ModTools.deleteBackupFiles()
                    if (!delBackupFile) {
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(R.string.toast_del_buckup_filed)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(R.string.toast_del_buckup_success)
                    }
                }
            }

            backupRepository.deleteAllBackups()
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
        _uiState.value = _uiState.value.copy(showAcknowledgments = b)
    }

    fun showSwitchGame(b: Boolean) {
        gameInfoJob?.cancel()
        gameInfoJob = viewModelScope.launch(Dispatchers.IO) {
            modRepository.getEnableModCount().collect {
                withContext(Dispatchers.Main) {
                    if (it > 0) {
                        // 如果有mod开启则提示
                        ToastUtils.longCall(R.string.toast_switch_game_when_mod_enable)
                    } else {

                        _uiState.value = _uiState.value.copy(showSwitchGame = b)
                    }
                }
                gameInfoJob?.cancel()
            }
            // 读取已安装的所有游戏
        }
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
    fun setGameInfoList(gameInfoList: List<GameInfoConstant>) {
        _uiState.value = _uiState.value.copy(gameInfoList = gameInfoList)
    }

    // 通过名称软件包名获取软件包信息
    private fun getGameInfo() {
        viewModelScope.launch {
            val gameInfoList = mutableListOf<GameInfoConstant>()
            GameInfoConstant.entries.forEach {
                val packageName = it.packageName
                try {
                    val packageInfo = App.get().packageManager.getPackageInfo(packageName, 0)
                    if (packageInfo != null) {
                        gameInfoList.add(it)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
            setGameInfoList(gameInfoList)
        }
    }

    //设置游戏信息
    fun setGameInfo(gameInfo: GameInfoConstant) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.savePreference("GAME_SERVICE", gameInfo.serviceName)
            userPreferencesRepository.savePreference("INSTALL_PATH", gameInfo.gamePath)
            withContext(Dispatchers.Main) {
                ToastUtils.longCall(R.string.toast_set_game_info_success)
                showSwitchGame(false)
            }
        }
    }
}