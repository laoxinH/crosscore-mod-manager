package top.laoxin.modmanager.tools

import java.io.InputStream
import java.security.MessageDigest

object MD5 {
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
}