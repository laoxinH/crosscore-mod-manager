package top.laoxin.modmanager.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModManagerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ModManagerUiState())
    val uiState: StateFlow<ModManagerUiState> = _uiState.asStateFlow()



    // 设置当前导航栏索引
    fun setCurrentNavigationIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentNavigationIndex = index)
    }

    //  设置是否打开权限请求窗口
    fun setOpenPermissionRequestDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(openPermissionRequestDialog = open)
    }
}