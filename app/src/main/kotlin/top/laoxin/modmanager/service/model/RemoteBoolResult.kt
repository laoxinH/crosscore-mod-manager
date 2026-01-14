package top.laoxin.modmanager.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 远程服务调用结果 - Boolean 类型 */
@Parcelize
data class RemoteBoolResult(
        val success: Boolean,
        val data: Boolean = false,
        val errorCode: Int = RemoteResult.ERROR_NONE,
        val errorMessage: String = ""
) : Parcelable {

    companion object {
        fun success(data: Boolean): RemoteBoolResult = RemoteBoolResult(success = true, data = data)

        fun error(errorCode: Int, message: String = ""): RemoteBoolResult =
                RemoteBoolResult(success = false, errorCode = errorCode, errorMessage = message)

        fun fileNotFound(path: String = ""): RemoteBoolResult =
                error(
                        RemoteResult.ERROR_FILE_NOT_FOUND,
                        if (path.isNotEmpty()) "文件不存在: $path" else "文件不存在"
                )

        fun permissionDenied(message: String = ""): RemoteBoolResult =
                error(RemoteResult.ERROR_PERMISSION_DENIED, message)
    }

    val isError: Boolean
        get() = !success
}
