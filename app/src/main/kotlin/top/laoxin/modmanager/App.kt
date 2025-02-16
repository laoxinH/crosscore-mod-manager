package top.laoxin.modmanager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.HiltAndroidApp
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.tools.LogTools
import java.io.File


@HiltAndroidApp
class App : Application() {

    companion object {
        var osVersion: OSVersion = OSVersion.OS_5
        var isHuawei: Boolean = false

        @Volatile
        private var instance: App? = null

        fun get(): App = instance ?: synchronized(this) {
            instance ?: throw IllegalStateException("Application not initialized!")
        }
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(App::class) {
            instance = this
        }

        initializeOsVersion()
        createTestFile()
        setupNotificationChannel()
        setupGlobalExceptionHandler()
    }

    // 初始化操作系统版本
    private fun initializeOsVersion() {
        val sdkVersion = Build.VERSION.SDK_INT
        osVersion = when {
            sdkVersion >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> OSVersion.OS_14
            sdkVersion >= Build.VERSION_CODES.TIRAMISU -> OSVersion.OS_13
            sdkVersion >= Build.VERSION_CODES.R -> OSVersion.OS_11
            sdkVersion >= Build.VERSION_CODES.M -> OSVersion.OS_6
            else -> OSVersion.OS_5
        }

        // Check for Huawei/HarmonyOS 4 specific conditions
        val incrementalVersion = Build.VERSION.INCREMENTAL
        isHuawei = osVersion == OSVersion.OS_11 &&
                (incrementalVersion.contains("104.0") || incrementalVersion.contains("104.2"))

        if (isHuawei) {
            osVersion = OSVersion.OS_14
        }

        Log.d("App", "Detected OS Version: $osVersion")
    }

    // 创建测试文件
    private fun createTestFile() {
        try {
            val filesDir = getExternalFilesDir(null)?.parent ?: return
            val testFile = File(filesDir, "test.txt")

            if (!testFile.exists()) {
                testFile.createNewFile()
                testFile.writeBytes("Hello  world!".toByteArray())
            }
        } catch (e: Exception) {
            Log.e("App", "Failed to create test file", e)
        }
    }

    // 设置通知渠道
    private fun setupNotificationChannel() {
        val channelId = getString(R.string.channel_id)
        val channelName = getString(R.string.channel_name)

        NotificationManagerCompat.from(this).apply {
            createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    // 设置全局异常处理
    private fun setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleGlobalException(thread, throwable)
        }
    }

    // 处理全局异常
    private fun handleGlobalException(thread: Thread, throwable: Throwable) {
        Log.e("GlobalException", "Uncaught exception in thread ${thread.name}", throwable)
        LogTools.logRecord("Uncaught  exception in thread ${thread.name}:  ${throwable.message}")
    }

}