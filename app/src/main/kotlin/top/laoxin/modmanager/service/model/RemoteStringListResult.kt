package top.laoxin.modmanager.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 远程服务调用结果 - List<String> 类型 */
@Parcelize
data class RemoteStringListResult(
        val success: Boolean,
        val data: List<String> = emptyList(),
        val errorCode: Int = RemoteResult.ERROR_NONE,
        val errorMessage: String = ""
) : Parcelable {

    companion object {
        fun success(data: List<String>): RemoteStringListResult =
                RemoteStringListResult(success = true, data = data)

        fun error(errorCode: Int, message: String = ""): RemoteStringListResult =
                RemoteStringListResult(
                        success = false,
                        errorCode = errorCode,
                        errorMessage = message
                )

        fun fileNotFound(path: String = ""): RemoteStringListResult =
                error(
                        RemoteResult.ERROR_FILE_NOT_FOUND,
                        if (path.isNotEmpty()) "文件不存在: $path" else "文件不存在"
                )

        fun permissionDenied(message: String = ""): RemoteStringListResult =
                error(RemoteResult.ERROR_PERMISSION_DENIED, message)
    }

    val isError: Boolean
        get() = !success
}
