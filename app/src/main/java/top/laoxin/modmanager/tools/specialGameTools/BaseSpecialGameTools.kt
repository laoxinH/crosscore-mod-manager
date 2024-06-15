package top.laoxin.modmanager.tools.specialGameTools

import android.text.TextUtils
import android.util.Log
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.io.inputstream.ZipInputStream
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.tools.ZipTools
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

interface BaseSpecialGameTools {
    fun specialOperationEnable(mod: ModBean,packageName : String) : Boolean
    fun specialOperationDisable(backup: List<BackupBean>,packageName : String) : Boolean

    fun calculateMD5(inputStream: InputStream): String? {
        try {
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
        } catch (e: Exception) {
            Log.e("BaseSpecialGameTools", "calculateMD5: ${e.message}")
            return null
        }

    }

    // 读取zip文件流
    fun getZipFileInputStream(
        zipFilePath: String,
        fileName: String,
        password: String = ""
    ): InputStream? {
        kotlin.runCatching {
            val file = File(zipFilePath)
            var zipFile = ZipFile(file)
            zipFile.charset = StandardCharsets.UTF_8
            val headers = zipFile.fileHeaders
            if (ZipTools.isRandomCode(headers)) { //判断文件名是否有乱码，有乱码，将编码格式设置成GBK
                zipFile.close()
                zipFile = ZipFile(zipFilePath)
                zipFile.charset = Charset.forName("GBK")
            }
            if (!zipFile.isValidZipFile) {
                throw ZipException("压缩文件不合法,可能被损坏.")
            }
            if (zipFile.isEncrypted && password.isNotEmpty()) { //加密zip，且输入的密码不为空，直接进行解密。
                zipFile.setPassword(password.toCharArray())
            }
            val fileHeader = zipFile.getFileHeader(fileName)
            zipFile.getInputStream(fileHeader)
        }.onSuccess {
            return it
        }.onFailure {
            Log.e("BaseSpecialGameTools", "getZipFileInputStream: ${it.message}")
            return null
        }
        return null
    }

    fun getInputStreamSize(inputStream: InputStream): Long {
        val buffer = ByteArray(8192)
        var count = 0L
        var n = 0
        while (-1 != inputStream.read(buffer).also { n = it }) {
            count += n.toLong()
        }
        return count
    }
}