package top.laoxin.modmanager.userservice.shizuku

import android.annotation.SuppressLint
import android.os.RemoteException
import android.util.Log
import top.laoxin.modmanager.constant.SpecialGame
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.service.IFileExplorerService
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class FileExplorerService : IFileExplorerService.Stub() {

    @Throws(RemoteException::class)

    override fun getFilesNames(path: String?): MutableList<String> {
        val list: MutableList<String> = ArrayList()
        try {
            val files = File(path!!).listFiles()
            if (files != null) {
                for (f in files) {
                    if (f.isDirectory || isFileType(f)) continue
                    list.add(f.name)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getGameFiles: $e")
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
        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)
            if (File(destPath!!).parentFile?.exists() == false) {
                File(destPath).parentFile?.mkdirs()
            }

            // 创建目标文件路径
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            Log.d(TAG, "copyFile错误: $e")
            LogTools.logRecord("shizuku复制文件失败--$srcPath:$e")
            false
        }
    }


    override fun writeFile(srcPath: String, name: String, content: String?): Boolean {
        //return false
        // Log.d(TAG, "writeFile: $srcPath==$name")
        return try {
            val file = File(srcPath, name)
            if (file.exists()) {
                file.delete()
                //file.createNewFile()
            }
            file.writeText(content!!)
            true
        } catch (e: IOException) {
            Log.e(TAG, "writeFile: $e")
            false
        }

    }

    override fun fileExists(path: String?): Boolean {
        return try {
            Log.d(TAG, "fileExists: $path==${File(path!!).exists()}")
            File(path).exists()
        } catch (_: Exception) {
            false
        }
    }

    // 修改文件权限
    @SuppressLint("SetWorldReadable")
    override fun chmod(path: String?): Boolean {
        return try {
            val g = File(path!!).setExecutable(true, false)
            val h = File(path).setReadable(true, false)
            val command = "chmod 777 $path"
            Log.i(TAG, "command = $command g = $g h = $h")
            val runtime = Runtime.getRuntime()
            runtime.exec(command)
            true
        } catch (e: IOException) {
            Log.i("TAG", "chmod fail!!!!")
            e.printStackTrace()
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
            val archiveItemInputStream =
                ArchiveUtil.getArchiveItemInputStream(zipPath!!, filename!!, password)
            val file = File(unzipPath!!, File(filename).name)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            archiveItemInputStream.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "unZipFile: $e")
            false
        }
    }

    override fun scanMods(sacnPath: String?, gameInfo: GameInfoBean?): Boolean {
        Log.d(TAG, "SHIZUKU开始扫描")
        return try {
            if (sacnPath == null || gameInfo == null) return false
            val files = File(sacnPath).listFiles()
            val gameFiles = mutableListOf<String>()
            gameInfo.gameFilePath.forEach {
                gameFiles.addAll(getFilesNames(it))
            }
            if (files != null) {
                // 判断file是否为压缩文件
                for (f in files) {
                    if (f.isDirectory || isFileType(f)) continue
                    if (ArchiveUtil.isArchive(f.absolutePath)) {
                        ArchiveUtil.listInArchiveFiles(f.absolutePath).forEach {
                            val modFileName = File(it).name
                            if (gameFiles.contains(modFileName) || specialOperationScanMods(
                                    gameInfo.packageName,
                                    modFileName
                                )
                            ) {
                                Log.d(TAG, "开始移动文件(SHIZUKU): ${f.name}==${modFileName}")
                                moveFile(
                                    f.path,
                                    Paths.get(gameInfo.modSavePath, f.name).toString()
                                )
                                return@forEach
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            LogTools.logRecord("shuzuku扫描mod失败:$e")
            Log.e(TAG, "scanMods: $e")
            false
        }
    }


    override fun moveFile(srcPath: String?, destPath: String?): Boolean {
        // 通过srcPath和destPath路径复制文件
        if (srcPath == destPath) return true
        return try {
            val source = Paths.get(srcPath)
            val destination = Paths.get(destPath)
            // 创建目标文件路径
            if (File(destPath!!).parentFile?.exists() == false) {
                File(destPath).parentFile?.mkdirs()
            }
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            Log.e(TAG, "moveFile失败: $e")
            false
        }
    }

    override fun isFile(path: String?): Boolean {
        return try {
            val file = File(path!!)
            file.isFile
        } catch (_: Exception) {
            false
        }
    }

    override fun isFileChanged(path: String): Long {
        return try {
            val lastModified = File(path).lastModified()
            Log.d(TAG, "isFileChanged: $lastModified")
            lastModified
        } catch (_: Exception) {
            0
        }
    }

    override fun changDictionaryName(path: String?, newName: String?): Boolean {
        return try {
            //Log.d(TAG, "changDictionaryName: $path==$newName")
            val file = File(path!!)
            val newFile = File(file.parent, newName!!)
            file.renameTo(newFile)
        } catch (e: Exception) {
            Log.e(TAG, "changDictionaryName: $e")
            false
        }
    }

    override fun createDictionary(path: String?): Boolean {
        try {
            if (path == null) return false
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "createDictionary: $e")
            return false
        }
    }


    private fun isFileType(file: File): Boolean {
        val name = file.name.lowercase()
        // 判断是否是图片、视频、音频或APK文件
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "flv", "wmv")
        val audioExtensions = listOf("mp3", "wav", "aac", "flac", "ogg")
        val apkExtension = "apk"

        // 检查文件扩展名
        return imageExtensions.any { name.endsWith(it) } ||
                videoExtensions.any { name.endsWith(it) } ||
                audioExtensions.any { name.endsWith(it) } ||
                name.endsWith(apkExtension)
    }

    fun specialOperationScanMods(packageName: String, modFileName: String): Boolean {
        var l = SpecialGame.specialGameList
        if (packageName.contains(l[1]) || packageName.contains(l[0])) {
            return false
        } else if (packageName.contains(l[2]) || packageName.contains(l[3])) {
            return modFileName.endsWith(".pak") == true
        }
        return false

        // 上述结果需要确保与下列一致
        // return specialGameToolsManager.getSpecialGameTools(packageName)?.specialOperationScanMods(
        //     packageName,
        //     modFileName
        // ) == true
    }

    companion object {
        private const val TAG = "FileExplorerService"
    }

}
