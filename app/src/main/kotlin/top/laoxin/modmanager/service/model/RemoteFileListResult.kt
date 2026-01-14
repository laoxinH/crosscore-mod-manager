package top.laoxin.modmanager.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import top.laoxin.modmanager.service.bean.FileInfoBean

/** 远程服务调用结果 - List<FileInfoBean> 类型 */
@Parcelize
data class RemoteFileListResult(
        val success: Boolean,
        val data: List<FileInfoBean> = emptyList(),
        val errorCode: Int = RemoteResult.ERROR_NONE,
        val errorMessage: String = ""
) : Parcelable {

    companion object {
        fun success(data: List<FileInfoBean>): RemoteFileListResult =
                RemoteFileListResult(success = true, data = data)

        fun error(errorCode: Int, message: String = ""): RemoteFileListResult =
                RemoteFileListResult(success = false, errorCode = errorCode, errorMessage = message)

        fun fileNotFound(path: String = ""): RemoteFileListResult =
                error(
                        RemoteResult.ERROR_FILE_NOT_FOUND,
                        if (path.isNotEmpty()) "文件不存在: $path" else "文件不存在"
                )

        fun permissionDenied(message: String = ""): RemoteFileListResult =
                error(RemoteResult.ERROR_PERMISSION_DENIED, message)
    }

    val isError: Boolean
        get() = !success
}
