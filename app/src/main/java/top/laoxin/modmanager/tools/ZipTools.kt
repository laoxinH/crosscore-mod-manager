package top.laoxin.modmanager.tools

import android.text.TextUtils
import android.util.Log
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
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
        try {
            val zFilePath = zipFile.file.absoluteFile
            var zFile = ZipFile(zFilePath)
            zFile.charset = StandardCharsets.UTF_8
            val headers = zFile.fileHeaders
            if (isRandomCode(headers)) { //判断文件名是否有乱码，有乱码，将编码格式设置成GBK
                zFile.close()
                zFile = ZipFile(zFilePath)
                zFile.charset = Charset.forName("GBK")
            }
            val fileHeaders = zFile.fileHeaders
            for (fileHeaderObj in fileHeaders) {

                if (fileHeaderObj.fileName.equals(iconPath, ignoreCase = true)) {

                    val inputStream = zFile.getInputStream(fileHeaderObj)
                    val calculateMD5 = calculateMD5(inputStream)
                    Log.d("ZipTools", "文件路径: $appPath")
                    val name = calculateMD5 + fileHeaderObj.fileName
                    return createFile(appPath, name, zFile.getInputStream(fileHeaderObj))
                }
            }

        } catch (e: Exception) {
            Log.e("ZipTools", "获取图标失败: $e")
            return null
        }
        return null
    }

    // 从zip对象中读取所有图片文件并写入appPath路径同时返回图片路径列表
    fun getImagesFromZip(
        zipFile: ZipFile,
        appPath: String,
        images: MutableList<String>
    ): List<String> {
        try {
            val zFilePath = zipFile.file.absoluteFile
            var zFile = ZipFile(zFilePath)
            zFile.charset = StandardCharsets.UTF_8
            val headers = zFile.fileHeaders
            if (isRandomCode(headers)) { //判断文件名是否有乱码，有乱码，将编码格式设置成GBK
                zFile.close()
                zFile = ZipFile(zFilePath)
                zFile.charset = Charset.forName("GBK")
            }
            val fileHeaders = zFile.fileHeaders
            val imagesList = mutableListOf<String>()
            for (fileHeaderObj in fileHeaders) {
                if (fileHeaderObj.fileName in images) {
                    val inputStream = zFile.getInputStream(fileHeaderObj)
                    val calculateMD5 = calculateMD5(inputStream)
                    Log.d("ZipTools", "文件路径: $appPath")
                    val name = calculateMD5 + fileHeaderObj.fileName
                    imagesList.add(createFile(appPath, name, zFile.getInputStream(fileHeaderObj)))
                }
            }
            return imagesList
        } catch (e: Exception) {
            Log.e("ZipTools", "获取图片失败: $e")
            return emptyList()
        }

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

    fun unZip(srcFilePath: String, destFilePath: String) {
        unZip(srcFilePath, destFilePath, "")
    }

    fun unZip(srcFilePath: String, destFilePath: String, password: String): String? {
        try {
            val unzipPath = destFilePath + File(srcFilePath).nameWithoutExtension + "/"
            if (File(unzipPath).exists()) {
                return unzipPath
            }
            var zFile = ZipFile(srcFilePath)
            zFile.charset = StandardCharsets.UTF_8
            val headers = zFile.fileHeaders
            if (isRandomCode(headers)) { //判断文件名是否有乱码，有乱码，将编码格式设置成GBK
                zFile.close()
                zFile = ZipFile(srcFilePath)
                zFile.charset = Charset.forName("GBK")
            }

            if (!zFile.isValidZipFile) {
                throw ZipException("压缩文件不合法,可能被损坏.")
            }
            if (zFile.isEncrypted && !TextUtils.isEmpty(password)) { //加密zip，且输入的密码不为空，直接进行解密。
                zFile.setPassword(password.toCharArray())
            }
            val destDir = File(unzipPath)
            if (destDir.parentFile?.exists() != true) {
                destDir.mkdir()
            }
            zFile.extractAll(unzipPath)
            return unzipPath
        } catch (exception: Exception) {
            exception.printStackTrace()
            return null
        }
    }

    //待解压的文件名是否乱码
    fun isRandomCode(fileHeaders: List<FileHeader>): Boolean {
        for (i in fileHeaders.indices) {
            val fileHeader = fileHeaders[i]
            val canEnCode = Charset.forName("GBK").newEncoder().canEncode(fileHeader.fileName)
            if (!canEnCode) { //canEnCode为true，表示不是乱码。false.表示乱码。是乱码则需要重新设置编码格式
                return true
            }
        }
        return false
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
        try {
            var zFile = ZipFile(modTempPath)
            zFile.charset = StandardCharsets.UTF_8
            val headers = zFile.fileHeaders
            if (isRandomCode(headers)) { //判断文件名是否有乱码，有乱码，将编码格式设置成GBK
                zFile.close()
                zFile = ZipFile(modTempPath)
                zFile.charset = Charset.forName("GBK")
            }
            if (!password.isNullOrEmpty()) {
                zFile.setPassword(password.toCharArray())
            }
            val fileHeaders = zFile.fileHeaders
            val flag: MutableList<Boolean> = mutableListOf()
            for (fileHeaderObj in fileHeaders) {
                if (fileHeaderObj.fileName == files) {
                    try {
                        File(gameModPath,File(fileHeaderObj.fileName).name).delete()
                        createFile(
                            gameModPath,
                            File(fileHeaderObj.fileName).name,
                            zFile.getInputStream(fileHeaderObj)
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
        } catch (e: Exception) {
            Log.e("ZipTools", "流解压失败: $e")
            e.printStackTrace()
            return false
        }

    }

}

// 读取加密mod信息




