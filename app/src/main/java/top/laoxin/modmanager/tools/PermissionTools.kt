package top.laoxin.modmanager.tools

import android.Manifest
import android.content.Context
import android.content.UriPermission
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.sui.Sui
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.RequestCode
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.tools.ModTools.ROOT_PATH
import top.laoxin.modmanager.userservice.shizuku.FileExplorerServiceManager


object PermissionTools {
    private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    private var osVersion = App.osVersion
    fun hasStoragePermission(): Boolean {
        val context: Context = App.get() as Context
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }


    // shizuku监听器
    var REQUEST_PERMISSION_RESULT_LISTENER =
        OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == RequestCode.SHIZUKU) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ModTools.setModsToolsSpecialPathReadType(PathType.SHIZUKU)
                    FileExplorerServiceManager.bindService()
                    //this.setScanQQDirectory(true)
                    ToastUtils.longCall(R.string.toast_shizuku_permission_granted)
                } else {
                    ToastUtils.longCall(R.string.toast_shizuku_permission_denied)
                    //this.setScanQQDirectory(false)
                }
            }
        }

    // 检查Shizuku权限 并重新绑定权限
    fun checkShizukuPermission(): Boolean {
        // 安卓11以下不需要Shizuku，使用File接口就能浏览/sdcard全部文件
        return if (isShizukuAvailable) {
            if (hasShizukuPermission()) {
                ModTools.setModsToolsSpecialPathReadType(PathType.SHIZUKU)
                FileExplorerServiceManager.bindService()
                true
            } else {
                //requestShizukuPermission()
                false
            }
        } else {
            false
        }
    }

    // 是否已安装shizuku/Sui
    private val isShizukuInstalled: Boolean
        get() {
            try {
                // 添加对Sui的检测
                if (Sui.init(App.get().packageName)) {
                    return true
                } else {
                    App.get().packageManager?.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
                    return true
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("PermissionTools", "Shizuku/Sui not installed")
            }
            return false
        }

    val isShizukuAvailable: Boolean
        get() = isShizukuInstalled && Shizuku.pingBinder()

    fun hasShizukuPermission(): Boolean {
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求权限。
     * @return Shizuku是否可用
     */
    fun requestShizukuPermission() {
        Shizuku.requestPermission(RequestCode.SHIZUKU)
    }

    fun hasUriPermission(path: String): Boolean {
        var noRootPath = path
        if (path.contains(ROOT_PATH)) noRootPath =
            noRootPath.replace("${ROOT_PATH}/", "")
        val uriPermissions = App.get().contentResolver.persistedUriPermissions
        for (uriPermission: UriPermission in uriPermissions) {
            val itemPath = uriPermission.uri.path
            Log.d("FileTools", "有权的路径: ${uriPermission.uri}")
            Log.d("FileTools", "权限的路径: $noRootPath")

            if (itemPath != null && ("$itemPath/").contains(noRootPath)) {
                return true
            }
        }
        Log.d("FileTools", "没有权限: $path")
        return false
    }

    // 检查全权限
    fun checkPermission(path: String): Int {
        if (isFromMyPackageNamePath(path) && App.osVersion == OSVersion.OS_11) {
            return PathType.FILE
        }
        when (App.osVersion) {
            OSVersion.OS_14 -> {
                if (isShizukuAvailable) {
                    return if (checkShizukuPermission()) {
                        return PathType.SHIZUKU
                    } else {
                        PathType.NULL
                    }
                }
            }

            OSVersion.OS_13 -> {
                var path1 = path
                if (isUnderAppDataPath(path)) {
                    path1 = getAppDataPath(path)
                }
                if (isShizukuAvailable) {
                    if (checkShizukuPermission()) {
                        return PathType.SHIZUKU
                    }
                }
                return if (hasUriPermission(path1)) {
                    PathType.DOCUMENT
                } else {
                    PathType.NULL
                }
            }

            OSVersion.OS_11 -> {
                var path1 = path
                if (isUnderDataPath(path)) {
                    path1 = ScanModPath.ANDROID_DATA
                }
                if (isShizukuAvailable) {
                    return if (checkShizukuPermission()) {
                        PathType.SHIZUKU
                    } else {
                        PathType.NULL
                    }
                }
                return if (hasUriPermission(path1)) {
                    PathType.DOCUMENT
                } else {
                    PathType.NULL
                }
            }

            OSVersion.OS_6 -> {
                return PathType.FILE
            }

            OSVersion.OS_5 -> return PathType.NULL
        }
        return PathType.NULL
    }

    private fun isUnderDataPath(path: String): Boolean {
        return path.contains("$ROOT_PATH/Android/data/")

    }

    fun getRequestPermissionPath(path: String): String {
        return if (App.osVersion == OSVersion.OS_11) {
            if (isUnderDataPath(path)) {
                ScanModPath.ANDROID_DATA
            } else {
                path
            }
        } else if (App.osVersion == OSVersion.OS_13) {
            if (isUnderAppDataPath(path)) {
                getAppDataPath(path)
            } else {
                path
            }
        } else {
            path
        }
    }

    fun isUnderAppDataPath(path: String): Boolean {
        // return true

        return if (path.contains("$ROOT_PATH/Android/data/")) {
            val appPath = path.replace("$ROOT_PATH/Android/data/", "").split("/".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]

            Log.d("FileTools", "检查是否是应用数据路径: $appPath")
            try {
                App.get().packageManager.getPackageInfo(appPath, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                Log.d("FileTools", "不是应用数据路径: $path")
                false
            }
        } else {
            false
        }
    }

    fun getAppDataPath(path: String): String {
        val appPath = path.replace("$ROOT_PATH/Android/data/", "").split("/".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        return "$ROOT_PATH/Android/data/$appPath"
    }

    fun isFromMyPackageNamePath(path: String): Boolean {
        return ("$path/").contains(
            (ROOT_PATH + "/Android/data/" + (App.get().packageName ?: "")).toString() + "/"
        )
    }
}

