package top.laoxin.modmanager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
        setupLifecycleObserver()
    }

    private fun setupLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                LogTools.flushAll()
            }
        })

        try {
            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    LogTools.shutdown()
                } catch (_: Exception) {
                }
            })
        } catch (_: Exception) {
        }
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
        val phoneBrand = Build.MANUFACTURER.lowercase()
        isHuawei = phoneBrand.contains("harmony") || phoneBrand.contains("oce")
                || phoneBrand.contains("huawei")

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

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleGlobalException(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleGlobalException(thread: Thread, throwable: Throwable) {
        val deviceInfo =
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android: ${Build.VERSION.RELEASE}, SDK: ${Build.VERSION.SDK_INT}"
        val exceptionInfo =
            "Thread: ${thread.name}\nException: ${throwable::class.java.simpleName}\nMessage: ${throwable.message}"
        val stackTrace = throwable.stackTraceToString()

        Log.e("GlobalException", "$deviceInfo\n$exceptionInfo", throwable)
        LogTools.logRecord("=== Crash Report ===\n$deviceInfo\n$exceptionInfo\n\nStack Trace:\n$stackTrace")
    }

}