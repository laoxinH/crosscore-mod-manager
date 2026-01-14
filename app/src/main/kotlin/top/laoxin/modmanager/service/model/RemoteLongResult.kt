package top.laoxin.modmanager.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 远程服务调用结果 - Long 类型 */
@Parcelize
data class RemoteLongResult(
        val success: Boolean,
        val data: Long = 0L,
        val errorCode: Int = RemoteResult.ERROR_NONE,
        val errorMessage: String = ""
) : Parcelable {

    companion object {
        fun success(data: Long): RemoteLongResult = RemoteLongResult(success = true, data = data)

        fun error(errorCode: Int, message: String = ""): RemoteLongResult =
                RemoteLongResult(success = false, errorCode = errorCode, errorMessage = message)

        fun fileNotFound(path: String = ""): RemoteLongResult =
                error(
                        RemoteResult.ERROR_FILE_NOT_FOUND,
                        if (path.isNotEmpty()) "文件不存在: $path" else "文件不存在"
                )

        fun permissionDenied(message: String = ""): RemoteLongResult =
                error(RemoteResult.ERROR_PERMISSION_DENIED, message)
    }

    val isError: Boolean
        get() = !success
}
