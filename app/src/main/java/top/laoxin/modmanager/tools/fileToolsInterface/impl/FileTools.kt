package top.laoxin.modmanager.tools.fileToolsInterface.impl

import android.util.Log
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.userservice.FileExplorerService
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object FileTools : BaseFileTools {
    private const val TAG = "FileTools"
    override fun deleteFile(path: String): Boolean {
        return try {
            Files.walk(Paths.get(path))
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
            true
        } catch (e: IOException) {
            Log.e(TAG, "deleteFile: $e")
            false
        }
    }

    override fun copyFile(srcPath: String, destPath: String): Boolean {
        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)
            Files.createDirectories(destination.parent)
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            Log.e(TAG, "copyFile: $e")
            false
        }
    }

    override fun getFilesNames(path: String): MutableList<String> {
        val list: MutableList<String> = ArrayList()
        try {
            val files = File(path).listFiles()
            if (files != null) {
                for (f in files) {
                    if (f.isDirectory) continue
                    list.add(f.name)
                }
            }
        } catch (e: Exception) {
           Log.e(TAG, "getGameFiles: $e")
        }
        return list
    }

    override fun writeFile(path: String, filename: String, content: String): Boolean {
        return try {
            val file = File(path, filename)
            file.writeText(content)
            true
        } catch (e: IOException) {
            Log.e(TAG, "writeFile: $e")
            false
        }
    }

    override fun moveFile(srcPath: String, destPath: String): Boolean {
        if (srcPath == destPath) {
            return true
        }
        return try {
            if (!File(destPath).exists()) {
                File(destPath).parentFile?.mkdirs()
            }
            Files.move(Paths.get(srcPath), Paths.get(destPath),StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            Log.e(TAG, "moveFile: $e")
            false
        }
    }

    override fun isFileExist(path: String): Boolean {

        return try {
            File(path).exists()
        } catch (e: Exception) {
            Log.e(TAG, "isFileExist: $e")
            false
        }

    }

    override fun isFile(filename: String): Boolean {
        return try {
            File(filename).isFile
        } catch (e: Exception) {
            Log.e(TAG, "isFile: $e")
            false
        }
    }

}