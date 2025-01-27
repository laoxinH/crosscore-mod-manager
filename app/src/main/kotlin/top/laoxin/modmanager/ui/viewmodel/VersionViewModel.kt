package top.laoxin.modmanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import top.laoxin.modmanager.App
import top.laoxin.modmanager.database.VersionRepository

class VersionViewModel(application: Application) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as App)
                VersionViewModel(application)
            }
        }
    }

    private val versionRepository = VersionRepository(application)

    private val _version = MutableLiveData<String>()
    val version: LiveData<String> = _version

    // 获取版本号
    suspend fun loadVersion(): String {
        return versionRepository.getVersion()
    }

    // 更新版本号
    fun updateVersion(version: String) {
        viewModelScope.launch {
            versionRepository.saveVersion(version)
            _version.value = version
        }
    }

    // 获取版本信息
    suspend fun loadVersionInfo(): String {
        return versionRepository.getVersionInfo()
    }

    // 更新版本信息
    fun updateVersionInfo(versionInfo: String) {
        viewModelScope.launch {
            versionRepository.saveVersionInfo(versionInfo)
        }
    }

    // 获取版本下载地址
    suspend fun loadVersionUrl(): String {
        return versionRepository.getVersionUrl()
    }

    // 更新版本下载地址
    fun updateVersionUrl(versionUrl: String) {
        viewModelScope.launch {
            versionRepository.saveVersionUrl(versionUrl)
        }
    }

    // 获取版本下载地址
    suspend fun loadUniversalUrl(): String {
        return versionRepository.getUniversalUrl()
    }

    // 更新版本下载地址
    fun updateUniversalUrl(universalUrl: String) {
        viewModelScope.launch {
            versionRepository.saveUniversalUrl(universalUrl)
        }
    }
}
