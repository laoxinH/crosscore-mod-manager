package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.model.ScanEvent

/** 传统 MOD 扫描服务接口 负责扫描压缩包和文件夹，通过被动识别构建 ModBean 适用于 ModForm.TRADITIONAL 类型的 MOD */
interface TraditionalModScanService {

    /**
     * 扫描压缩包，识别其中的 MOD
     * @param archivePath 压缩包完整路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选，如果未提供则内部加载）
     * @return 识别到的 MOD 列表
     */
    suspend fun scanArchive(
            archivePath: String,
            gameInfo: GameInfoBean,
            gameFilesByDir: Map<String, Set<String>>? = null
    ): Result<List<ModBean>>

    /**
     * 扫描压缩包（带进度，使用 Flow）
     * @param archivePath 压缩包完整路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选）
     * @return 扫描事件 Flow
     */
    fun scanArchiveWithProgress(
            archivePath: String,
            gameInfo: GameInfoBean,
            gameFilesByDir: Map<String, Set<String>>? = null
    ): Flow<ScanEvent>

    /**
     * 扫描文件夹，识别其中的 MOD
     * @param folderPath 文件夹完整路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选，如果未提供则内部加载）
     * @return 识别到的 MOD 列表
     */
    suspend fun scanDirectoryMod(
            folderPath: String,
            gameInfo: GameInfoBean,
            gameFilesByDir: Map<String, Set<String>>? = null
    ): Result<List<ModBean>>

    /**
     * 扫描文件夹（带进度，使用 Flow）
     * @param folderPath 文件夹完整路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选）
     * @return 扫描事件 Flow
     */
    fun scanDirectoryModWithProgress(
            folderPath: String,
            gameInfo: GameInfoBean,
            gameFilesByDir: Map<String, Set<String>>? = null
    ): Flow<ScanEvent>

    /**
     * 判断指定路径是否包含有效 MOD
     * @param path 压缩包或文件夹路径
     * @param gameInfo 游戏配置信息
     * @return 是否包含有效 MOD
     */
    suspend fun isModSource(path: String, gameInfo: GameInfoBean): Result<Boolean>
}

