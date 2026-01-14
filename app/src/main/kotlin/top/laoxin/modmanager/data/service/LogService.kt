package top.laoxin.modmanager.data.service

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.PathConstants

/** 日志服务 封装日志记录和 logcat 捕获功能 */
@Singleton
class LogService @Inject constructor(@param:ApplicationContext private val context: Context) {
    companion object {
        private const val TAG = "LogService"
        const val LOG_CAT_NAME = "logcat.txt"
        const val LOG_FILE_NAME = "log.txt"
        private const val MAX_LOG_SIZE = 10 * 1024 * 1024
    }

    private var logPath: String? = null
    private var logcatExecutor: ExecutorService? = null
    private var logRecordExecutor: ExecutorService? = null
    private val isLogcatRunning = AtomicBoolean(false)
    private var logcatProcess: Process? = null

    init {
        logRecordExecutor = Executors.newSingleThreadExecutor()
    }

    /**
     * 设置日志存储路径
     * @param path 日志目录路径
     */
    suspend fun setLogPath(path: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "setLogPath path: $path 更目录：${context.getExternalFilesDir(null)?.absolutePath}")
        try {
            if (path.isEmpty()) {
                Log.e(TAG, context.getString(R.string.log_path_empty))
                return@withContext
            }

            val directory = File(path)
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, context.getString(R.string.log_create_dir_failed))
                    return@withContext
                }
            }

            if (!directory.canWrite()) {
                Log.e(TAG, context.getString(R.string.log_path_not_writable))
                return@withContext
            }

            // 如果为存储根目录，则直接返回
            if (path == PathConstants.ROOT_PATH) {
                Log.e(TAG, context.getString(R.string.log_path_is_root))
                return@withContext
            }
            shutdown()
            logPath = path
            startLogcatLogging()
        } catch (e: Exception) {
            Log.e(TAG, "setLogPath error", e)
        }
    }

    /** 开始捕获 logcat 日志 */
    fun startLogcatLogging() {
        try {
            if (logPath.isNullOrEmpty()) {
                Log.e(TAG, context.getString(R.string.log_path_empty))
                return
            }

            if (isLogcatRunning.get()) {
                Log.i(TAG, context.getString(R.string.log_logcat_running))
                return
            }

            val logFile = File(logPath, LOG_CAT_NAME)

            val parentDir = logFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            if (logFile.exists()) {
                logFile.delete()
            }
            logFile.createNewFile()
            Log.e(TAG, "logcat 文件路径：${logFile.path}", )

            if (logcatExecutor == null || logcatExecutor?.isShutdown == true) {
                logcatExecutor = Executors.newSingleThreadExecutor()
            }

            logcatExecutor?.execute {
                isLogcatRunning.set(true)
                var writer: BufferedWriter? = null
                try {
                    logcatProcess =
                            Runtime.getRuntime().exec(arrayOf("logcat", "-v", "time", "*:V"))

                    val reader = InputStreamReader(logcatProcess?.inputStream)
                    writer = BufferedWriter(FileWriter(logFile, true), 8192)

                    val buffer = CharArray(4096)
                    var read: Int
                    var flushCounter = 0

                    while (isLogcatRunning.get()) {
                        read = reader.read(buffer)
                        if (read == -1) break

                        writer.write(buffer, 0, read)
                        flushCounter++

                        if (flushCounter >= 10) {
                            writer.flush()
                            flushCounter = 0
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "logcat error", e)
                } finally {
                    writer?.flush()
                    writer?.close()
                    logcatProcess?.destroy()
                    isLogcatRunning.set(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startLogcatLogging error", e)
            isLogcatRunning.set(false)
        }
    }

    /** 停止 logcat 捕获 */
    fun stopLogcat() {
        try {
            isLogcatRunning.set(false)
            logcatProcess?.destroy()
            logcatExecutor?.shutdown()
            logcatExecutor?.awaitTermination(2, TimeUnit.SECONDS)
        } catch (_: Exception) {}
    }

    /**
     * 记录日志到文件
     * @param log 日志内容
     */
    @Synchronized
    fun logRecord(log: String) {
        logRecordExecutor?.execute {
            try {
                if (logPath.isNullOrEmpty()) return@execute

                val file = File(logPath, LOG_FILE_NAME)

                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) return@execute
                }

                if (file.exists() && file.length() > MAX_LOG_SIZE) {
                    val backupFile = File(logPath, "${LOG_FILE_NAME}.old")
                    if (backupFile.exists()) {
                        backupFile.delete()
                    }
                    file.renameTo(backupFile)
                    file.createNewFile()
                }

                if (!file.exists()) {
                    file.createNewFile()
                }

                val currentDateAndTime =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Calendar.getInstance().time)

                val logWithDateAndTime = "$currentDateAndTime: $log\n"

                BufferedWriter(FileWriter(file, true)).use { writer ->
                    writer.write(logWithDateAndTime)
                    writer.flush()
                }
            } catch (_: Exception) {}
        }
    }

    /** 关闭日志服务 */
    fun shutdown() {
        try {
            stopLogcat()
            logRecordExecutor?.shutdown()
            if (logRecordExecutor?.awaitTermination(1, TimeUnit.SECONDS) == false) {
                logRecordExecutor?.shutdownNow()
            }
        } catch (_: Exception) {}
    }

    /** 刷新所有待写入的日志 */
    fun flushAll() {
        logRecordExecutor?.execute {

        }
    }
}
