package top.laoxin.modmanager.tools.fileToolsInterface


import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import top.laoxin.modmanager.App
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.DocumentFileTools
import java.io.File

interface BaseFileTools {

    // 通过路径删除文件
    fun deleteFile(path: String): Boolean

    // 通过srcPath和destPath路径复制文件
    fun copyFile(srcPath: String, destPath: String): Boolean

    // 通过path路径获取文件列表
    fun getFilesNames(path: String): MutableList<String>

    // 写入文件
    fun writeFile(path: String,filename : String, content: String): Boolean

    // 移动文件
    fun moveFile(srcPath: String, destPath: String): Boolean

    // 判断文件是否存在
    fun isFileExist(path: String): Boolean

    // 判断是否未文件
    fun isFile(filename: String): Boolean

    // 通过DocumentFile和File复制文件
    fun copyFileByDF(srcPath: String, destPath: String): Boolean {
        return try {
            val app = App.get()
            val srcPathUri = DocumentFileTools.pathToUri(srcPath)
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

    fun isFileType(filename: String): Boolean {

        return (/*name.contains(".jpg", ignoreCase = true) ||
                name.contains(".png", ignoreCase = true) ||
                name.contains(".gif", ignoreCase = true) ||
                name.contains(".jpeg", ignoreCase = true) ||
                name.contains(".mp4", ignoreCase = true) ||
                name.contains(".mp3", ignoreCase = true) ||*/
                filename.contains(".apk", ignoreCase = true))
    }

    fun pathToUri(path: String): Uri {
        val halfPath = path.replace("${ModTools.ROOT_PATH}/", "")
        val segments = halfPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val uriBuilder = Uri.Builder()
            .scheme("content")
            .authority("com.android.externalstorage.documents")
            .appendPath("tree")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uriBuilder.appendPath("primary:Android/" + segments[1] + "/" + segments[2])
        } else {
            uriBuilder.appendPath("primary:Android/" + segments[1])
        }
        uriBuilder.appendPath("document")
            .appendPath("primary:$halfPath")
        return uriBuilder.build()
    }

    fun copyFileByFD(srcPath: String, destPath: String): Boolean{
        if (!File(srcPath).exists()) return false
        return try {
            val app = App.get()
            val destPathUri = DocumentFileTools.pathToUri(destPath)
            var destDocumentFile = DocumentFile.fromTreeUri(app, destPathUri)

            if (destDocumentFile?.exists() == true) {
                destDocumentFile.delete()
                val parentFile = File(destPath).parentFile?.absolutePath
                val parentDocumentFile = DocumentFile.fromTreeUri(
                    app,
                    pathToUri(parentFile!!)
                )
                parentDocumentFile?.createFile("application/octet-stream",File(destPath).name)?.let {
                    destDocumentFile = it
                }
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
            Log.e("FileTools", "copyFileByDF: $e")
            false
        }
    }
}