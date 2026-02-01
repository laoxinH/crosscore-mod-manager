package top.laoxin.modmanager.domain.model

/** 应用错误类型定义 用于统一管理应用中的各类错误 */
sealed class AppError {

    /** 转换为异常（用于需要抛出异常的场景） */
    fun toException(): Exception =
        when (this) {
            is FileError -> FileException(this)
            is ModError -> ModException(this)
            is ArchiveError -> ArchiveException(this)
            is PermissionError -> PermissionException(this)
            is GameError -> GameException(this)
            is NetworkError -> NetworkException(this)
            is DatabaseError -> DatabaseException(this)
            is AntiHarmonyError -> AntiHarmonyException(this)
            is GameConfigError -> GameConfigException(this)
            is Unknown -> throwable as? Exception ?: Exception(throwable)
        }

    // ==================== 文件相关错误 ====================
    sealed class FileError : AppError() {
        /** 权限不足 */
        object PermissionDenied : FileError()

        /** 文件不存在 */
        data class FileNotFound(val path: String = "") : FileError()

        /** 文件复制失败 */
        object CopyFailed : FileError()

        /** 文件删除失败 */
        object DeleteFailed : FileError()

        /** 文件移动失败 */
        object MoveFailed : FileError()

        /** 目录创建失败 */
        object CreateDirectoryFailed : FileError()

        /** 文件读取失败 */
        object ReadFailed : FileError()

        /** 文件写入失败 */
        object WriteFailed : FileError()
        /** shizuku 链接断开 */
        data class ShizukuDisconnected(val message: String) : FileError()
        /** 未知文件错误 */

        data class Unknown(val message: String) : FileError()
    }

    // ==================== 压缩包相关错误 ====================
    sealed class ArchiveError : AppError() {
        /** 解压失败 */
        object ExtractFailed : ArchiveError()

        /** 压缩包已加密，需要密码 */
        object EncryptedNeedPassword : ArchiveError()

        /** 密码错误 */
        object WrongPassword : ArchiveError()

        /** 不支持的压缩格式 */
        data class UnsupportedFormat(val format: String) : ArchiveError()

        /** 压缩包损坏 */
        object CorruptedArchive : ArchiveError()

        /** 压缩包为空 */
        object EmptyArchive : ArchiveError()

        /** 压缩包内文件未找到 */
        data class ItemNotFound(val itemName: String) : ArchiveError()

        /** 读取压缩包内容失败 */
        object ReadContentFailed : ArchiveError()

        /** 未知压缩包错误 */
        data class Unknown(val message: String) : ArchiveError()
    }

    // ==================== Mod 相关错误 ====================
    sealed class ModError : AppError() {
        /** Mod 解析失败 */
        object ParseFailed : ModError()

        /** Mod 已加密，需要密码 */
        object EncryptedNeedPassword : ModError()

        /** 密码错误 */
        object WrongPassword : ModError()

        /** 不支持的格式 */
        data class UnsupportedFormat(val format: String) : ModError()

        /** 不支持的 MOD 类型 */
        data class UnsupportedModType(val type: String) : ModError()

        /** 无效的 Mod 结构 */
        data class InvalidStructure(val reason: String) : ModError()

        /** 无效的 MOD 数据 */
        data class InvalidModData(val reason: String) : ModError()

        /** Mod 启用失败 */
        data class EnableFailed(val reason: String) : ModError()

        /** Mod 禁用失败 */
        data class DisableFailed(val reason: String) : ModError()

        /** 未知 Mod 错误 */
        data class Unknown(val message: String) : ModError()

        /** 备份失败 */
        data class BackupFailed(val reason: String) : ModError()

        /** 恢复失败 */
        data class RestoreFailed(val reason: String) : ModError()

        /** 文件缺失 */
        data class FileMissing(val reason: String) : ModError()

        /** 文件写入失败 */
        data class WriteFailed(val reason: String) : ModError()

        /** mod文件读取失败 */
        data class ReadFailed(val reason: String) : ModError()

        /** 文件复制失败 */
        data class CopyFailed(val reason: String) : ModError()

