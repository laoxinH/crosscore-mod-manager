package top.laoxin.modmanager.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 远程服务调用结果 - String 类型 */
@Parcelize
data class RemoteStringResult(
        val success: Boolean,
        val data: String = "",
        val errorCode: Int = RemoteResult.ERROR_NONE,
        val errorMessage: String = ""
) : Parcelable {

    companion object {
        fun success(data: String): RemoteStringResult =
                RemoteStringResult(success = true, data = data)

        fun error(errorCode: Int, message: String = ""): RemoteStringResult =
                RemoteStringResult(success = false, errorCode = errorCode, errorMessage = message)

        fun fileNotFound(path: String = ""): RemoteStringResult =
                error(
                        RemoteResult.ERROR_FILE_NOT_FOUND,
                        if (path.isNotEmpty()) "文件不存在: $path" else "文件不存在"
                )

        fun permissionDenied(message: String = ""): RemoteStringResult =
                error(RemoteResult.ERROR_PERMISSION_DENIED, message)

        fun readFailed(message: String = ""): RemoteStringResult =
                error(RemoteResult.ERROR_READ_FAILED, message)
    }

    val isError: Boolean
        get() = !success
}
