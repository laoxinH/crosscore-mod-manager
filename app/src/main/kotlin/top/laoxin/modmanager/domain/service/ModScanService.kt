package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ModForm
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.model.ScanEvent

/** MOD 扫描服务统一接口 负责自动检测 MOD 类型并调度到对应的扫描服务 */
interface ModScanService {

    /**
     * 预加载游戏文件映射 在批量扫描前调用一次，避免每次扫描都重复加载
     * @param gameInfo 游戏配置信息
     * @return Map<游戏目录名, Set<文件名>>
     */
    suspend fun loadGameFilesMap(gameInfo: GameInfoBean): Result<Map<String, Set<String>>>

    /**
     * 扫描单个源（自动检测 MOD 类型）
     * @param path 压缩包或文件夹路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选，如果未提供则内部加载）
     * @return 识别到的 MOD 列表
     */
    suspend fun scan(
            path: String,
            gameInfo: GameInfoBean,
            gameFilesByDir: Map<String, Set<String>>? = null
    ): Result<List<ModBean>>

    /**
     * 扫描压缩包（自动检测 MOD 类型）
     * @param archivePath 压缩包完整路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选）
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
     * 扫描文件夹（自动检测 MOD 类型）
     * @param folderPath 文件夹完整路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选）
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
     * 检测指定路径的 MOD 类型
     * @param path 压缩包或文件夹路径
     * @return MOD 类型
     */
    suspend fun detectModForm(path: String): Result<ModForm>

    /**
     * 判断指定路径是否包含有效 MOD
     * @param path 压缩包或文件夹路径
     * @param gameInfo 游戏配置信息
     * @param gameFilesByDir 预加载的游戏文件映射（可选）
     * @return 是否包含有效 MOD
     */
    suspend fun isModSource(
            path: String,
            gameInfo: GameInfoBean,
            gameFilesByDir: Map<String, Set<String>>? = null
    ): Result<Boolean>
}


