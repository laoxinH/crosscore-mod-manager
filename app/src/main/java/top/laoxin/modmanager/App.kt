package top.laoxin.modmanager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.database.AppContainer
import top.laoxin.modmanager.database.AppDataContainer
import top.laoxin.modmanager.database.UserPreferencesRepository
import top.laoxin.modmanager.tools.LogTools
import java.io.File
import java.io.FileOutputStream
import java.util.Objects


private const val PREFERENCE_NAME = "mod_manager_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCE_NAME
)

class App : Application() {

    lateinit var userPreferencesRepository: UserPreferencesRepository
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
        container = AppDataContainer(this)
        sApp = this
        checkOsVersion()
        createFile()
        registerNotificationService()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 这里处理全局异常
            Log.e("GlobalException", "Uncaught exception in thread ${thread.name}", throwable)
            LogTools.logRecord("Uncaught exception in thread ${thread.name}: $throwable")
        }

    }

    private fun createFile() {
        try {
            val path = Objects.requireNonNull(getExternalFilesDir(null))?.getParent()
            Log.d("Appmy", "createFile: $path")
            val file = File(path, "test.txt")
            if (!file.exists()) {
                file.createNewFile()
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write("Hello world!".toByteArray())
                fileOutputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private lateinit var sApp: App
        lateinit var osVersion: OSVersion
        fun get(): App {
            return sApp
        }
    }

    private fun checkOsVersion() {
        val sdkVersion = android.os.Build.VERSION.SDK_INT
        osVersion = when {
            sdkVersion >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> OSVersion.OS_14
            sdkVersion >= android.os.Build.VERSION_CODES.TIRAMISU -> OSVersion.OS_13
            sdkVersion >= android.os.Build.VERSION_CODES.R -> OSVersion.OS_11
            sdkVersion >= android.os.Build.VERSION_CODES.M -> OSVersion.OS_6
            else -> OSVersion.OS_5
        }
        // 鸿蒙4.0
        val version = Build.VERSION.INCREMENTAL
        Log.d("App初始化", "checkOsVersion: $version")
        if (version.contains("Harmony", true) && version.contains("4.0")) {
            osVersion = OSVersion.OS_13
        }

        Log.d("App", "checkOsVersion: $osVersion")
    }

    // 注册系统通知服务
    private fun registerNotificationService() {
        val channelId = getString(R.string.channel_id)
        val channelName = getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}
