package top.laoxin.modmanager.tools.fileToolsInterface.impl

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import top.laoxin.modmanager.App
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import java.io.File
import java.io.InputStream

object DocumentFileTools : BaseFileTools {
    private const val TAG = "DocumentFileTools"
    private val app = App.get()

    override fun deleteFile(path: String): Boolean {
        val pathUri = pathToUri(path)
        val app = App.get()
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        return try {
            documentFile?.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteFile: $e")
            false
        }
    }

    override fun copyFile(srcPath: String, destPath: String): Boolean {
        Log.d(TAG, "复制文件: $srcPath---$destPath")
        return try {
            val srcPathUri = pathToUri(srcPath)
            val destPathUri = pathToUri(destPath)

            val srcDocumentFile = DocumentFile.fromTreeUri(app, srcPathUri)
            var destDocumentFile = DocumentFile.fromTreeUri(app, destPathUri)
            try {
                val file = File(destPath)
                if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
                if (!file.exists()) file.createNewFile()
            } catch (e: Exception) {
                Log.e(TAG, "copyFile 创建父目录 : ${e}")
            }
            if (destDocumentFile?.exists() == true) {
                destDocumentFile.delete()
                val parentFile = File(destPath).parentFile?.absolutePath
                val parentDocumentFile = DocumentFile.fromTreeUri(app, pathToUri(parentFile!!))
                parentDocumentFile?.createFile("application/octet-stream", File(destPath).name)
                    .let {
                        destDocumentFile = it
                    }
            }


            val openInputStream =
                app.contentResolver.openInputStream(srcDocumentFile?.uri!!)
            val openOutputStream =
                app.contentResolver.openOutputStream(destDocumentFile?.uri!!)
            openInputStream.use { inputStream ->
                openOutputStream.use { outputStream ->
                    inputStream?.copyTo(outputStream ?: return false)
                    outputStream?.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "copyFile失败: ${e}")
            LogTools.logRecord("$TAG copyFile失败: ${e}")
            false
        }
    }

    override fun getFilesNames(path: String): MutableList<String> {
        val pathUri = pathToUri(path)
        val list: MutableList<String> = ArrayList()
        try {
            val documentFile = DocumentFile.fromTreeUri(app, pathUri)
            documentFile?.listFiles()?.forEach {
                list.add(it.name!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFilesNames: $e")
        }
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

        return try {
            file?.let {
                app.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeFile: $e")
            false
        }
    }

    override fun moveFile(srcPath: String, destPath: String): Boolean {
        if (srcPath == destPath) {
            return true
        }
        return try {
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "moveFile: $e")
            false
        }

    }

    override fun isFileExist(path: String): Boolean {
        return try {
            val pathUri = pathToUri(path)
            val documentFile = DocumentFile.fromTreeUri(app, pathUri)
            documentFile?.exists() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "isFileExist: $e")
            false
        }

    }

    override fun isFile(filename: String): Boolean {
        return try {
            val pathUri = pathToUri(filename)
            val documentFile = DocumentFile.fromTreeUri(app, pathUri)
            documentFile?.isFile ?: false
        } catch (e: Exception) {
            Log.e(TAG, "isFile: $e")
            false
        }
    }

    override fun createFileByStream(
        path: String,
        filename: String,
        inputStream: InputStream?
    ): Boolean {
        val pathUri = pathToUri(path)
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        val file = documentFile?.createFile("application/octet-stream", filename)
        return try {
            file?.let {
                app.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                    inputStream?.use { input ->
                        input.copyTo(outputStream)
                        outputStream.flush()
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "createFileByStream: $e")
            false
        }
    }

    override fun isFileChanged(path: String): Long {
        val file = DocumentFile.fromTreeUri(app, pathToUri(path))
        return file?.lastModified() ?: 0
    }

    override fun changDictionaryName(path: String, name: String): Boolean {
        try {
            val file = DocumentFile.fromTreeUri(app, pathToUri(path))
            return file?.renameTo(name) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "changDictionaryName: $e")
            return false
        }

    }

    override fun createDictionary(path: String): Boolean {
        try {
            if (isFileExist(path)) {
                return true
            }
            val absolutePath = File(path).parentFile?.absolutePath
            val pathUri = pathToUri(absolutePath!!)
            val documentFile = DocumentFile.fromTreeUri(app, pathUri)
            val createDirectory = documentFile?.createDirectory(File(path).name)
            if (createDirectory == null) {
                Log.e(TAG, "createDictionary: 创建文件夹失败")
                return false
            } else {
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "createDictionary: $e")
            return false
        }
    }
}