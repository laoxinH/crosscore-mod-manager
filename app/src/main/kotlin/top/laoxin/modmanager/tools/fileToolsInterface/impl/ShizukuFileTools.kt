package top.laoxin.modmanager.tools.fileToolsInterface.impl

import android.util.Log
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.useservice.IFileExplorerService
import java.io.InputStream

object ShizukuFileTools : BaseFileTools {
    private const val TAG = "ShizukuFileTools"
    var iFileExplorerService: IFileExplorerService? = null
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
}