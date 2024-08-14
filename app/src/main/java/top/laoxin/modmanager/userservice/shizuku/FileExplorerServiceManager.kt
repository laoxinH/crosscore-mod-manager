package top.laoxin.modmanager.userservice.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import rikka.shizuku.shared.BuildConfig
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.fileToolsInterface.impl.ShizukuFileTools
import top.laoxin.modmanager.useservice.IFileExplorerService


object FileExplorerServiceManager {
    private const val TAG = "FileExplorerServiceManager"
    private var isBind = false
    private val USER_SERVICE_ARGS = UserServiceArgs(
        ComponentName(App.get().packageName, FileExplorerService::class.java.getName())
    ).daemon(false).debuggable(BuildConfig.DEBUG).processNameSuffix("file_explorer_service")
        .version(1)
    private val SERVICE_CONNECTION: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected: ")
            isBind = true
            ModTools.iFileExplorerService = IFileExplorerService.Stub.asInterface(service)
            ShizukuFileTools.iFileExplorerService = IFileExplorerService.Stub.asInterface(service)
            if (!isBind) {
                ToastUtils.shortCall(R.string.toast_shizuku_connected)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "onServiceDisconnected: ")
            isBind = false
            ModTools.iFileExplorerService = null
            ShizukuFileTools.iFileExplorerService = null
            ToastUtils.shortCall(R.string.toast_shizuku_disconnected)
        }
    }

    fun bindService() {
        Log.d(TAG, "bindService: isBind = " + isBind)
        Shizuku.bindUserService(USER_SERVICE_ARGS, SERVICE_CONNECTION)
    }
}

