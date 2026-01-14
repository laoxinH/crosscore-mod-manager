package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.AppError

/** MOD 源文件准备服务接口 负责从外部目录（QQ文件夹、Download文件夹等）转移 MOD 文件到 MOD 目录 */
interface ModSourcePrepareService {

        /**
         * 从外部目录转移 MOD 文件到目标目录
         * @param externalPaths 外部目录列表（如 QQ文件夹、Download文件夹）
         * @param targetPath 目标 MOD 目录（如 /sdcard/ModManager/游戏包名/）
         * @param gameInfo 游戏配置信息，用于验证是否为有效 MOD
         * @return Flow<TransferEvent> 转移事件流
         */
        fun transferModSources(
                externalPaths: List<String>,
                targetPath: String,
                gameInfo: GameInfoBean
        ): Flow<TransferEvent>

        /**
         * 扫描外部目录，查找可能的 MOD 文件
         * @param externalPath 外部目录路径
         * @param gameInfo 游戏配置信息
         * @return 找到的 MOD 文件路径列表
         */
        suspend fun findModFiles(externalPath: String, gameInfo: GameInfoBean): Flow<FindModEvent>
}

/** 文件转移事件（流式状态） */
sealed class TransferEvent {
        /** 正在扫描目录 */
        data class Scanning(val directory: String, val current: Int, val total: Int) : TransferEvent()

        data class ScanningFile(val directory: String, val fileName: String, val current: Int, val total: Int) :TransferEvent()
        /** 文件转移进度 */
        data class FileProgress(val fileName: String, val current: Int, val total: Int) :
                TransferEvent()

        /** 转移完成 */
        data class Complete(val result: TransferResult) : TransferEvent()

        /** 转移错误 */
        data class Error(val err: AppError) : TransferEvent()
}

/** 文件转移结果 */
data class TransferResult(
        /** 成功转移的文件数量 */
        val transferredCount: Int,
        /** 跳过的文件数量（已存在或不是有效 MOD） */
        val skippedCount: Int,
        /** 转移失败的错误信息列表 */
        val errors: List<String> = emptyList()
)
/** 查找MOD事件 (流式)
 *
 */

sealed class FindModEvent {
    /** 正在扫描目录 */
    data class Scanning(val directory: String, val current: Int, val total: Int) : FindModEvent()

    /** 正在扫描文件 */
    data class ScanningFile(val fileName: String, val current: Int, val total: Int) :
        FindModEvent()
    /** 查找到一个MOD文件 */
    data class FoundOne(val fileName: String, val filePath : String) : FindModEvent()

    /** 扫描完成 */
    data class Complete(val result: FindModResult) : FindModEvent()

    /** 扫描错误 */
    data class Error(val err: AppError) : FindModEvent()
}
/** 扫描结果 */
data class FindModResult(
    /** 找到的MOD文件路径列表 */
    val modFiles: List<String>,
    /** 跳过的文件数量（不是MOD文件） */
    val skippedCount: Int,
    /** 扫描失败的错误信息列表 */
    val errors: List<String> = emptyList()
)
