package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.StateFlow
import top.laoxin.modmanager.ui.state.ScanProgressState

/**
 * 扫描状态仓库接口
 * 作为扫描状态的单一数据源，供 Service 发布状态，ViewModel 观察状态
 */
interface ScanStateRepository {
    /**
     * 扫描状态 Flow
     * null 表示没有扫描任务
     */
    val scanState: StateFlow<ScanProgressState?>

    /**
     * 更新扫描状态
     */
    fun updateState(state: ScanProgressState?)

    /**
     * 清除扫描状态
     */
    fun clear()
    fun exitBackgroundMode()
}
