package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.listener.ProgressUpdateListener
import top.laoxin.modmanager.tools.ArchiveUtil
import java.io.InputStream
import java.security.MessageDigest

interface BaseSpecialGameTools {

    companion object {
        var progressUpdateListener: ProgressUpdateListener? = null
    }


    fun specialOperationEnable(mod: ModBean, packageName: String): Boolean
    fun specialOperationDisable(
        backup: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ): Boolean

    fun specialOperationStartGame(gameInfo: GameInfoBean): Boolean
    fun specialOperationCreateMods(gameInfo: GameInfoBean): List<ModBeanTemp>
    fun specialOperationScanMods(gameInfo: String, modFileName: String): Boolean
    fun specialOperationSelectGame(gameInfo: GameInfoBean): Boolean
    fun specialOperationNeedOpenVpn(): Boolean

    fun onProgressUpdate(progress: String) {
        progressUpdateListener?.onProgressUpdate(progress)
    }


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
        password: String?
    ): InputStream? {
        kotlin.runCatching {
            ArchiveUtil.getArchiveItemInputStream(
                zipFilePath,
                fileName,
                password
            )
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