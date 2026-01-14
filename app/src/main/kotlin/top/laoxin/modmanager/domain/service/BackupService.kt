package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ReplacedFileBean
import top.laoxin.modmanager.domain.model.AppError

/** 备份服务接口 负责游戏文件的备份和还原 */
interface BackupService {

    /**
     * 备份游戏文件（流式返回状态） 将 mod.gameFilesPath 中的文件备份到备份目录
     * @param mod MOD 信息（包含 gameFilesPath）
     * @param gameInfo 游戏信息
     * @param replacedFilesMap 已替换文件映射 (gameFilePath -> ReplacedFileBean)，用于智能跳过备份
     * @return Flow<BackupEvent> 备份事件流
     */
    fun backupGameFiles(
            mod: ModBean,
            gameInfo: GameInfoBean,
            replacedFilesMap: Map<String, ReplacedFileBean> = emptyMap()
    ): Flow<BackupEvent>

    /**
     * 还原游戏文件（流式返回状态） 将备份的文件还原到原位置
     * @param backups 备份记录列表
     * @param replacedFilesMap 已替换文件映射 (gameFilePath -> ReplacedFileBean)，用于智能判断是否还原
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<BackupEvent> 还原事件流
     */
    fun restoreBackups(
            backups: List<BackupBean>,
            replacedFilesMap: Map<String, ReplacedFileBean>,
            mod: ModBean,
            gameInfo: GameInfoBean
    ): Flow<BackupEvent>

    /**
     * 根据 MOD ID 获取备份记录
     * @param modId MOD ID
     * @return Result<List<BackupBean>> 成功返回备份记录列表
     */
    // suspend fun getBackupsByModId(modId: Int): Result<List<BackupBean>>

    /**
     * 删除 MOD 相关的备份记录
     * @param modId MOD ID
     * @return Result<Unit> 成功返回 Unit，失败返回错误
     */
    // suspend fun deleteBackupsByModId(modId: Int): Result<Unit>
}

/** 备份事件（流式状态） */
sealed class BackupEvent {
    /** 文件进度 */
    data class FileProgress(val fileName: String, val current: Int, val total: Int) : BackupEvent()

    /** 备份完成（成功） */
    data class Success(val backups: List<BackupBean>) : BackupEvent()

    /** 备份失败 */
    data class Failed(val error: AppError) : BackupEvent()
}
