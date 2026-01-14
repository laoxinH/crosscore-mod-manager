package top.laoxin.modmanager.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 远程服务调用结果 用于跨进程传递操作结果和详细错误信息 */
@Parcelize
data class RemoteResult(
        val success: Boolean,
        val errorCode: Int = ERROR_NONE,
        val errorMessage: String = ""
) : Parcelable {

    companion object {
        // ==================== 成功 ====================
        const val ERROR_NONE = 0

        // ==================== 通用错误 (1-99) ====================
        /** 未知错误 */
        const val ERROR_UNKNOWN = 1
        /** 参数无效 */
        const val ERROR_INVALID_ARGUMENT = 2
        /** 操作不支持 */
        const val ERROR_NOT_SUPPORTED = 3
        /** 操作被取消 */
        const val ERROR_CANCELLED = 4

        // ==================== 权限错误 (100-199) ====================
        /** 权限不足 */
        const val ERROR_PERMISSION_DENIED = 100
        /** 访问被拒绝 */
        const val ERROR_ACCESS_DENIED = 101
        /** 需要 root 权限 */
        const val ERROR_ROOT_REQUIRED = 102

        // ==================== 文件错误 (200-299) ====================
        /** 文件不存在 */
        const val ERROR_FILE_NOT_FOUND = 200
        /** 路径无效 */
        const val ERROR_INVALID_PATH = 201
        /** 文件已存在 */
        const val ERROR_FILE_ALREADY_EXISTS = 202
        /** 不是文件 */
        const val ERROR_NOT_A_FILE = 203
        /** 不是目录 */
        const val ERROR_NOT_A_DIRECTORY = 204
        /** 目录不为空 */
        const val ERROR_DIRECTORY_NOT_EMPTY = 205
        /** 磁盘空间不足 */
        const val ERROR_NO_SPACE = 206
        /** 文件名无效 */
        const val ERROR_INVALID_FILENAME = 207
        /** 路径过长 */
        const val ERROR_PATH_TOO_LONG = 208

        // ==================== 文件操作错误 (300-399) ====================
        /** 复制失败 */
        const val ERROR_COPY_FAILED = 300
        /** 删除失败 */
        const val ERROR_DELETE_FAILED = 301
        /** 移动失败 */
        const val ERROR_MOVE_FAILED = 302
        /** 创建失败 */
        const val ERROR_CREATE_FAILED = 303
        /** 写入失败 */
        const val ERROR_WRITE_FAILED = 304
        /** 读取失败 */
        const val ERROR_READ_FAILED = 305
        /** 重命名失败 */
        const val ERROR_RENAME_FAILED = 306
        /** 打开文件失败 */
        const val ERROR_OPEN_FAILED = 307
        /** 关闭文件失败 */
        const val ERROR_CLOSE_FAILED = 308
        /** 刷新失败 */
        const val ERROR_FLUSH_FAILED = 309
        /** 文件被占用 */
        const val ERROR_FILE_IN_USE = 310

        // ==================== IO 错误 (400-499) ====================
        /** IO 错误 */
        const val ERROR_IO = 400
        /** 流已关闭 */
        const val ERROR_STREAM_CLOSED = 401
        /** 读取超时 */
        const val ERROR_READ_TIMEOUT = 402
        /** 写入超时 */
        const val ERROR_WRITE_TIMEOUT = 403

        // ==================== 工厂方法 ====================

        /** 成功结果 */
        fun success(): RemoteResult = RemoteResult(success = true)

        /** 失败结果 */
        fun error(errorCode: Int, message: String = ""): RemoteResult =
                RemoteResult(success = false, errorCode = errorCode, errorMessage = message)

        /** 未知错误 */
        fun unknownError(message: String = ""): RemoteResult = error(ERROR_UNKNOWN, message)

        /** 权限不足 */
        fun permissionDenied(message: String = ""): RemoteResult =
                error(ERROR_PERMISSION_DENIED, message)

        /** 文件不存在 */
        fun fileNotFound(path: String = ""): RemoteResult =
                error(ERROR_FILE_NOT_FOUND, if (path.isNotEmpty()) "文件不存在: $path" else "文件不存在")

        /** 复制失败 */
        fun copyFailed(message: String = ""): RemoteResult = error(ERROR_COPY_FAILED, message)

        /** 删除失败 */
        fun deleteFailed(message: String = ""): RemoteResult = error(ERROR_DELETE_FAILED, message)

        /** 移动失败 */
        fun moveFailed(message: String = ""): RemoteResult = error(ERROR_MOVE_FAILED, message)

        /** 创建失败 */
        fun createFailed(message: String = ""): RemoteResult = error(ERROR_CREATE_FAILED, message)

        /** 写入失败 */
        fun writeFailed(message: String = ""): RemoteResult = error(ERROR_WRITE_FAILED, message)

        /** 读取失败 */
        fun readFailed(message: String = ""): RemoteResult = error(ERROR_READ_FAILED, message)

        /** IO 错误 */
        fun ioError(message: String = ""): RemoteResult = error(ERROR_IO, message)
    }

    /** 是否失败 */
    val isError: Boolean
        get() = !success

    /** 获取错误描述 */
    fun getErrorDescription(): String {
        if (success) return "成功"
        return when (errorCode) {
            ERROR_UNKNOWN -> "未知错误"
            ERROR_INVALID_ARGUMENT -> "参数无效"
            ERROR_NOT_SUPPORTED -> "操作不支持"
            ERROR_CANCELLED -> "操作被取消"
            ERROR_PERMISSION_DENIED -> "权限不足"
            ERROR_ACCESS_DENIED -> "访问被拒绝"
            ERROR_ROOT_REQUIRED -> "需要 root 权限"
            ERROR_FILE_NOT_FOUND -> "文件不存在"
            ERROR_INVALID_PATH -> "路径无效"
            ERROR_FILE_ALREADY_EXISTS -> "文件已存在"
            ERROR_NOT_A_FILE -> "不是文件"
            ERROR_NOT_A_DIRECTORY -> "不是目录"
            ERROR_DIRECTORY_NOT_EMPTY -> "目录不为空"
            ERROR_NO_SPACE -> "磁盘空间不足"
            ERROR_INVALID_FILENAME -> "文件名无效"
            ERROR_PATH_TOO_LONG -> "路径过长"
            ERROR_COPY_FAILED -> "复制失败"
            ERROR_DELETE_FAILED -> "删除失败"
            ERROR_MOVE_FAILED -> "移动失败"
            ERROR_CREATE_FAILED -> "创建失败"
            ERROR_WRITE_FAILED -> "写入失败"
            ERROR_READ_FAILED -> "读取失败"
            ERROR_RENAME_FAILED -> "重命名失败"
            ERROR_OPEN_FAILED -> "打开文件失败"
            ERROR_CLOSE_FAILED -> "关闭文件失败"
            ERROR_FLUSH_FAILED -> "刷新失败"
            ERROR_FILE_IN_USE -> "文件被占用"
            ERROR_IO -> "IO 错误"
            ERROR_STREAM_CLOSED -> "流已关闭"
            ERROR_READ_TIMEOUT -> "读取超时"
            ERROR_WRITE_TIMEOUT -> "写入超时"
            else -> "错误码: $errorCode"
        } + if (errorMessage.isNotEmpty()) " - $errorMessage" else ""
    }
}
