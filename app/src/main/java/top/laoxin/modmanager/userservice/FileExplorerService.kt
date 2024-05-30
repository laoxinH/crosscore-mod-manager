package top.laoxin.modmanager.userservice

import android.os.RemoteException
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.lingala.zip4j.ZipFile
import top.laoxin.modmanager.App

import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.tools.ZipTools
import top.laoxin.modmanager.useservice.IFileExplorerService

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


class FileExplorerService : IFileExplorerService.Stub() {
    @Throws(RemoteException::class)
    override fun listFiles(
        path: String?,
        appPath: String,
        gameModPath: String,
        downloadModPath : String
    ): MutableList<ModBean> {
        val list: MutableList<ModBean> = ArrayList<ModBean>()
        val files = path?.let { File(it).listFiles() }
        if (files != null) {
            // 判断file是否为压缩文件
            for (f in files) {
                if (f.isDirectory) continue
                val zipFile = ZipTools.createZipFile(f)
                if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                    // 读取zip文件中的mod文件
                    val modTempMap = createModTempMap(zipFile, gameModPath)
                    val mods : List<ModBean> = ZipTools.readModBeans(zipFile,modTempMap,appPath,downloadModPath)
                    list.addAll(mods)
                }
            }
        }
        return list
    }

    override fun deleteFile(path: String?): Boolean {
        // 通过path路径删除文件
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

    override fun copyFile(srcPath: String?, destPath: String?): Boolean {
        // 通过srcPath和destPath路径复制文件
        Log.d(TAG, "copyFile: $srcPath--- $destPath")

        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)
            // 创建目标文件路径
            Files.createDirectories(destination.parent)
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            Log.d(TAG, "copyFile错误: $e")
            false
        }
    }


    override fun writeFile(srcPath: String, name: String, content: String?): Boolean {
        //return false
        Log.d(TAG, "writeFile: $srcPath")
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
            Log.e(TAG, "writeFile: $e")
            false
        }

    }

    override fun fileExists(path: String?): Boolean {
        return try {
            Log.d(TAG, "fileExists: $path==${ File(path!!).exists()}")

            File(path!!).exists()
        } catch (e: Exception) {
            false
        }
    }

    // 修改文件权限
    override fun chmod(path: String?): Boolean {
        return try {
            val g = File(path).setExecutable(true, false)
            val h = File(path).setReadable(true, false)
            val command = "chmod 777 " + path
            Log.i(TAG, "command = $command g = $g h = $h")
            val runtime = Runtime.getRuntime()
            val proc = runtime.exec(command)
            true
        } catch (e : IOException) {
            Log.i("TAG","chmod fail!!!!");
            e.printStackTrace();
            false
        }
    }

    override fun unZipFile(
        zipPath: String?,
        unzipPath: String?,
        filename: String?,
        password: String?
    ): Boolean {
        return try {
            ZipTools.unZipByFileHeard(zipPath!!, unzipPath!!, filename, password)
        } catch (e: Exception) {
            Log.e(TAG, "unZipFile: $e")
            false
        }
    }

    override fun scanMods(
        path: String?,
        appPath: String?,
        gameModPath: String?,
        downloadModPath: String?
    ): Boolean {
        return try {
            val files = File(path!!).listFiles()
            if (files != null) {
                // 判断file是否为压缩文件
                for (f in files) {
                    if (f.isDirectory) continue
                    val zipFile = ZipTools.createZipFile(f)
                    if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                        // 读取zip文件中的mod文件
                        val modTempMap = createModTempMap(zipFile, gameModPath!!)
                        if (modTempMap.isNotEmpty()) moveFile(zipFile.file.path, downloadModPath + zipFile.file.name)
                    }
                }
            }
            true
        }catch (e : Exception) {
            false
        }
    }

    fun moveFile(srcPath: String?, destPath: String?): String? {
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

    fun createModTempMap(
        zipFile: ZipFile,
        gameModPath: String
    ): MutableMap<String, ModBeanTemp> {
        // 创建一个ModBeanTemp的map
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()
        val fileHeaders = zipFile.fileHeaders
        for (fileHeaderObj in fileHeaders) {
            val modName = fileHeaderObj.fileName.substringAfterLast("/")
            val gameFile = File(gameModPath,modName)
            if (gameFile.exists() && gameFile.isFile) {
                val modEntries = File(ZipTools.getFileName(fileHeaderObj))
                val key = modEntries.parent ?: zipFile.file.name
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp == null) {
                    val beanTemp = ModBeanTemp(
                        name = zipFile.file.nameWithoutExtension + if (modEntries.parentFile == null) "" else "(" + modEntries.parentFile!!.path.replace(
                            "/",
                            "|"
                        ) + ")", // 如果是根目录则不加括号
                        iconPath = null,
                        readmePath = null,
                        modFiles = mutableListOf(fileHeaderObj.fileName),
                        images = mutableListOf(),
                        fileReadmePath = null,
                        isEncrypted = ZipTools.isEncrypted(zipFile)
                    )
                    modBeanTempMap[key] = beanTemp
                } else {
                    modBeanTemp.modFiles.add(fileHeaderObj.fileName)
                }
            }
        }
        Log.d("ZipTools", "modBeanTempMap: $modBeanTempMap")
        // 判断是否存在readme.txt文件
        for (fileHeaderObj in fileHeaders) {
            val fileName = ZipTools.getFileName(fileHeaderObj)
            if (fileName.substringAfterLast("/").equals("readme.txt", ignoreCase = true)) {
                Log.d("ZipTools", "readme文件路径: $fileName")
                val key = File(fileName).parent ?: zipFile.file.name
                Log.d("ZipTools", "readme文件key: $key")
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp != null) {
                    modBeanTemp.readmePath = fileName
                }
            } else if (fileName.contains(".jpg", ignoreCase = true)
                || fileName.contains(".png", ignoreCase = true)
                || fileName.contains(".gif", ignoreCase = true)
                || fileName.contains(".jpeg", ignoreCase = true)
            ) {
                val key = File(fileName).parent ?: zipFile.file.name
                val modBeanTemp = modBeanTempMap[key]
                modBeanTemp?.images?.add(fileName)
                modBeanTemp?.iconPath = fileName
            } else if (fileName.equals("readme.txt", ignoreCase = true)) {
                modBeanTempMap.forEach {
                    val modBeanTemp = it.value
                    modBeanTemp.fileReadmePath = fileName
                }
            }
        }
        return modBeanTempMap
    }



    companion object {
        private const val TAG = "FileExplorerService"
    }


}
