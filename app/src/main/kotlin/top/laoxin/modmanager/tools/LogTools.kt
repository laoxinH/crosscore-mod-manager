package top.laoxin.modmanager.tools

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object LogTools {
    private var logPath: String? = null
    private var logcatExecutor: ExecutorService? = null
    private var logRecordExecutor: ExecutorService? = null
    private val isLogcatRunning = AtomicBoolean(false)
    private var logcatProcess: Process? = null

    private const val TAG = "LogTools"
    const val LOG_CAT_NAME = "logcat.txt"
    const val LOG_FILE_NAME = "log.txt"
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024

    init {
        logRecordExecutor = Executors.newSingleThreadExecutor()
    }

    fun setLogPath(path: String) {
        try {
            if (path.isEmpty()) {
                Log.e(TAG, App.get().getString(R.string.log_path_empty))
                return
            }

            val directory = File(path)
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, App.get().getString(R.string.log_create_dir_failed))
                    return
                }
            }

            if (!directory.canWrite()) {
                Log.e(TAG, App.get().getString(R.string.log_path_not_writable))
                return
            }

            logPath = path
            startLogcatLogging()
        } catch (e: Exception) {
            Log.e(TAG, "setLogPath error", e)
        }
    }

    fun startLogcatLogging() {
        try {
            if (logPath.isNullOrEmpty()) {
                Log.e(TAG, App.get().getString(R.string.log_path_empty))
                return
            }

            if (isLogcatRunning.get()) {
                Log.i(TAG, App.get().getString(R.string.log_logcat_running))
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

            if (logcatExecutor == null || logcatExecutor?.isShutdown == true) {
                logcatExecutor = Executors.newSingleThreadExecutor()
            }

            logcatExecutor?.execute {
                isLogcatRunning.set(true)
                var writer: BufferedWriter? = null
                try {
                    logcatProcess = Runtime.getRuntime().exec(
                        arrayOf("logcat", "-v", "time", "*:V")
                    )
                    
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

    fun stopLogcat() {
        try {
            isLogcatRunning.set(false)
            logcatProcess?.destroy()
            logcatExecutor?.shutdown()
            logcatExecutor?.awaitTermination(2, TimeUnit.SECONDS)
        } catch (_: Exception) {
        }
    }

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

                val currentDateAndTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Calendar.getInstance().time)

                val logWithDateAndTime = "$currentDateAndTime: $log\n"

                BufferedWriter(FileWriter(file, true)).use { writer ->
                    writer.write(logWithDateAndTime)
                    writer.flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun shutdown() {
        try {
            stopLogcat()
            logRecordExecutor?.shutdown()
            if (logRecordExecutor?.awaitTermination(1, TimeUnit.SECONDS) == false) {
                logRecordExecutor?.shutdownNow()
            }
        } catch (_: Exception) {
        }
    }
    
    fun flushAll() {
        logRecordExecutor?.execute { }
    }
}