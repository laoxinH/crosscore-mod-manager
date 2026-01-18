package top.laoxin.modmanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.laoxin.modmanager.domain.repository.ScanStateRepository
import top.laoxin.modmanager.ui.state.ScanProgressState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 扫描状态仓库实现
 * 使用 MutableStateFlow 作为状态容器
 */
@Singleton
class ScanStateRepositoryImpl @Inject constructor() : ScanStateRepository {

    private val _scanState = MutableStateFlow<ScanProgressState?>(null)
    override val scanState: StateFlow<ScanProgressState?> = _scanState.asStateFlow()

    override fun updateState(state: ScanProgressState?) {
        _scanState.update { state }
    }

    override fun clear() {
        _scanState.update { null }
    }

    override fun exitBackgroundMode() {
        _scanState.update { currentState ->
            currentState?.copy(isBackgroundMode = false)
        }
    }
}
