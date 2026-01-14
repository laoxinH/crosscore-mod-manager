package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ScanFileBean

/**
 * 负责解析MOD文件的仓库。
 */
interface ModParseRepository {
    /**
     * 从扫描到的文件中解析出ModBean。
     * @param scanFile 扫描到的文件
     * @param gameInfo 游戏信息
     * @return 解析出的ModBean
     */
    fun parseMod(scanFile: ScanFileBean, gameInfo: GameInfoBean): Flow<ModBean>
}
