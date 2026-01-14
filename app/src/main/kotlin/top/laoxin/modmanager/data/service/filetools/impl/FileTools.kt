package top.laoxin.modmanager.data.service.filetools.impl

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.data.service.filetools.BaseFileTools
import kotlin.io.use

/** 标准文件系统操作工具 不捕获异常，让异常传播到 FileServiceImpl 统一处理 */
@Singleton
class FileTools @Inject constructor() :
        BaseFileTools() {

    companion object {
        const val TAG = "FileTools"
    }

    override fun deleteFile(path: String): Boolean {
        Files.walk(Paths.get(path)).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach(Files::delete)
        }
        return true
    }

    override fun copyFile(srcPath: String, destPath: String): Boolean {
        val source = Paths.get(srcPath)
        val destination = Paths.get(destPath)
        Files.createDirectories(destination.parent)
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    override fun getFilesNames(path: String): MutableList<String> {
        val list: MutableList<String> = ArrayList()
        val files = File(path).listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) continue
                list.add(f.name)
            }
        }
        return list
    }

    override fun writeFile(path: String, filename: String, content: String): Boolean {
        val file = File(path, filename)
        file.writeText(content)
        return true
    }

    override fun moveFile(srcPath: String, destPath: String): Boolean {
        if (srcPath == destPath) {
            return true
        }
        if (!File(destPath).exists()) {
            File(destPath).parentFile?.mkdirs()
        }
        Files.move(Paths.get(srcPath), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    override fun isFileExist(path: String): Boolean {
        return File(path).exists()
    }

    override fun isFile(filename: String): Boolean {
        return File(filename).isFile
    }

    override fun createFileByStream(
            path: String,
            filename: String,
            inputStream: InputStream
    ): Boolean {
        val file = File(path, filename)
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
                fileOutputStream.use { outputStream -> input.copyTo(outputStream) }
            }
        }
        return true
    }

    override fun isFileChanged(path: String): Long {
        val file = File(path)
        return file.lastModified()
    }

    override fun changDictionaryName(path: String, name: String): Boolean {
        val file = File(path)
        if (file.exists()) {
            return file.renameTo(File(file.parent, name))
        }
        return false
    }

    override fun createDictionary(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            return file.mkdirs()
        }
        return true
    }

    override fun readFile(path: String): String {
        return File(path).readText()
    }

    override fun listFiles(path: String): MutableList<File> {
        val files = File(path).listFiles()
        return files?.toMutableList() ?: mutableListOf()
    }

    override fun calculateFileMd5(path: String): String {
        val file = File(path)
        val digest = java.security.MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return  digest.digest().joinToString("") { "%02x".format(it) }
    }

    override fun getFileSize(path: String): Long {
       return File(path).length()
    }
}
