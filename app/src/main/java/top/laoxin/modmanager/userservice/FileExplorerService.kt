package top.laoxin.modmanager.userservice

import android.os.RemoteException
import android.util.Log
import net.lingala.zip4j.ZipFile
import top.laoxin.modmanager.bean.GameInfo

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
            false
        }
    }


    override fun writeFile(srcPath: String, name: String, content: String?): Boolean {
        //return false
        return try {
            val file = File(srcPath, name)
            file.writeText(content!!)
            true
        } catch (e: IOException) {
            Log.e(TAG, "writeFile: $e")
            false
        }

    }

    override fun fileExists(path: String?): Boolean {
        return try {
            Log.d(TAG, "fileExists: $path==${ File(path!!).exists()}")
            File(path).exists()
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

    override fun scanMods(sacnPath: String?, gameInfo: GameInfo?): Boolean {
        return try {
            val files = File(sacnPath!!).listFiles()
            val gameFiles = mutableListOf<String>()
            gameInfo?.gameFilePath?.forEach {
                gameFiles.addAll(getFilesNames(it))
            }
            Log.d(TAG, "游戏中的文件: ${gameFiles}")
            if (files != null) {
                // 判断file是否为压缩文件
                for (f in files) {
                    if (f.isDirectory || isFileType(f)) continue
                    val zipFile = ZipTools.createZipFile(f)
                    Log.d(TAG, "scanMods: ${f.name}")

                    if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                        zipFile.fileHeaders.forEach {
                            val modFileName = File(ZipTools.getFileName(it)).name
                            if (gameFiles.contains(modFileName)) {
                                Log.d(TAG, "开始移动文件: ${f.name}==${modFileName}")
                                moveFile(f.path, Paths.get(gameInfo!!.modSavePath, f.name).toString())
                                return@forEach
                            }
                        }
                    }
                }
            }
            true
        }catch (e : Exception) {
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
        } catch (e: Exception) {
            false
        }
    }


    // 判断文件类型,如果是图片, 视频, 音频, apk文件则返回false
    private fun isFileType(file: File): Boolean {
        val name = file.name
        return (/*name.contains(".jpg", ignoreCase = true) ||
                name.contains(".png", ignoreCase = true) ||
                name.contains(".gif", ignoreCase = true) ||
                name.contains(".jpeg", ignoreCase = true) ||
                name.contains(".mp4", ignoreCase = true) ||
                name.contains(".mp3", ignoreCase = true) ||*/
                name.contains(".apk", ignoreCase = true))
    }

    companion object {
        private const val TAG = "FileExplorerService"
    }

}
