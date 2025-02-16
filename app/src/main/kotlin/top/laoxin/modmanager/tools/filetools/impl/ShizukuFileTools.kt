package top.laoxin.modmanager.tools.filetools.impl

import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.service.IFileExplorerService
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuFileTools @Inject constructor(
    appPathsManager: AppPathsManager
) : BaseFileTools(appPathsManager) {

    companion object {
        const val TAG = "ShizukuFileTools"
        var iFileExplorerService: IFileExplorerService? = null
    }

    override fun deleteFile(path: String): Boolean {
        return try {
            iFileExplorerService?.deleteFile(path)
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteFile: $e")
            false
        }
    }

    override fun copyFile(srcPath: String, destPath: String): Boolean {
        return try {
            iFileExplorerService?.copyFile(srcPath, destPath) == true
        } catch (e: Exception) {
            Log.e(TAG, "copyFile: $e")
            logRecord("ShizukuFileTools-copyFile: $e")
            false
        }
    }

    override fun getFilesNames(path: String): MutableList<String> {
        return try {
            iFileExplorerService?.getFilesNames(path) ?: mutableListOf()
        } catch (e: Exception) {
            Log.e(TAG, "getFilesNames: $e")
            mutableListOf()
        }
    }

    override fun writeFile(path: String, filename: String, content: String): Boolean {
        return try {
            iFileExplorerService?.writeFile(path, filename, content) == true
        } catch (e: Exception) {
            Log.e(TAG, "writeFile: $e")
            false
        }
    }

    override fun moveFile(srcPath: String, destPath: String): Boolean {
        return try {
            iFileExplorerService?.moveFile(srcPath, destPath) == true
        } catch (e: Exception) {
            Log.e(TAG, "moveFile: $e")
            false
        }
    }

    override fun isFileExist(path: String): Boolean {

        return try {

            iFileExplorerService?.fileExists(path) == true
        } catch (e: Exception) {
            Log.e(TAG, "isFileExist: $e")
            false
        }
    }

    override fun isFile(filename: String): Boolean {
        return try {
            iFileExplorerService?.isFile(filename) == true
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
        TODO("Not yet implemented")
    }

    override fun isFileChanged(path: String): Long {
        return try {
            iFileExplorerService?.isFileChanged(path) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "isFileChanged: $e")
            0
        }
    }

    override fun changDictionaryName(path: String, name: String): Boolean {
        Log.d(TAG, "changDictionaryName: $path==$name")
        return try {
            iFileExplorerService?.changDictionaryName(path, name) == true
        } catch (e: Exception) {
            Log.e(TAG, "changDictionaryName: $e")
            false
        }
    }

    override fun createDictionary(path: String): Boolean {
        return try {
            iFileExplorerService?.createDictionary(path) == true
        } catch (e: Exception) {
            Log.e(TAG, "createDictionary: $e")
            false
        }
    }

    override fun readFile(path: String): String {
        TODO("Not yet implemented")
    }

    override fun listFiles(path: String): MutableList<File> {
        TODO("Not yet implemented")
    }

    // 通过shizuku扫描mods
    suspend fun scanModsByShizuku(
        scanPath: String, gameInfo: GameInfoBean
    ): Boolean {
        return try {
            iFileExplorerService?.scanMods(scanPath, gameInfo) == true
        } catch (e: RemoteException) {
            withContext(Dispatchers.Main) {
                ToastUtils.longCall(R.string.toast_shizuku_load_file_failed)
            }
            e.printStackTrace()
            false
        }
    }

    fun unzipFile(
        zipPath: String,
        unzipPath: String,
        filename: String,
        password: String?
    ): Boolean {
        return try {
            iFileExplorerService?.unZipFile(zipPath, unzipPath, filename, password) == true
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }
}