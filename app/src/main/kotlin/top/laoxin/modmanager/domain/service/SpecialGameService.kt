package top.laoxin.modmanager.domain.service

import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result

/** 特殊游戏服务接口 处理特定游戏的特殊操作，如明日方舟校验文件修改、尘白禁区注入等 */
interface SpecialGameService {

    /**
     * 启用 Mod 时的特殊操作
     * @param mod Mod 信息
     * @param packageName 游戏包名
     * @return Result<Unit> 操作结果
     */
    suspend fun onModEnable(mod: ModBean, packageName: String): Result<Unit>

    /**
     * 禁用 Mod 时的特殊操作
     * @param backup 备份信息列表
     * @param packageName 游戏包名
     * @param mod Mod 信息
     * @return Result<Unit> 操作结果
     */
    suspend fun onModDisable(
            backup: List<BackupBean>,
            packageName: String,
            mod: ModBean
    ): Result<Unit>

    /**
     * 启动游戏时的特殊操作
     * @param gameInfo 游戏信息
     * @return Result<Unit> 操作结果
     */
    suspend fun onGameStart(gameInfo: GameInfoBean): Result<Unit>

    /**
     * 启动游戏前的检查
     * @param gameInfo 游戏信息
     * @return Result<GameStartCheckResult> 检查结果
     */
    suspend fun checkBeforeGameStart(gameInfo: GameInfoBean): Result<GameStartCheckResult>

    /**
     * 扫描 Mod 时的特殊判断
     * @param packageName 游戏包名
     * @param modFileName Mod 文件名
     * @return 是否为该游戏支持的 Mod 文件
     */
    fun isModFileSupported(packageName: String, modFileName: String): Boolean

    /**
     * 选择游戏时的特殊操作
     * @param gameInfo 游戏信息
     * @return Result<GameInfoBean> 可能更新后的游戏信息
     */
    suspend fun onGameSelect(gameInfo: GameInfoBean): Result<GameInfoBean>

    /**
     * 更新游戏信息
     * @param gameInfo 原始游戏信息
     * @return 更新后的游戏信息
     */
    fun updateGameInfo(gameInfo: GameInfoBean): GameInfoBean

    /**
     * 该游戏是否需要启动后台服务
     * @param packageName 游戏包名
     */
    fun needGameService(packageName: String): Boolean

    /**
     * 该游戏是否需要开启 VPN
     * @param packageName 游戏包名
     */
    fun needOpenVpn(packageName: String): Boolean

    /**
     * 是否为特殊游戏
     * @param packageName 游戏包名
     */
    fun isSpecialGame(packageName: String): Boolean
}

/** 游戏启动前检查结果 */
sealed class GameStartCheckResult {
    /** 检查通过，可以启动 */
    object Success : GameStartCheckResult()

    /** 游戏已更新，需要重新处理 */
    object GameUpdated : GameStartCheckResult()

    /** 没有权限 */
    object NoPermission : GameStartCheckResult()
}
