package top.laoxin.modmanager.data.service.filetools.impl

import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.App
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.data.service.filetools.BaseFileTools
import java.io.FileNotFoundException

/** DocumentFile 文件系统操作工具（SAF） 不捕获异常，让异常传播到 FileServiceImpl 统一处理 */
@Singleton
class DocumentFileTools @Inject constructor() : BaseFileTools() {

    private val app = App.get()

    companion object {
        const val TAG = "DocumentFileTools"
    }

    override fun deleteFile(path: String): Boolean {
        val pathUri = pathToUri(path)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        return documentFile?.delete() ?: false
    }

    override fun copyFile(srcPath: String, destPath: String): Boolean {
        Log.d(TAG, "复制文件: $srcPath---$destPath")
        val srcPathUri = pathToUri(srcPath)
        val destPathUri = pathToUri(destPath)

        val srcDocumentFile = DocumentFile.fromTreeUri(app, srcPathUri)
        var destDocumentFile = DocumentFile.fromTreeUri(app, destPathUri)

        // 创建目标文件的父目录
        val file = File(destPath)
        if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()

        if (destDocumentFile?.exists() == true) {
            destDocumentFile.delete()
            val parentFile = File(destPath).parentFile?.absolutePath
            val parentDocumentFile = DocumentFile.fromTreeUri(app, pathToUri(parentFile!!))
            parentDocumentFile?.createFile("application/octet-stream", File(destPath).name).let {
                destDocumentFile = it
            }
        }

        val openInputStream = app.contentResolver.openInputStream(srcDocumentFile?.uri!!)
        val openOutputStream = app.contentResolver.openOutputStream(destDocumentFile?.uri!!)
        openInputStream.use { inputStream ->
            openOutputStream.use { outputStream ->
                inputStream?.copyTo(outputStream ?: throw IllegalStateException("无法打开输出流"))
                outputStream?.flush()
            }
        }
        return true
    }

    override fun getFilesNames(path: String): MutableList<String> {
        val pathUri = pathToUri(path)
        val list: MutableList<String> = ArrayList()
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        documentFile?.listFiles()?.forEach { it.name?.let { name -> list.add(name) } }
        return list
    }

    override fun writeFile(path: String, filename: String, content: String): Boolean {
        val pathUri = pathToUri(path)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        val existingFile = documentFile?.findFile(filename)

        if (existingFile?.exists() == true) {
            existingFile.delete()
        }
        val file = documentFile?.createFile("application/octet-stream", filename)

        file?.let {
            app.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
                outputStream.flush()
            }
        }
        return true
    }

    override fun moveFile(srcPath: String, destPath: String): Boolean {
        if (srcPath == destPath) {
            return true
        }
        val srcPathUri = pathToUri(srcPath)
        val destPathUri = pathToUri(destPath)

        val inputStream = app.contentResolver.openInputStream(srcPathUri)
        val outputStream = app.contentResolver.openOutputStream(destPathUri)
        inputStream?.use { input ->
            outputStream?.use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        DocumentFile.fromTreeUri(app, srcPathUri)?.delete()
        return true
    }

    override fun isFileExist(path: String): Boolean {
        val pathUri = pathToUri(path)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        return documentFile?.exists() == true
    }

    override fun isFile(filename: String): Boolean {
        val pathUri = pathToUri(filename)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        return documentFile?.isFile == true
    }

    override fun createFileByStream(
        path: String,
        filename: String,
        inputStream: InputStream
    ): Boolean {

        val pathUri = pathToUri(path)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        val existingFile = documentFile?.findFile(filename)

        if (existingFile?.exists() == true) {
            existingFile.delete()
        }

        val file = documentFile?.createFile("application/octet-stream", filename)

        file?.let {
            app.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                    outputStream.flush()
                }
            }
        }
        return true
    }

    override fun isFileChanged(path: String): Long {
        val file = DocumentFile.fromTreeUri(app, pathToUri(path))
        return file?.lastModified() ?: 0
    }

    override fun changDictionaryName(path: String, name: String): Boolean {
        val file = DocumentFile.fromTreeUri(app, pathToUri(path))
        return file?.renameTo(name) ?: false
    }

    override fun createDictionary(path: String): Boolean {
        if (isFileExist(path)) {
            return true
        }
        val absolutePath =
            File(path).parentFile?.absolutePath
                ?: throw IllegalArgumentException("无效的路径: $path")
        val pathUri = pathToUri(absolutePath)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        val createDirectory = documentFile?.createDirectory(File(path).name)
        return createDirectory != null
    }

    override fun readFile(path: String): String {
        val file = DocumentFile.fromTreeUri(app, pathToUri(path))
        return file?.let {
            app.contentResolver.openInputStream(it.uri)?.bufferedReader()?.use { reader ->
                reader.readText()
            }
        }
            ?: ""
    }

    override fun listFiles(path: String): MutableList<File> {
        val documentFile = DocumentFile.fromTreeUri(app, pathToUri(path))
        val list = mutableListOf<File>()
        documentFile?.listFiles()?.forEach { docFile ->
            val filePath = uriToPath(docFile.uri)
            if (filePath.isNotEmpty()) {
               // Log.d(TAG, "listFiles: $filePath")
                list.add(File(filePath))
            }
        }
        return list
    }

    /**
     * 将 DocumentFile URI 转换回文件系统路径
     * URI 格式: content://com.android.externalstorage.documents/tree/primary:Android/.../document/primary:Android/data/...
     * 需要提取 document 后面的 primary: 部分
     */
    private fun uriToPath(uri: android.net.Uri): String {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            // docId 格式: "primary:Android/data/com.xxx/..."
            if (docId.startsWith("primary:")) {
                "${PathConstants.ROOT_PATH}/${docId.removePrefix("primary:")}"
            } else {
                // 其他存储卷，如 SD 卡
                val parts = docId.split(":", limit = 2)
                if (parts.size == 2) {
                    "/storage/${parts[0]}/${parts[1]}"
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert URI to path: $uri", e)
            ""
        }
    }

    override fun calculateFileMd5(path: String): String {
        val documentFile = DocumentFile.fromTreeUri(app, pathToUri(path))
        if (documentFile == null || !documentFile.exists()) {
            throw FileNotFoundException(path)
        }

        val digest = java.security.MessageDigest.getInstance("MD5")
        app.contentResolver.openInputStream(documentFile.uri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
            ?: return ""

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override fun getFileSize(path: String): Long {
        val documentFile = DocumentFile.fromTreeUri(app, pathToUri(path))
        if (documentFile == null || !documentFile.exists()) {
            throw FileNotFoundException("文件不存在: $path")
        }
        return documentFile.length()
    }
}
