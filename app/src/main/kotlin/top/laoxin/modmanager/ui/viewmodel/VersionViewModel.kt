package top.laoxin.modmanager.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import top.laoxin.modmanager.data.repository.VersionRepository
import javax.inject.Inject

@HiltViewModel
class VersionViewModel @Inject constructor(
    private val versionRepository: VersionRepository
)  : ViewModel() {


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
