package top.laoxin.modmanager.data.service.filetools.impl

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.service.filetools.BaseFileTools
import top.laoxin.modmanager.service.model.RemoteBoolResult
import top.laoxin.modmanager.service.model.RemoteFileListResult
import top.laoxin.modmanager.service.model.RemoteLongResult
import top.laoxin.modmanager.service.model.RemoteResult
import top.laoxin.modmanager.service.model.RemoteStringListResult
import top.laoxin.modmanager.service.model.RemoteStringResult
import top.laoxin.modmanager.service.shizuku.IFileExplorerService

/** Shizuku 文件系统操作工具 不捕获异常，让异常传播到 FileServiceImpl 统一处理 根据 RemoteResult 错误码抛出相应异常 */
@Singleton
class ShizukuFileTools @Inject constructor(
    private val scope : CoroutineScope
) : BaseFileTools() {

    companion object {
        const val TAG = "ShizukuFileTools"
        var iFileExplorerService: IFileExplorerService? = null
    }

    // 使用 SupervisorJob 确保单个协程失败不会影响其他协程
   //private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun getService(): IFileExplorerService {
        return iFileExplorerService ?: throw IllegalStateException("Shizuku 服务未连接")
    }

    /** 检查 RemoteResult 并在失败时抛出异常 */
    private fun checkResult(result: RemoteResult) {
        if (!result.success) {
            throw when (result.errorCode) {
                RemoteResult.ERROR_PERMISSION_DENIED, RemoteResult.ERROR_ACCESS_DENIED -> {
                    SecurityException(result.getErrorDescription())
                }
                RemoteResult.ERROR_FILE_NOT_FOUND -> {
                    java.io.FileNotFoundException(result.getErrorDescription())
                }
                RemoteResult.ERROR_IO,
                RemoteResult.ERROR_COPY_FAILED,
                RemoteResult.ERROR_DELETE_FAILED,
                RemoteResult.ERROR_MOVE_FAILED,
                RemoteResult.ERROR_WRITE_FAILED,
                RemoteResult.ERROR_READ_FAILED -> {
                    IOException(result.getErrorDescription())
                }
                RemoteResult.ERROR_INVALID_ARGUMENT,
                RemoteResult.ERROR_INVALID_PATH,
                RemoteResult.ERROR_INVALID_FILENAME -> {
                    IllegalArgumentException(result.getErrorDescription())
                }
                else -> {
                    RuntimeException(result.getErrorDescription())
                }
            }
        }
    }

    /** 检查 RemoteBoolResult 并在失败时抛出异常 */
    private fun checkBoolResult(result: RemoteBoolResult): Boolean {
        if (!result.success) {
            throw when (result.errorCode) {
                RemoteResult.ERROR_PERMISSION_DENIED, RemoteResult.ERROR_ACCESS_DENIED ->
                        SecurityException(result.errorMessage)
                RemoteResult.ERROR_FILE_NOT_FOUND ->
                        java.io.FileNotFoundException(result.errorMessage)
                else -> RuntimeException(result.errorMessage)
            }
        }
        return result.data
    }

    /** 检查 RemoteLongResult 并在失败时抛出异常 */
    private fun checkLongResult(result: RemoteLongResult): Long {
        if (!result.success) {
            throw when (result.errorCode) {
                RemoteResult.ERROR_PERMISSION_DENIED, RemoteResult.ERROR_ACCESS_DENIED ->
                        SecurityException(result.errorMessage)
                RemoteResult.ERROR_FILE_NOT_FOUND ->
                        java.io.FileNotFoundException(result.errorMessage)
                else -> RuntimeException(result.errorMessage)
            }
        }
        return result.data
    }

    /** 检查 RemoteStringResult 并在失败时抛出异常 */
    private fun checkStringResult(result: RemoteStringResult): String {
        if (!result.success) {
            throw when (result.errorCode) {
                RemoteResult.ERROR_PERMISSION_DENIED, RemoteResult.ERROR_ACCESS_DENIED ->
                        SecurityException(result.errorMessage)
                RemoteResult.ERROR_FILE_NOT_FOUND ->
                        java.io.FileNotFoundException(result.errorMessage)
                RemoteResult.ERROR_READ_FAILED -> IOException(result.errorMessage)
                else -> RuntimeException(result.errorMessage)
            }
        }
        return result.data
    }

    /** 检查 RemoteStringListResult 并在失败时抛出异常 */
    private fun checkStringListResult(result: RemoteStringListResult): List<String> {
        if (!result.success) {
            throw when (result.errorCode) {
                RemoteResult.ERROR_PERMISSION_DENIED, RemoteResult.ERROR_ACCESS_DENIED ->
                        SecurityException(result.errorMessage)
                RemoteResult.ERROR_FILE_NOT_FOUND ->
                        java.io.FileNotFoundException(result.errorMessage)
                else -> RuntimeException(result.errorMessage)
            }
        }
        return result.data
    }

    /** 检查 RemoteFileListResult 并在失败时抛出异常 */
    private fun checkFileListResult(result: RemoteFileListResult): List<File> {
        if (!result.success) {
            throw when (result.errorCode) {
                RemoteResult.ERROR_PERMISSION_DENIED, RemoteResult.ERROR_ACCESS_DENIED ->
                        SecurityException(result.errorMessage)
                RemoteResult.ERROR_FILE_NOT_FOUND ->
                        java.io.FileNotFoundException(result.errorMessage)
                else -> RuntimeException(result.errorMessage)
            }
        }
        return result.data.map { File(it.path) }
    }

    override fun deleteFile(path: String): Boolean {
        val result = getService().deleteFile(path)
        checkResult(result)
        return true
    }

    override fun copyFile(srcPath: String, destPath: String): Boolean {
        val result = getService().copyFile(srcPath, destPath)
        checkResult(result)
        return true
    }

    override fun getFilesNames(path: String): MutableList<String> {
        val result = getService().getFilesNames(path)
        return checkStringListResult(result).toMutableList()
    }

    override fun writeFile(path: String, filename: String, content: String): Boolean {
        val result = getService().writeFile(path, filename, content)
        checkResult(result)
        return true
    }

    override fun moveFile(srcPath: String, destPath: String): Boolean {
        val result = getService().moveFile(srcPath, destPath)
        checkResult(result)
        return true
    }

    override fun isFileExist(path: String): Boolean {
        val result = getService().fileExists(path)
        return checkBoolResult(result)
    }

    override fun isFile(filename: String): Boolean {
        val result = getService().isFile(filename)
        return checkBoolResult(result)
    }

    override fun createFileByStream(
            path: String,
            filename: String,
            inputStream: InputStream
    ): Boolean {
        val pfd = getPfdFromInputStream(inputStream)
        val result = getService().createFileByStream(path, filename, pfd)
        checkResult(result)
        return true
    }

    override fun isFileChanged(path: String): Long {
        val result = getService().getLastModified(path)
        return checkLongResult(result)
    }

    override fun changDictionaryName(path: String, name: String): Boolean {
        val result = getService().changDictionaryName(path, name)
        checkResult(result)
        return true
    }

    override fun createDictionary(path: String): Boolean {
        val result = getService().createDictionary(path)
        checkResult(result)
        return true
    }

    override fun readFile(path: String): String {
        val result = getService().readFile(path)
        return checkStringResult(result)
    }

    override fun listFiles(path: String): MutableList<File> {
        val result = getService().listFile(path)
        return checkFileListResult(result).toMutableList()
    }

    override fun calculateFileMd5(path: String): String {
        val result = getService().md5(path)
        return checkStringResult(result)
    }

    override fun getFileSize(path: String): Long {
        val result = getService().getFileSize(path)
        return checkLongResult(result)
    }

    /** 从 InputStream 创建 ParcelFileDescriptor */
    private fun getPfdFromInputStream(inputStream: InputStream): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide: ParcelFileDescriptor = pipe[0]
        val writeSide: ParcelFileDescriptor = pipe[1]

        // 必须在单独的协程中写入数据！
        // 原因：管道缓冲区有限（通常64KB），同步写入会在缓冲区满时阻塞，
        // 但此时远程服务还未开始读取（IPC调用尚未发起），导致死锁。
        scope.launch(Dispatchers.IO){
            var outputStream: OutputStream? = null
            try {
                outputStream = ParcelFileDescriptor.AutoCloseOutputStream(writeSide)
                inputStream.copyTo(outputStream)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to pipe", e)
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to close inputStream", e)
                }
                try {
                    outputStream?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to close outputStream", e)
                }
            }
        }

        return readSide
    }
}
