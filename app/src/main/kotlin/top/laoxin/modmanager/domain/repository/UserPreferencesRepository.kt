package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.Result

/** 用户偏好设置 Repository 接口 */
interface UserPreferencesRepository {

    // ==================== 游戏选择 ====================

    /** 当前选中的游戏信息 直接存储完整的 GameInfoBean，避免索引失效问题 默认值为 GameInfoConstant.NO_GAME */
    val selectedGame: Flow<GameInfoBean>

    /** 同步获取当前选中的游戏信息 用于命令型 UseCase 中需要获取当前值的场景 避免 .first() 的 I/O 开销 */
    val selectedGameValue: GameInfoBean

    /**
     * 保存选中的游戏
     * @param gameInfo 游戏信息
     */
    suspend fun saveSelectedGame(gameInfo: GameInfoBean)

    // ==================== 目录设置 ====================

    val selectedDirectory: Flow<String>

    val scanQQDirectory: Flow<Boolean>
    suspend fun saveScanQQDirectory(shouldScan: Boolean)

    val scanDownload: Flow<Boolean>
    suspend fun saveScanDownload(shouldScan: Boolean)

    val scanDirectoryMods: Flow<Boolean>
    suspend fun saveScanDirectoryMods(shouldScan: Boolean)

    val deleteUnzipDirectory: Flow<Boolean>
    suspend fun saveDeleteUnzipDirectory(shouldDelete: Boolean)

    // ==================== UI 设置 ====================

    val showCategoryView: Flow<Boolean>
    suspend fun saveShowCategoryView(shouldShow: Boolean)

    val userTips: Flow<Boolean>
    suspend fun saveUserTips(shouldShow: Boolean)

    /** MOD页面导航索引 (NavigationIndex.index) */
    val modsViewIndex: Flow<Int>
    suspend fun saveModsViewIndex(index: Int)

    /** MOD列表显示模式 (0=列表视图, 1=大图网格视图) */
    val modListDisplayMode: Flow<Int>
    suspend fun saveModListDisplayMode(mode: Int)

    /** MOD冲突检测开关 (默认开启) */
    val conflictDetectionEnabled: Flow<Boolean>
    suspend fun saveConflictDetectionEnabled(enabled: Boolean)

    // ==================== 高级操作 ====================

    suspend fun prepareAndSetModDirectory(
            selectedDirectoryPath: String,
    ): Result<Unit>

    // ==================== 版本缓存 ====================

    val cachedVersionName: Flow<String>
    suspend fun saveCachedVersionName(name: String)

    val cachedChangelog: Flow<String>
    suspend fun saveCachedChangelog(log: String)

    val cachedDownloadUrl: Flow<String>
    suspend fun saveCachedDownloadUrl(url: String)

    val cachedUniversalUrl: Flow<String>
    suspend fun saveCachedUniversalUrl(url: String)

    // ==================== 公告缓存 ====================

    val cachedInformation: Flow<String>
    suspend fun saveCachedInformation(information: String)

    val cachedInformationVision: Flow<Double>
    suspend fun saveCachedInformationVision(vision: Double)
    suspend fun saveSelectedDirectory(path: String)
}
