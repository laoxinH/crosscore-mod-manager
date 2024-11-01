package top.laoxin.modmanager.tools

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.Executors

object LogTools {
    private var logPah: String? = null

    fun setLogPath(path: String) {
        logPah = path
    }

    private const val TAG = "LogTools"
    private const val LOG_FILE_NAME = "logcat.txt"

    fun startLogcatLogging() {
        val logFile = File(logPah, LOG_FILE_NAME)
        logFile.writeText("")

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val process = Runtime.getRuntime().exec("logcat")
                val reader = InputStreamReader(process.inputStream)
                val writer = FileWriter(logFile, true)
                val buffer = CharArray(1024)
                var read: Int

                while (true) {
                    read = reader.read(buffer)
                    if (read == -1) break
                    writer.write(buffer, 0, read)
                }

                writer.close()
                reader.close()
            } catch (e: Exception) {
                Log.e(TAG, "启动 logcat 失败: $e")
            }
        }

    }

    fun logRecord(log: String) {
        try {
            if (logPah?.isEmpty() == true) {
                Log.i(TAG, "日志路径为空")
                return
            }
            val file = File(logPah + "log.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
            val fileWriter = FileWriter(file, true)

            // 获取当前日期和时间
            val currentDateAndTime = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Calendar.getInstance().time)

            // 在日志信息前添加日期和时间
            val logWithDateAndTime = "$currentDateAndTime: $log\n"

            fileWriter.write(logWithDateAndTime)
            fileWriter.close()
        } catch (e: Exception) {
            Log.e(TAG, "写入日志失败: $e")
        }
    }

}