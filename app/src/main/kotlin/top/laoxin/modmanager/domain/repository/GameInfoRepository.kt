package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.DownloadGameConfigBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.Result

interface GameInfoRepository {
    /** Provides an observable, real-time list of all available games. */
    fun getGameInfoList(): Flow<List<GameInfoBean>>

    /** Reloads the game configurations from the data source. */
    suspend fun reloadGameInfo()

    suspend fun enrichGameInfo(baseGameInfo: GameInfoBean, modPath: String): GameInfoBean

    /** 获取远程游戏配置列表 */
    suspend fun getRemoteGameConfigs(): Result<List<DownloadGameConfigBean>>

    /**
     * 下载并安装远程游戏配置
     * @param config 要下载的配置信息
     * @return 下载结果
     */
    suspend fun downloadRemoteGameConfig(config: DownloadGameConfigBean): Result<GameInfoBean>

    /**
     * 从自定义目录导入游戏配置文件
     * @param customConfigPath 自定义配置文件所在的目录
     * @return 导入结果，包含导入成功的数量和失败的数量
     */
    suspend fun importCustomGameConfigs(customConfigPath: String): Result<ImportConfigResult>


}

/**
 * 配置导入结果
 */
data class ImportConfigResult(
    val successCount: Int,
    val failedCount: Int,
    val successConfigs: List<String> = emptyList()
)
