package top.laoxin.modmanager.service.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import top.laoxin.modmanager.App
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.service.filetools.impl.ShizukuFileTools
import top.laoxin.modmanager.tools.ToastUtils


object FileExplorerServiceManager {
    const val TAG = "FileExplorerServiceManager"
    private var isBind = false

    val USER_SERVICE_ARGS: UserServiceArgs = UserServiceArgs(
        ComponentName(App.get().packageName, FileExplorerService::class.java.getName())
    ).daemon(false).debuggable(BuildConfig.DEBUG).processNameSuffix("file_explorer_service")
        .version(1)

    val SERVICE_CONNECTION: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (!isBind) {
                Log.d(TAG, "shizuku服务已连接 onServiceConnected: $name")
            }

            isBind = true

            ShizukuFileTools.iFileExplorerService = IFileExplorerService.Stub.asInterface(service)

        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "shizuku服务已关闭 onServiceDisconnected: $name")
            isBind = false
            ShizukuFileTools.iFileExplorerService = null
        }
    }

    fun bindService() {
        try {
            App.get()
        } catch (_: IllegalStateException) {
            Log.e(TAG, "Cannot bind service before application initialization!")
            return
        }
        if (!isBind) {
            Log.d(TAG, "bindService: isBind = $isBind")
        }
        Shizuku.bindUserService(USER_SERVICE_ARGS, SERVICE_CONNECTION)
    }
}

