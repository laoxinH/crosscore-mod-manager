package top.laoxin.modmanager.tools

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.util.Locale

object LogTools {
    private var logPah: String? = null

    fun setLogPath(path: String) {
        logPah = path
    }

    private const val TAG = "LogTools"

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