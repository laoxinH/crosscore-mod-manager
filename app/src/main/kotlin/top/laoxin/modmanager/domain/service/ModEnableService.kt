package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError

/** MOD 开启服务接口 处理单个 MOD 的文件验证和复制操作 */
interface ModEnableService {

    /**
     * 开启单个 MOD（返回 Flow 实时反馈文件级进度）
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<EnableFileEvent> 文件级事件流
     */
    fun enableSingleMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent>

    /**
     * 关闭单个 MOD
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<EnableFileEvent> 文件级事件流
     */
    fun disableSingleMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent>

    /**
     * 验证 MOD 是否可开启
     * @param mod MOD 信息
     * @return ValidationResult 验证结果
     */
    suspend fun validateMod(mod: ModBean): ValidationResult
}

/** 开启步骤枚举（用于 i18n） */
enum class EnableStep {
    VALIDATING, // 验证中
    BACKING_UP, // 备份中
    ENABLING, // 开启中（复制文件）
    DISABLING, // 关闭中（还原文件）
    SPECIAL_PROCESS, // 特殊处理
    UPDATING_DB, // 更新数据库
    COMPLETE, // 完成
    // 取消中
    CANCELING,
}

/** 文件级别事件（Service 发射） */
sealed class EnableFileEvent {
    /** 文件进度 */
    data class FileProgress(val fileName: String, val current: Int, val total: Int) :
            EnableFileEvent()

    /** 单个 MOD 完成 */
    data class Complete(
            val success: Boolean,
            val error: AppError? = null,
            /** 替换文件的 MD5 映射: gameFilePath -> md5 */
            val fileMd5Map: Map<String, String> = emptyMap()
    ) : EnableFileEvent()
}

/** 验证结果枚举 */
enum class ValidationResult {
    VALID, // 验证通过
    NEED_PASSWORD, // 需要密码
    FILE_MISSING // 文件缺失
}
