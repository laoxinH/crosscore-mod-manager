package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ScanFileBean

/**
 * 负责扫描MOD文件的仓库。
 * 封装了所有与文件系统交互的逻辑。
 */
interface ModScanRepository {
    /**
     * 扫描指定路径的Mod文件。
     * @param scanPath 要扫描的路径
     * @param gameInfo 游戏信息
     * @return 扫描到的文件列表
     */
    fun scanMods(scanPath: String, gameInfo: GameInfoBean): Flow<ScanFileBean>
}