        /** MD5 计算失败 */
        data class Md5CalculationFailed(val path: String) : ModError()

        // 创建文件夹失败
        data class CreateDirectoryFailed(val reason: String) : ModError()

        /** 解密失败 */
        data class DecryptFailed(val reason: String) : ModError()

        // 特殊操作失败
        data class SpecialOperationFailed(val reason: String) : ModError()
    }

    // ==================== 权限相关错误 ====================
    sealed class PermissionError : AppError() {
        /** Shizuku 未安装 */
        object ShizukuNotInstalled : PermissionError()

        /** Shizuku 未运行 */
        object ShizukuNotRunning : PermissionError()

        /** Shizuku 权限被拒绝 */
        object ShizukuPermissionDenied : PermissionError()

        /** 存储权限被拒绝 */
        object StoragePermissionDenied : PermissionError()

        /** URI 权限未授予 */
        object UriPermissionNotGranted : PermissionError()

        /** 未知权限错误 */
        data class Unknown(val message: String) : PermissionError()

        /** shizhuku 请求权限错误 */
        data class ShizukuPermissionRequestFailed(val message: String) : PermissionError()
    }

    // ==================== 游戏相关错误 ====================
    sealed class GameError : AppError() {
        /** 未选择游戏 */
        object GameNotSelected : GameError()

        /** 游戏未安装 */
        data class GameNotInstalled(val gameName: String) : GameError()
    }

    // ==================== 网络相关错误 ====================
    sealed class NetworkError : AppError() {
        /** 无网络连接 */
        object NoConnection : NetworkError()

        /** 请求超时 */
        object Timeout : NetworkError()

        /** 连接失败 */
        data class ConnectionFailed(val message: String) : NetworkError()

        /** 服务器错误 */
        data class ServerError(val code: Int, val message: String) : NetworkError()

        /** 未知网络错误 */
        data class Unknown(val message: String) : NetworkError()
    }

    // ==================== 游戏配置相关错误 ====================
    sealed class GameConfigError : AppError() {
        /** 无效的配置 */
        data class InvalidConfig(val reason: String) : GameConfigError()

        /** 配置已存在 */
        data class ConfigAlreadyExists(val packageName: String) : GameConfigError()

        /** 配置保存失败 */
        data class SaveFailed(val reason: String) : GameConfigError()
    }

    // ==================== 数据库相关错误 ====================
    sealed class DatabaseError : AppError() {
        /** 插入失败 */
        object InsertFailed : DatabaseError()

        /** 更新失败 */
        object UpdateFailed : DatabaseError()

        /** 删除失败 */
        object DeleteFailed : DatabaseError()

        /** 查询失败 */
        object QueryFailed : DatabaseError()

        /** 迁移失败 */
        data class MigrationFailed(val from: Int, val to: Int) : DatabaseError()
    }

    // ==================== 反和谐相关错误 ====================
    sealed class AntiHarmonyError : AppError() {
        /** 不支持反和谐 */
        object NotSupported : AntiHarmonyError()

        /** 反和谐操作失败 */
        data class OperationFailed(val message: String) : AntiHarmonyError()

        /** 未知反和谐错误 */
        data class Unknown(val message: String) : AntiHarmonyError()
    }

    // ==================== 通用未知错误 ====================
    data class Unknown(val throwable: Throwable) : AppError()
}

// ==================== 自定义异常类 ====================

class FileException(val error: AppError.FileError) : Exception(error.toString())

class ArchiveException(val error: AppError.ArchiveError) : Exception(error.toString())

class ModException(val error: AppError.ModError) : Exception(error.toString())

class PermissionException(val error: AppError.PermissionError) : Exception(error.toString())

class GameException(val error: AppError.GameError) : Exception(error.toString())

class NetworkException(val error: AppError.NetworkError) : Exception(error.toString())

class DatabaseException(val error: AppError.DatabaseError) : Exception(error.toString())

class AntiHarmonyException(val error: AppError.AntiHarmonyError) : Exception(error.toString())

class GameConfigException(val error: AppError.GameConfigError) : Exception(error.toString())
