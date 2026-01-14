package top.laoxin.modmanager.service.shizuku

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import top.laoxin.modmanager.service.bean.FileInfoBean
import top.laoxin.modmanager.service.model.RemoteBoolResult
import top.laoxin.modmanager.service.model.RemoteFileListResult
import top.laoxin.modmanager.service.model.RemoteLongResult
import top.laoxin.modmanager.service.model.RemoteResult
import top.laoxin.modmanager.service.model.RemoteStringListResult
import top.laoxin.modmanager.service.model.RemoteStringResult

/** 文件浏览器服务实现 通过 Shizuku 提供高权限文件操作 所有方法返回封装的 Result 类型 */
class FileExplorerService : IFileExplorerService.Stub() {

    companion object {
        private const val TAG = "FileExplorerService"
    }

    // ==================== 查询操作 ====================

    @Throws(RemoteException::class)
    override fun getFilesNames(path: String?): RemoteStringListResult {
        if (path.isNullOrEmpty()) {
            return RemoteStringListResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteStringListResult.fileNotFound(path)
            }
            if (!file.isDirectory) {
                return RemoteStringListResult.error(
                        RemoteResult.ERROR_NOT_A_DIRECTORY,
                        "不是目录: $path"
                )
            }

            val files = file.listFiles()
            val list = mutableListOf<String>()
            files?.forEach { f ->
                if (!isMediaOrApkFile(f)) {
                    list.add(f.name)
                }
            }
            RemoteStringListResult.success(list)
        } catch (e: SecurityException) {
            Log.e(TAG, "getFilesNames 权限错误: $e")
            RemoteStringListResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "getFilesNames 错误: $e")
            RemoteStringListResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    override fun listFile(path: String?): RemoteFileListResult {
        if (path.isNullOrEmpty()) {
            return RemoteFileListResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteFileListResult.fileNotFound(path)
            }
            if (!file.isDirectory) {
                return RemoteFileListResult.error(RemoteResult.ERROR_NOT_A_DIRECTORY, "不是目录: $path")
            }

            val files = file.listFiles()
            val list =
                    files?.map {
                        FileInfoBean(
                                it.name,
                                it.path,
                                it.isDirectory,
                                it.length(),
                                it.lastModified()
                        )
                    }
                            ?: emptyList()
            RemoteFileListResult.success(list)
        } catch (e: SecurityException) {
            Log.e(TAG, "listFile 权限错误: $e")
            RemoteFileListResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "listFile 错误: $e")
            RemoteFileListResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    override fun fileExists(path: String?): RemoteBoolResult {
        if (path.isNullOrEmpty()) {
            return RemoteBoolResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val exists = File(path).exists()
            RemoteBoolResult.success(exists)
        } catch (e: SecurityException) {
            Log.e(TAG, "fileExists 权限错误: $e")
            RemoteBoolResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "fileExists 错误: $e")
            RemoteBoolResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    override fun isFile(path: String?): RemoteBoolResult {
        if (path.isNullOrEmpty()) {
            return RemoteBoolResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteBoolResult.fileNotFound(path)
            }
            RemoteBoolResult.success(file.isFile)
        } catch (e: SecurityException) {
            Log.e(TAG, "isFile 权限错误: $e")
            RemoteBoolResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "isFile 错误: $e")
            RemoteBoolResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    override fun getLastModified(path: String?): RemoteLongResult {
        if (path.isNullOrEmpty()) {
            return RemoteLongResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteLongResult.fileNotFound(path)
            }
            RemoteLongResult.success(file.lastModified())
        } catch (e: SecurityException) {
            Log.e(TAG, "getLastModified 权限错误: $e")
            RemoteLongResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "getLastModified 错误: $e")
            RemoteLongResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    override fun readFile(path: String?): RemoteStringResult {
        if (path.isNullOrEmpty()) {
            return RemoteStringResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteStringResult.fileNotFound(path)
            }
            if (!file.isFile) {
                return RemoteStringResult.error(RemoteResult.ERROR_NOT_A_FILE, "不是文件: $path")
            }
            RemoteStringResult.success(file.readText())
        } catch (e: SecurityException) {
            Log.e(TAG, "readFile 权限错误: $e")
            RemoteStringResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "readFile IO错误: $e")
            RemoteStringResult.readFailed(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "readFile 错误: $e")
            RemoteStringResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    // ==================== 文件操作 ====================

    override fun copyFile(srcPath: String?, destPath: String?): RemoteResult {
        if (srcPath.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "源路径为空")
        }
        if (destPath.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "目标路径为空")
        }

        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)

            if (!Files.exists(source)) {
                return RemoteResult.fileNotFound(srcPath)
            }

            File(destPath).parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    return RemoteResult.error(
                            RemoteResult.ERROR_CREATE_FAILED,
                            "无法创建目标目录: ${parent.absolutePath}"
                    )
                }
            }

            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            Log.d(TAG, "copyFile 成功: $srcPath -> $destPath")
            RemoteResult.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "copyFile 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "copyFile 文件不存在: $e")
            RemoteResult.fileNotFound(srcPath)
        } catch (e: IOException) {
            Log.e(TAG, "copyFile IO错误: $e")
            RemoteResult.ioError(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "copyFile 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun deleteFile(path: String?): RemoteResult {
        if (path.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteResult.fileNotFound(path)
            }

            Files.walk(Paths.get(path)).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach(Files::delete)
            }

            Log.d(TAG, "deleteFile 成功: $path")
            RemoteResult.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "deleteFile 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "deleteFile IO错误: $e")
            RemoteResult.deleteFailed(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "deleteFile 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun moveFile(srcPath: String?, destPath: String?): RemoteResult {
        if (srcPath.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "源路径为空")
        }
        if (destPath.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "目标路径为空")
        }
        if (srcPath == destPath) {
            return RemoteResult.success()
        }

        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)

            if (!Files.exists(source)) {
                return RemoteResult.fileNotFound(srcPath)
            }

            File(destPath).parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    return RemoteResult.error(
                            RemoteResult.ERROR_CREATE_FAILED,
                            "无法创建目标目录: ${parent.absolutePath}"
                    )
                }
            }

            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            Log.d(TAG, "moveFile 成功: $srcPath -> $destPath")
            RemoteResult.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "moveFile 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "moveFile IO错误: $e")
            RemoteResult.moveFailed(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "moveFile 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun writeFile(srcPath: String?, name: String?, content: String?): RemoteResult {
        if (srcPath.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }
        if (name.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "文件名为空")
        }
        if (content == null) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "内容为空")
        }

        return try {
            val file = File(srcPath, name)
            if (file.exists()) {
                file.delete()
            }
            file.writeText(content)
            Log.d(TAG, "writeFile 成功: ${file.absolutePath}")
            RemoteResult.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "writeFile 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "writeFile IO错误: $e")
            RemoteResult.writeFailed(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "writeFile 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun createFileByStream(
            path: String?,
            filename: String?,
            pfd: ParcelFileDescriptor?
    ): RemoteResult {
        Log.d(TAG, "shuzuku通过流创建文件: $path, $filename")
        if (path.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }
        if (filename.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "文件名为空")
        }
        if (pfd == null) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "文件描述符为空")
        }

        return try {
            pfd.use {
                val inputStream = ParcelFileDescriptor.AutoCloseInputStream(it)
                val file = File(path, filename)

                file.parentFile?.let { parent ->
                    if (!parent.exists() && !parent.mkdirs()) {
                        return RemoteResult.error(
                                RemoteResult.ERROR_CREATE_FAILED,
                                "无法创建目录: ${parent.absolutePath}"
                        )
                    }
                }

                if (file.exists()) {
                    file.delete()
                }

                file.createNewFile()
                FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }

                Log.d(TAG, "createFileByStream 成功: ${file.absolutePath}")
                RemoteResult.success()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "createFileByStream 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "createFileByStream IO错误: $e")
            RemoteResult.writeFailed(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "createFileByStream 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun createDictionary(path: String?): RemoteResult {
        if (path.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (file.exists()) {
                if (file.isDirectory) {
                    return RemoteResult.success()
                } else {
                    return RemoteResult.error(
                            RemoteResult.ERROR_NOT_A_DIRECTORY,
                            "路径已存在但不是目录: $path"
                    )
                }
            }

            if (file.mkdirs()) {
                Log.d(TAG, "createDictionary 成功: $path")
                RemoteResult.success()
            } else {
                RemoteResult.createFailed("创建目录失败: $path")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "createDictionary 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "createDictionary 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun changDictionaryName(path: String?, newName: String?): RemoteResult {
        if (path.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }
        if (newName.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "新名称为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteResult.fileNotFound(path)
            }

            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) {
                Log.d(TAG, "changDictionaryName 成功: $path -> ${newFile.absolutePath}")
                RemoteResult.success()
            } else {
                RemoteResult.error(RemoteResult.ERROR_RENAME_FAILED, "重命名失败")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "changDictionaryName 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "changDictionaryName 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    @SuppressLint("SetWorldReadable")
    override fun chmod(path: String?): RemoteResult {
        if (path.isNullOrEmpty()) {
            return RemoteResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteResult.fileNotFound(path)
            }

            val execResult = file.setExecutable(true, false)
            val readResult = file.setReadable(true, false)

            val command = "chmod 777 $path"
            Log.i(TAG, "chmod: command=$command, exec=$execResult, read=$readResult")
            Runtime.getRuntime().exec(command)

            RemoteResult.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "chmod 权限错误: $e")
            RemoteResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "chmod IO错误: $e")
            RemoteResult.ioError(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "chmod 未知错误: $e")
            RemoteResult.unknownError(e.message ?: "")
        }
    }

    override fun md5(path: String?): RemoteStringResult {
        if (path.isNullOrEmpty()) {
            return RemoteStringResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteStringResult.fileNotFound(path)
            }
            if (!file.isFile) {
                return RemoteStringResult.error(RemoteResult.ERROR_NOT_A_FILE, "不是文件: $path")
            }

            val digest = java.security.MessageDigest.getInstance("MD5")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (inputStream.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            
            val md5Hash = digest.digest().joinToString("") { "%02x".format(it) }
            RemoteStringResult.success(md5Hash)
        } catch (e: SecurityException) {
            Log.e(TAG, "md5 权限错误: $e")
            RemoteStringResult.permissionDenied(e.message ?: "")
        } catch (e: IOException) {
            Log.e(TAG, "md5 IO错误: $e")
            RemoteStringResult.error(RemoteResult.ERROR_READ_FAILED, e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "md5 错误: $e")
            RemoteStringResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }
    }

    override fun getFileSize(path: String?): RemoteLongResult? {
        // 读取文件大小
        if (path.isNullOrEmpty()) {
            return RemoteLongResult.error(RemoteResult.ERROR_INVALID_ARGUMENT, "路径为空")
        }
        return try {
            val file = File(path)
            if (!file.exists()) {
                return RemoteLongResult.fileNotFound(path)
            }
            RemoteLongResult.success(file.length())
        } catch (e: SecurityException) {
            Log.e(TAG, "getLastModified 权限错误: $e")
            RemoteLongResult.permissionDenied(e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "getLastModified 错误: $e")
            RemoteLongResult.error(RemoteResult.ERROR_UNKNOWN, e.message ?: "")
        }

    }

    // ==================== 辅助方法 ====================

    private fun isMediaOrApkFile(file: File): Boolean {
        val name = file.name.lowercase()
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "flv", "wmv")
        val audioExtensions = listOf("mp3", "wav", "aac", "flac", "ogg")
        val apkExtension = "apk"

        return imageExtensions.any { name.endsWith(it) } ||
                videoExtensions.any { name.endsWith(it) } ||
                audioExtensions.any { name.endsWith(it) } ||
                name.endsWith(apkExtension)
    }
}
