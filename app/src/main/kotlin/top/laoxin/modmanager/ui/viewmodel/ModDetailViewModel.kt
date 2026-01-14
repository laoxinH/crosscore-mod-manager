package top.laoxin.modmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.usercase.mod.RefreshModDetailUseCase
import top.laoxin.modmanager.ui.state.ModDetailUiState
import top.laoxin.modmanager.ui.state.SnackbarManager
import javax.inject.Inject

/**
 * Mod详情和预览ViewModel
 */
@HiltViewModel
class ModDetailViewModel @Inject constructor(
    private val refreshModDetailUseCase: RefreshModDetailUseCase,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    companion object {
        const val TAG = "ModDetailViewModel"
    }

    private val _uiState = MutableStateFlow(ModDetailUiState())
    val uiState: StateFlow<ModDetailUiState> = _uiState.asStateFlow()

    /**
     * 打开mod详情
     */
    fun openModDetail(mod: ModBean, isShow:  Boolean) {
        _uiState.update { it.copy(mod = mod, isShown = true) }
    }

    /**
     * 关闭mod详情
     */
    fun closeModDetail() {
        _uiState.update { it.copy(isShown = false, mod = null) }
    }

    /**
     * 刷新mod详情
     */
/*    fun refreshModDetail() {
        val modBean = _uiState.value.mod ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }
            try {
                val result = flashModDetailUserCase(modBean)
                _uiState.update { it.copy(mod = result.mod) }
            } finally {
                _uiState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }*/

    /**
     * 刷新mod预览图（重新从压缩包提取图片到缓存）
     * 
     * @param modBean 需要刷新图片的MOD
     * @param silent 是否静默模式（不显示loading状态）
     */
    fun refreshModDetail(modBean: ModBean, silent: Boolean = true) {
        viewModelScope.launch {
            if (!silent) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoadingImage = true) }
                }
            }

            try {
                // 使用 RefreshModDetailUseCase 重新提取图片和 README
                when (val result = refreshModDetailUseCase(modBean)) {
                    is Result.Success -> {
                        // 刷新成功，显示提示
                        if (!silent) {
                            snackbarManager.showMessage(R.string.refresh_detail_success)
                        }
                        
                        // 如果是当前显示的mod，更新详情
                        if (_uiState.value.mod?.id == modBean.id) {
                            _uiState.update { it.copy(mod = result.data) }
                        }
                    }
                    is Result.Error -> {
                        // 刷新失败，显示错误提示
                        if (!silent) {
                            snackbarManager.showMessage(R.string.refresh_detail_failed)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh mod detail: ${modBean.name}", e)
                if (!silent) {
                    snackbarManager.showMessage(R.string.refresh_detail_failed)
                }
            } finally {
                if (!silent) {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isLoadingImage = false) }
                    }
                }
            }
        }
    }

    fun setShowModDetail(bool: Boolean) {
        _uiState.update { it.copy(isShown = bool) }
    }
}
