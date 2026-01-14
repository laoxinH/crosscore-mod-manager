package top.laoxin.modmanager.data.service.specialgame

import java.io.InputStream
import java.security.MessageDigest
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.GameStartCheckResult
import top.laoxin.modmanager.listener.ProgressUpdateListener
import top.laoxin.modmanager.tools.ArchiveUtil

/** 特殊游戏处理器基类 定义特殊游戏操作的内部接口 */
interface BaseSpecialGameHandler {

    companion object {
        var progressUpdateListener: ProgressUpdateListener? = null
    }

    /** 启用 Mod 的特殊操作 */
    suspend fun handleModEnable(mod: ModBean, packageName: String): Result<Unit>

    /** 禁用 Mod 的特殊操作 */
    suspend fun handleModDisable(
            backup: List<BackupBean>,
            packageName: String,
            mod: ModBean
    ): Result<Unit>

    /** 启动游戏的特殊操作 */
    suspend fun handleGameStart(gameInfo: GameInfoBean): Result<Unit>

    /** 启动游戏前的检查 */
    suspend fun checkBeforeGameStart(gameInfo: GameInfoBean): Result<GameStartCheckResult>

    /** 扫描 Mod 的特殊判断 */
    fun isModFileSupported(modFileName: String): Boolean

    /** 选择游戏的特殊操作 */
    suspend fun handleGameSelect(gameInfo: GameInfoBean): Result<GameInfoBean>

    /** 更新游戏信息 */
    fun updateGameInfo(gameInfo: GameInfoBean): GameInfoBean

    /** 是否需要后台服务 */
    fun needGameService(): Boolean

    /** 是否需要 VPN */
    fun needOpenVpn(): Boolean

    // ==================== 工具方法 ====================

    fun onProgressUpdate(progress: String) {
        progressUpdateListener?.onProgressUpdate(progress)
    }

    fun calculateMD5(inputStream: InputStream): String? {
        return try {
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
            result.toString()
        } catch (e: Exception) {
            null
        }
    }

    fun getZipFileInputStream(
            zipFilePath: String,
            fileName: String,
            password: String?
    ): InputStream? {
        return runCatching {
            ArchiveUtil.getArchiveItemInputStream(zipFilePath, fileName, password)
        }
                .getOrNull()
    }

    fun getInputStreamSize(inputStream: InputStream): Long {
        val buffer = ByteArray(8192)
        var count = 0L
        var n: Int
        while (inputStream.read(buffer).also { n = it } != -1) {
            count += n.toLong()
        }
        return count
    }
}
