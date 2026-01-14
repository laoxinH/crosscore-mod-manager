package top.laoxin.modmanager.domain.model

import top.laoxin.modmanager.domain.bean.ModBean

/**
 * 扫描步骤枚举
 * 用于标识当前扫描处于哪个阶段，UI 层根据此枚举获取多语言字符串
 */
enum class ScanStep {
    /** 检查加密状态 */
    CHECKING_ENCRYPTION,
    
    /** 读取文件列表 */
    LISTING_FILES,
    
    /** 分析文件 */
    ANALYZING_FILES,
    
    /** 识别 MOD */
    IDENTIFYING_MODS,
    
    /** 检查文件夹 */
    CHECKING_FOLDER,
    
    /** 转移外部 MOD */
    TRANSFERRING,
    
    /** 扫描 MOD 目录 */
    SCANNING_DIRECTORY,
    
    /** 同步数据库 */
    SYNCING_DATABASE,
    
    /** 完成 */
    COMPLETE
}

/**
 * 扫描事件密封类
 * 用于在 Flow 中发射扫描过程的各种状态
 */
sealed class ScanEvent {
    /**
     * 进度更新事件
     * @param step 当前扫描步骤（枚举，用于 i18n）
     * @param currentFile 当前处理的文件名（可选）
     * @param current 当前进度值（如已处理文件数）
     * @param total 总数（如总文件数）
     * @param subProgress 子进度百分比 (0-1)，-1 表示不确定进度
     */
    data class Progress(
        val step: ScanStep,
        val currentFile: String = "",
        val current: Int = 0,
        val total: Int = 0,
        val subProgress: Float = -1f
    ) : ScanEvent()

    /**
     * 发现一个 MOD 事件
     */
    data class ModFound(val mod: ModBean) : ScanEvent()

    /**
     * 扫描完成事件
     * @param mods 扫描到的所有 MOD 列表
     */
    data class Complete(val mods: List<ModBean>) : ScanEvent()

    /**
     * 错误事件
     */
    data class Error(val error: AppError) : ScanEvent()
}
