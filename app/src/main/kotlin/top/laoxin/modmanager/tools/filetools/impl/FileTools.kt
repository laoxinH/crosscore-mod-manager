package top.laoxin.modmanager.tools.filetools.impl

import android.util.Log
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTools @Inject constructor(
    appPathsManager: AppPathsManager
) : BaseFileTools(appPathsManager) {
    companion object {
        const val TAG = "FileTools"
    }
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
            Files.move(Paths.get(srcPath), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING)
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

    override fun createFileByStream(
        path: String,
        filename: String,
        inputStream: InputStream?
    ): Boolean {
        Log.d("ZipTools", "文件路径: $path + $filename")
        val file = File(path, filename)
        try {
            if (!file.exists()) {
                if (file.parentFile?.exists() != true) {
                    file.parentFile?.mkdirs()
                }
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
                val fileOutputStream = FileOutputStream(file)
                inputStream.use { input ->
                    fileOutputStream.use { outputStream ->
                        input?.copyTo(outputStream)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "createFileByStream: $e")
            return false
        }
    }

    override fun isFileChanged(path: String): Long {
        val file = File(path)
        return file.lastModified()

    }

    override fun changDictionaryName(path: String, name: String): Boolean {
        val file = File(path)
        if (file.exists()) {
            file.renameTo(File(file.parent, name))
            return true
        } else {
            return false
        }
    }

    override fun createDictionary(path: String): Boolean {
        val file = File(path)
        try {
            if (!file.exists()) {
                file.mkdirs()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "createDictionary: $e")
            return false
        }
    }

    override fun readFile(path: String): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            Log.e(TAG, "readFile: $e")
            ""
        }
    }

    override fun listFiles(path: String): MutableList<File> {
        val list: MutableList<File> = ArrayList()
        try {
            val files = File(path).listFiles()
            return files?.toMutableList() ?: list
        } catch (e: Exception) {
            Log.e(TAG, "listFiles: $e")
        }
        return list
    }
}