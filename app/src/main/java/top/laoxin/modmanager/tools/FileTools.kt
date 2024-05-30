package top.laoxin.modmanager.tools

import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import top.laoxin.modmanager.App
import top.laoxin.modmanager.bean.BeanFile
import top.laoxin.modmanager.userservice.FileExplorerService
import top.laoxin.modmanager.useservice.IFileExplorerService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object FileTools {
    lateinit var iFileExplorerService: IFileExplorerService
    val app = App.get()
    // 通过file删除文件
    fun deleteFileByFile(path: String): Boolean {
        return try {
            Files.walk(Paths.get(path))
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // 通过shizuku删除文件
    fun deleteFileByShizuku(path: String): Boolean {
        return try {
            iFileExplorerService.deleteFile(path)
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }
    // 通过documentFile删除文件
    fun deleteFileByDocument(path: String): Boolean {
        val pathUri = pathToUri(path)
        Log.d("FileTools", "生成的uri: $pathUri")
        Log.d("FileTools", "生成的path: ${pathUri.path}")
        val app = App.get()
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        return try {
            documentFile?.delete()
            true
        } catch (e: Exception) {
            Log.e("FileTools", "deleteFile: $e")
            false
        }
    }

    // 通过shuzuku复制文件
     fun copyFileByShizuku(srcPath: String?, destPath: String?): Boolean {
        try {
            Log.d("FileTools", "复制文件: $srcPath---$destPath")
            val b =  iFileExplorerService.copyFile(srcPath, destPath)
            Log.d("FileTools", "复制文件结果: $b")
            return b
        } catch (e: RemoteException) {
            Log.e("FileTools", "RemoteException异常: ", e)
            e.printStackTrace()
        }
        return false
    }

    // 通过file复制文件
    fun copyFileByFile(srcPath: String, destPath: String): Boolean {
        // 通过srcPath和destPath路径复制文件
        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)
            // 创建目标文件路径
            Files.createDirectories(destination.parent)
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            false
        }
    }

    // 通过documentFile复制文件
    fun copyFileByDocument(srcPath: String, destPath: String): Boolean {
        Log.d("FileTools", "复制文件: $srcPath---$destPath")
        return try {
            val srcPathUri = pathToUri(srcPath)
            val destPathUri = pathToUri(destPath)

            val srcDocumentFile = DocumentFile.fromTreeUri(app, srcPathUri)
            val destDocumentFile = DocumentFile.fromTreeUri(app, destPathUri)
            try {
                val file = File(destPath)
                if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
                if (!file.exists()) file.createNewFile()
            } catch (e : Exception){
                Log.e("FileTools", "copyFile: ${e}")
            }

            val openInputStream = app.contentResolver.openInputStream(srcDocumentFile?.uri!!)
            val openOutputStream = app.contentResolver.openOutputStream(destDocumentFile?.uri!!)
            openInputStream?.copyTo(openOutputStream?: return false)
            openOutputStream?.flush()
            openInputStream?.close()
            openOutputStream?.close()
            true
        } catch (e: Exception) {
            Log.e("FileTools", "copyFile: ${e}")
            false
        }
    }

    // 通过shizuku写入文件(路径文件均不存在时会创建)
    fun writeFileByShizuku(path: String, filename: String, content: String?): Boolean? {
        return ModTools.iFileExplorerService?.writeFile(path, filename, content)
    }

    // 通过file写入文件
    fun writeFileByFile(srcPath: String, name: String, content: String?): Boolean {
        //return false
        Log.d("FileTools", "writeFile: $srcPath")
        return try {
            val file = File(srcPath, name)
            if (!file.exists()) {
                file.createNewFile()
            }
            file.parentFile?.mkdirs()
            val fileOutputStream = FileOutputStream(file)
            if (content != null) {
                fileOutputStream.write(content.toByteArray())
            }
            fileOutputStream.close()
            true

        } catch (e: Exception) {
            Log.e("FileTools", "writeFile: $e")
            false
        }

    }

    // 通过documentFile写入文件
    fun writeFileByDocument(path: String, filename: String, content: String?): Boolean {
        val pathUri = pathToUri(path)
        Log.d("FileTools", "生成的uri: $pathUri")
        Log.d("FileTools", "生成的path: ${pathUri.path}")

        val documentFile = DocumentFile.fromTreeUri(app, pathUri)
        return try {

            var writeFile = documentFile?.findFile(filename)
            writeFile?.delete()
            writeFile = documentFile?.createFile("text/plain", filename)

            val fileOutputStream = app.contentResolver.openOutputStream(writeFile?.uri!!)
            if (content != null) {
                fileOutputStream?.write(content.toByteArray())
            }
            fileOutputStream?.close()
            true
        } catch (e: Exception) {
            Log.e("FileTools", "writeFile: $e")
            false
        }
    }

    // 通过File读取文件列表
    fun getFileListByFile(path: String): List<BeanFile> {
        val isPkgNamePath = ModTools.isDataPath(path) || ModTools.isObbPath(path)
        val list: MutableList<BeanFile> = ArrayList<BeanFile>()
        val dir = File(path)
        var files: Array<File>
        if (dir.listFiles().also { files = it } != null) {
            for (file: File in files) {
                list.add(
                    BeanFile(
                        file.getName(),
                        file.path,
                        file.isDirectory(),
                        false,
                        if (isPkgNamePath) file.getName() else null
                    )
                )
            }
        }
        Log.d("FileTools", "文件列表: $list")
        return list
    }

    fun getFileListByDocument(path: String): List<BeanFile> {
        val pathUri = pathToUri(path)
        Log.d("FileTools", "生成的uri: $pathUri")
        Log.d("FileTools", "生成的path: ${pathUri.path}")
        val app = App.get()
        val documentFile = DocumentFile.fromTreeUri(app, pathUri)

        val list: MutableList<BeanFile> = ArrayList<BeanFile>()
        if (documentFile != null) {
            val documentFiles = documentFile.listFiles()
            for (df: DocumentFile in documentFiles) {
                val fName = df.name
                val fPath = "$path/$fName"
                list.add(BeanFile(fName, fPath, df.isDirectory, false,
                    ModTools.getPathPackageName(fName)
                ))
            }
        }
        Log.d("FileTools", "文件列表: $list")
        return list

    }


    fun moveFileByFile(srcPath: String, destPath: String): String? {
        Log.d("ZipTools", "moveFile: $srcPath--- $destPath")
        // 通过srcPath和destPath路径复制文件
        if (srcPath == destPath) return destPath
        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)
            // 创建目标文件路径
            Files.createDirectories(destination.parent)
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            destPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun pathToUri(path: String): Uri {
        Log.d("FileTools", "传入的paht: $path")
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
        Log.d("FileTools", "生成的uri: ${uriBuilder.build()}")
        return uriBuilder.build()
    }


    fun isFromMyPackageNamePath(path: String): Boolean {
        return ("$path/").contains(
            (ModTools.ROOT_PATH + "/Android/data/" + (App.get().packageName ?: "")).toString() + "/"
        )
    }
}