package top.laoxin.modmanager.domain.model

import top.laoxin.modmanager.domain.bean.ModBean

/** 开启事件密封类 */
sealed class EnableEvent {
    /** 验证阶段 */
    data class Validating(val modName: String, val current: Int, val total: Int) : EnableEvent()

    /** 备份阶段 */
    data class Backing(val modName: String, val current: Int, val total: Int) : EnableEvent()

    /** 开启/关闭阶段 */
    data class Processing(val modName: String, val current: Int, val total: Int) : EnableEvent()

    /** 单个 MOD 完成 */
    data class ModComplete(val mod: ModBean, val success: Boolean, val error: String? = null) : EnableEvent()

    /** 全部完成 */
    data class Complete(val result: EnableResult) : EnableEvent()

    /** 错误 */
    data class Error(val error: String) : EnableEvent()
}

/** 开启结果 */
data class EnableResult(
    val enabledCount: Int,
    val skippedCount: Int,
    val needPasswordMods: List<ModBean> = emptyList(),
    val fileMissingMods: List<ModBean> = emptyList(),
    val errors: List<String> = emptyList()
)