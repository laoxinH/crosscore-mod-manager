package top.laoxin.modmanager.data.service.filetools

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import top.laoxin.modmanager.App
import top.laoxin.modmanager.constant.PathConstants

abstract class BaseFileTools() {

    // 通过路径删除文件
    abstract fun deleteFile(path: String): Boolean

    // 通过srcPath和destPath路径复制文件
    abstract fun copyFile(srcPath: String, destPath: String): Boolean

    // 通过path路径获取文件列表
    abstract fun getFilesNames(path: String): MutableList<String>

    // 写入文件
    abstract fun writeFile(path: String, filename: String, content: String): Boolean

    // 移动文件
    abstract fun moveFile(srcPath: String, destPath: String): Boolean

    // 判断文件是否存在
    abstract fun isFileExist(path: String): Boolean

    // 判断是否未文件
    abstract fun isFile(filename: String): Boolean

    // 通过流创建文件
    abstract fun createFileByStream(
            path: String,
            filename: String,
            inputStream: InputStream
    ): Boolean

    // 监听文件变化
    abstract fun isFileChanged(path: String): Long

    // 创建文件夹

    // 通过DocumentFile和File复制文件
    fun copyFileByDF(srcPath: String, destPath: String): Boolean {
        return try {
            val app = App.get()
            val srcPathUri = pathToUri(srcPath)
            val inputStream = app.contentResolver.openInputStream(srcPathUri)
            val file = File(destPath)
            if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
            if (file.exists()) {
                file.delete()
            } else {
                file.createNewFile()
            }
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FileTools", "copyFileByDF: $e")

            false
        }
    }

    fun isExcludeFileType(filename: String): Boolean {
        return (filename.contains(".jpg", ignoreCase = true) ||
                filename.contains(".png", ignoreCase = true) ||
                filename.contains(".gif", ignoreCase = true) ||
                filename.contains(".jpeg", ignoreCase = true) ||
                filename.contains(".mp4", ignoreCase = true) ||
                filename.contains(".mp3", ignoreCase = true) ||
                filename.contains(".apk", ignoreCase = true))
    }

    fun pathToUri(path: String): Uri {
        val halfPath = path.replace("${PathConstants.ROOT_PATH}/", "")
        val segments = halfPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val uriBuilder =
                Uri.Builder()
                        .scheme("content")
                        .authority("com.android.externalstorage.documents")
                        .appendPath("tree")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uriBuilder.appendPath("primary:Android/" + segments[1] + "/" + segments[2])
        } else {
            uriBuilder.appendPath("primary:Android/" + segments[1])
        }
        uriBuilder.appendPath("document").appendPath("primary:$halfPath")
        return uriBuilder.build()
    }

    fun copyFileByFD(srcPath: String, destPath: String): Boolean {
        if (!File(srcPath).exists()) return false
        return try {
            val app = App.get()
            val destPathUri = pathToUri(destPath)
            var destDocumentFile = DocumentFile.fromTreeUri(app, destPathUri)

            if (destDocumentFile?.exists() == true) {
                destDocumentFile.delete()
            }
            val parentFile = File(destPath).parentFile?.absolutePath
            val parentDocumentFile = DocumentFile.fromTreeUri(app, pathToUri(parentFile!!))
            Log.d("FileTools", "copyFileByFD: 开始创建文件")

            parentDocumentFile!!.createFile("application/octet-stream", File(destPath).name)!!.let {
                Log.d("FileTools", "copyFileByFD: 开始创建文件")

                destDocumentFile = it
            }

            val outputStream = app.contentResolver.openOutputStream(destDocumentFile?.uri!!)
            val inputStream = File(srcPath).inputStream()
            inputStream.use { input ->
                outputStream?.use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isDataPath(path: String): Boolean {
        return (("${PathConstants.ROOT_PATH}/Android/data") == path)
    }

    fun isObbPath(path: String): Boolean {
        return (("${PathConstants.ROOT_PATH}/Android/obb") == path)
    }

    private fun isUnderDataPath(path: String): Boolean {
        return path.contains("${PathConstants.ROOT_PATH}/Android/data/")
    }

    private fun isUnderObbPath(path: String): Boolean {
        return path.contains("${PathConstants.ROOT_PATH}/Android/obb/")
    }

    abstract fun changDictionaryName(path: String, name: String): Boolean
    abstract fun createDictionary(path: String): Boolean

    // 读取文件字符
    abstract fun readFile(path: String): String

    // 列出文件夹下所有文件
    abstract fun listFiles(path: String): MutableList<File>


    abstract fun calculateFileMd5(path: String): String

    // 读取文件大小
    abstract fun getFileSize(path: String): Long

    /** 如果字符串是应用包名，返回字符串，反之返回null */
}
