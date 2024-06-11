package top.laoxin.modmanager.tools

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object ZipTools {
    private const val TAG = "ZipTools"
    private val fileTools = FileTools


    // 通过file生成zip压缩对象,需要判断是否为zip文件
    fun createZipFile(file: File): ZipFile? {
        val zipFile = ZipFile(file)
        if (zipFile.isValidZipFile) {
            return zipFile
        }
        return zipFile
        //return null
    }

    // 通过zip对象获取文件列表判断存在readme.txt文件
    fun hasReadmeFile(zipFile: ZipFile): Boolean {
        val fileHeaders = zipFile.fileHeaders
        for (fileHeaderObj in fileHeaders) {
            val fileHeader = fileHeaderObj as FileHeader
            if (fileHeader.fileName.equals("readme.txt", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    // 读取readme.txt文件中的字段,并生成mod对象
    fun readModBean(
        zipFile: ZipFile,
        modBeanTemp: ModBeanTemp,
        appPath: String
    ): ModBean? {
        val fileHeaders = zipFile.fileHeaders
        var modBean: ModBean? = null

        for (fileHeaderObj in fileHeaders) {
            if (getFileName(fileHeaderObj).equals(modBeanTemp.readmePath, ignoreCase = true)
                || getFileName(fileHeaderObj).equals(modBeanTemp.fileReadmePath, ignoreCase = true)
            ) {
                val inputStream = zipFile.getInputStream(fileHeaderObj)
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val content = reader.readText()
                reader.close()

                val lines = content.split("\n")
                val infoMap = mutableMapOf<String, String>()
                for (line in lines) {
                    val parts = line.split("：")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        infoMap[key] = value
                    }
                }
                Log.d("FileExplorerService", "mod信息: $infoMap")

                val name = infoMap["名称"]
                val description = infoMap["描述"]
                val version = infoMap["版本"]
                val author = infoMap["作者"]

                val icon = getFirstImageFromZip(zipFile, appPath, modBeanTemp.iconPath)
                val images = getImagesFromZip(zipFile, appPath, modBeanTemp.images)


                modBean = ModBean(
                    id = 0,
                    name = name ?: modBeanTemp.name,
                    version = version ?: "1.0",
                    description = description ?: "由Mod管理器生成",
                    author = author ?: "未知",
                    date = zipFile.file.lastModified(),
                    path = zipFile.file.path,
                    icon = icon,
                    images = images,
                    modFiles = modBeanTemp.modFiles,
                    isEncrypted = isEncrypted(zipFile),
                    readmePath = modBeanTemp.readmePath,
                    fileReadmePath = modBeanTemp.fileReadmePath,
                    password = null,
                    isEnable = false,
                    gamePackageName = modBeanTemp.gamePackageName,
                    gameModPath = modBeanTemp.gameModPath,
                    modType = modBeanTemp.modType,

                )


            }
        }
        return modBean
    }


    // 从zip对象中读取第一个图片文件并写入appPath同时返回图片路径

    fun getFirstImageFromZip(
        zipFile: ZipFile,
        appPath: String,
        iconPath: String?
    ): String? {
       // Log.d("ZipTools", "图标路径: $iconPath")
       // Log.d("ZipTools", "zip文件头: ${zipFile.fileHeaders.map { getFileName(it) }}")
        val fileHeaders = zipFile.fileHeaders
        for (fileHeaderObj in fileHeaders) {
           // Log.d("ZipTools", "对比: ${getFileName(fileHeaderObj)} ---- $iconPath")
           // Log.d("ZipTools", "对比结果: ${getFileName(fileHeaderObj).equals(iconPath, ignoreCase = true)}")
            if (getFileName(fileHeaderObj).equals(iconPath, ignoreCase = true)) {

                val inputStream = zipFile.getInputStream(fileHeaderObj)
                val calculateMD5 = calculateMD5(inputStream)
                Log.d("ZipTools", "文件路径: $appPath")
                val name = calculateMD5 + fileHeaderObj.fileName
                return createFile(appPath, name, zipFile.getInputStream(fileHeaderObj))
            }
        }
        return null
    }

    // 从zip对象中读取所有图片文件并写入appPath路径同时返回图片路径列表
    fun getImagesFromZip(
        zipFile: ZipFile,
        appPath: String,
        images: MutableList<String>
    ): List<String> {
        val fileHeaders = zipFile.fileHeaders
        val imagesList = mutableListOf<String>()
        for (fileHeaderObj in fileHeaders) {
            if (getFileName(fileHeaderObj) in images) {
                val inputStream = zipFile.getInputStream(fileHeaderObj)
                val calculateMD5 = calculateMD5(inputStream)
                Log.d("ZipTools", "文件路径: $appPath")
                val name = calculateMD5 + fileHeaderObj.fileName
                imagesList.add(createFile(appPath, name, zipFile.getInputStream(fileHeaderObj)))
            }
        }
        return imagesList
    }

    // 通过路径和流对象创建文件
    fun createFile(path: String, name: String, inputStream: InputStream): String {
        Log.d("ZipTools", "文件路径: $path + $name")
        val file = File(path, name)
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
                    input.copyTo(outputStream)
                }
            }
        }
        return file.absolutePath
    }

    // 解压zip文件
    fun unZip(modTempPath: String, s: String, password: String?): String? {
        Log.d("ZipTools", "文件路径: $modTempPath")
        Log.d("ZipTools", "解压路径: $s")
        val zipFile = ZipFile(modTempPath)

        if (password != null) {
            zipFile.setPassword(password.toCharArray())
        }
        val upZipPath = s + File(modTempPath).name + "/"
        if (File(upZipPath).exists()) {
            return upZipPath
        }
        // 创建目标文件路径

        //Files.createDirectories(Paths.get(upZipPath).parent)

        // 解压
        return try {
            //zipFile.charset = Charset.forName(checkZipFileCharset(zipFile))
            //zipFile.extractAll()
            zipFile.extractAll(upZipPath)
            upZipPath
        } catch (e: Exception) {
            fileTools.deleteFile(upZipPath)
            Log.e("ZipTools", "解压失败: $e")
            e.printStackTrace()
            null
        }
    }


    fun calculateMD5(inputStream: InputStream): String {
        val buffer = ByteArray(8192)
        val md5 = MessageDigest.getInstance("MD5")

        var numRead: Int
        while (inputStream.read(buffer).also { numRead = it } > 0) {
            md5.update(buffer, 0, numRead)
        }

        val md5Bytes = md5.digest()
        val result = StringBuilder(md5Bytes.size * 2)

        md5Bytes.forEach {
            val i = it.toInt()
            result.append(Character.forDigit((i shr 4) and 0xf, 16))
            result.append(Character.forDigit(i and 0xf, 16))
        }

        return result.toString()
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

    fun getFileName(fileHeader: FileHeader): String {
        return try {
            if (fileHeader.isFileNameUTF8Encoded) {
                fileHeader.fileName
            } else {
                String(
                    fileHeader.fileName.toByteArray(Charset.forName("Cp437")),
                    Charset.forName("GBK")
                )
            }
        } catch (e: Exception) {
            fileHeader.fileName
        }
    }


    // 判断zip文件是否加密
    fun isEncrypted(zipFile: ZipFile): Boolean {
        return zipFile.isEncrypted
    }


    fun isZipFile(zipFile: ZipFile): Boolean {
        return zipFile.isValidZipFile
    }

    fun unZipByFileHeard(
        modTempPath: String,
        gameModPath: String,
        files: String?,
        password: String?
    ): Boolean {
        val zipFile = ZipFile(modTempPath)
        if (password != null) {
            zipFile.setPassword(password.toCharArray())
        }
        val fileHeaders = zipFile.fileHeaders
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (getFileName(fileHeaderObj) == files) {
                try {
                    createFile(
                        gameModPath,
                        File(fileHeaderObj.fileName).name,
                        zipFile.getInputStream(fileHeaderObj)
                    )
                    flag.add(true)
                } catch (e: Exception) {
                    Log.e("ZipTools", "通过流解压失败: $e")
                    e.printStackTrace()
                    flag.add(false)
                }
            }
        }
        return flag.all { it }
    }

}

// 读取加密mod信息




