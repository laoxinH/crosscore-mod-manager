package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result

/** MOD 解密服务接口 负责验证密码并更新加密 MOD 的信息 */
interface ModDecryptService {
    /**
     * 解密 MOD
     * @param archivePath 压缩包路径
     * @param password 密码
     * @param mods 该压缩包包含的所有 ModBean
     * @return Flow<DecryptEvent> 解密事件流
     */
    fun decryptMods(archivePath: String, password: String, mods: List<ModBean>): Flow<DecryptEvent>

    /**
     * 刷新 MOD 详情（重新从压缩包提取图片、图标和 README 到缓存）
     * 
     * 当用户误删缓存导致图片或描述无法显示时使用此方法
     * 仅对压缩包 MOD 生效，文件夹 MOD 直接返回成功
     * 
     * @param mod 需要刷新的 MOD
     * @return Result<ModBean> 刷新后的 MOD（包含更新后的图片路径和描述）
     */
    suspend fun refreshModDetail(mod: ModBean): Result<ModBean>
}

/** 解密事件（流式状态） */
sealed class DecryptEvent {
    /** 正在验证密码 */
    data class Validating(val archivePath: String) : DecryptEvent()

    /** 解密进度 */
    data class Progress(
            val modName: String,
            val current: Int,
            val total: Int,
            val step: DecryptStep
    ) : DecryptEvent()

    /** 单个 MOD 更新完成 */
    data class ModUpdated(val mod: ModBean) : DecryptEvent()

    /** 全部完成 */
    data class Complete(val result: DecryptResult) : DecryptEvent()

    /** 错误 */
    data class Error(val error: AppError) : DecryptEvent()
}

/** 解密步骤 */
enum class DecryptStep {
    VALIDATING_PASSWORD,
    EXTRACTING_ICON,
    EXTRACTING_IMAGES,
    READING_README,
    UPDATING_DATABASE
}

/** 解密结果 */
data class DecryptResult(val decryptedCount: Int, val failedMods: List<ModBean> = emptyList())
