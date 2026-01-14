package top.laoxin.modmanager.data.service

import android.content.Context
import android.content.UriPermission
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.sui.Sui
import top.laoxin.modmanager.App
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.OSVersion
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.constant.RequestCode
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.service.shizuku.FileExplorerServiceManager

/** 权限服务实现类 封装 Shizuku、SAF、标准文件权限的检查和请求逻辑 */
@Singleton
class PermissionServiceImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) : PermissionService {
    companion object {
        private const val TAG = "PermissionService"
        private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    }

    private val rootPath: String = Environment.getExternalStorageDirectory().path

    // Shizuku 权限请求回调监听器
    val requestPermissionResultListener =
            OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == RequestCode.SHIZUKU) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        FileExplorerServiceManager.bindService()
                    }
                }
            }

    /** 注册 Shizuku 权限请求监听器 应在 Activity.onCreate 中调用 */
    override fun registerShizukuListener(): Result<Unit> {
        return try {
            if (isShizukuAvailable()) {
                Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.PermissionError.ShizukuNotRunning)
        }
    }

    /** 解除 Shizuku 权限请求监听器 应在 Activity.onDestroy 中调用 */
    override fun unregisterShizukuListener(): Result<Unit> {
        return try {
            if (isShizukuAvailable()) {
                Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.PermissionError.ShizukuNotRunning)
        }
    }

    /**
     * 检查 Shizuku 权限并绑定服务
     * @return Result<Boolean> 是否成功绑定
     */
    override fun checkAndBindShizuku(): Result<Boolean> {
        return try {
            if (!isShizukuInstalled()) {
                Result.Error(AppError.PermissionError.ShizukuNotInstalled)
            } else if (!Shizuku.pingBinder()) {
                Result.Error(AppError.PermissionError.ShizukuNotRunning)
            } else if (!hasShizukuPermission()) {
                Result.Error(AppError.PermissionError.ShizukuPermissionDenied)
            } else {
                FileExplorerServiceManager.bindService()
                Result.Success(true)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /** Shizuku 是否已安装 */
    private fun isShizukuInstalled(): Boolean {
        return try {
            if (Sui.init(context.packageName)) {
                true
            } else {
                context.packageManager?.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
                true
            }
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Shizuku 是否可用（已安装且正在运行） */
    override fun isShizukuAvailable(): Boolean {
        return isShizukuInstalled() && Shizuku.pingBinder()
    }

    /** 是否已获得 Shizuku 权限 */
    override fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /** 请求 Shizuku 权限 */
    override fun requestShizukuPermission(): Result<Unit> {
        return try {
            if (!isShizukuInstalled()) {
                Result.Error(AppError.PermissionError.ShizukuNotInstalled)
            } else if (!Shizuku.pingBinder()) {
                Result.Error(AppError.PermissionError.ShizukuNotRunning)
            } else {
                Shizuku.requestPermission(RequestCode.SHIZUKU)
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /** 检查是否有 URI 权限 */
    override fun hasUriPermission(path: String): Result<Boolean> {
        return try {
            var noRootPath = path
            if (path.contains(rootPath)) {
                noRootPath = noRootPath.replace("$rootPath/", "")
            }
            val uriPermissions = context.contentResolver.persistedUriPermissions
            for (uriPermission: UriPermission in uriPermissions) {
                val itemPath = uriPermission.uri.path
                //Log.d(TAG, "权限路径: $itemPath, 鉴权路径: $noRootPath")
                if (itemPath != null && ("$itemPath/").contains(noRootPath)) {

                    return Result.Success(true)
                }
            }
            //Log.d(TAG, "没有权限: $path")
            Result.Success(false)
        } catch (e: Exception) {
            Result.Error(AppError.PermissionError.UriPermissionNotGranted)
        }
    }

    /** 检查是否有全局存储权限 (Android 11+ MANAGE_EXTERNAL_STORAGE) */
    override fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Android 10 及以下不需要此权限
            true
        }
    }

    /** 获取指定路径的文件访问类型 */
    override fun getFileAccessType(path: String): FileAccessType {
       // Log.d(TAG, "checkPermission: 鉴权路径$path")
        if (isFromMyPackageNamePath(path) || path.contains("Download/")) {
            return FileAccessType.STANDARD_FILE
        }

        // 检查 Shizuku
        if (isShizukuAvailable() && hasShizukuPermission()) {
            val bindResult = checkAndBindShizuku()
            if (bindResult is Result.Success && bindResult.data) {
                return FileAccessType.SHIZUKU
            }
        }

        return when (App.osVersion) {
            OSVersion.OS_14 -> {
                if (isShizukuAvailable() && hasShizukuPermission()) {
                    FileAccessType.SHIZUKU
                } else {
                    FileAccessType.NONE
                }
            }
            OSVersion.OS_13 -> {
                var path1 = path
                if (isUnderAppDataPath(path)) {
                    path1 = getAppDataPath(path)
                }
                val hasUri = hasUriPermission(path1)
                when {
                    isShizukuAvailable() && hasShizukuPermission() -> FileAccessType.SHIZUKU
                    hasUri is Result.Success && hasUri.data -> FileAccessType.DOCUMENT_FILE
                    else -> FileAccessType.NONE
                }
            }
            OSVersion.OS_11 -> {
                var path1 = path
                if (isUnderDataPath(path)) {
                    path1 = PathConstants.ANDROID_DATA
                }
                val hasUri = hasUriPermission(path1)
                when {
                    isShizukuAvailable() && hasShizukuPermission() -> FileAccessType.SHIZUKU
                    hasUri is Result.Success && hasUri.data -> FileAccessType.DOCUMENT_FILE
                    else -> FileAccessType.NONE
                }
            }
            OSVersion.OS_6 -> FileAccessType.STANDARD_FILE
            OSVersion.OS_5 -> FileAccessType.NONE
        }
    }

    private fun isUnderDataPath(path: String): Boolean {
        return path.contains("$rootPath/Android/data/")
    }

    /** 获取需要请求权限的路径 */
    override fun getRequestPermissionPath(path: String): String {
        val osVersion = App.osVersion
        return when (osVersion) {
            OSVersion.OS_11 -> {
                if (isUnderDataPath(path)) {
                    PathConstants.ANDROID_DATA
                } else {
                    path
                }
            }
            OSVersion.OS_13 -> {
                if (isUnderAppDataPath(path)) {
                    getAppDataPath(path)
                } else {
                    path
                }
            }
            else -> path
        }
    }

    override fun isUnderAppDataPath(path: String): Boolean {
        return if (path.contains("$rootPath/Android/data/")) {
            val appPath =
                    path.replace("$rootPath/Android/data/", "")
                            .split("/".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]

          //  Log.d(TAG, "检查是否是应用数据路径: $appPath")
            try {
                context.packageManager.getPackageInfo(appPath, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                Log.d(TAG, "不是应用数据路径: $path")
                false
            }
        } else {
            false
        }
    }

    override fun getAppDataPath(path: String): String {
        val appPath =
                path.replace("$rootPath/Android/data/", "")
                        .split("/".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]
        return "$rootPath/Android/data/$appPath"
    }

    override fun isFromMyPackageNamePath(path: String): Boolean {
        return ("$path/").contains("$rootPath/Android/data/${context.packageName}/")
    }

    override fun getRootPath(): String = rootPath


    override fun checkPathPermissions(gamePath: String): Result<Unit> {
        // 检查存储权限
        if (!hasStoragePermission()) {
            return Result.Error(AppError.PermissionError.StoragePermissionDenied)
        }
        // 如果游戏目录在系统 data 目录下，检查 Shizuku 或 URI 权限

        if (isUnderAppDataPath(gamePath)) {
            // 检查 Shizuku 权限
            if (isShizukuAvailable() && hasShizukuPermission()
            ) {
                return Result.Success(Unit)
            }

            // 检查 URI 权限
            val gamePathUri = getRequestPermissionPath(gamePath)
            val uriResult = hasUriPermission(gamePathUri)
            if (uriResult is Result.Success && uriResult.data) {
                return Result.Success(Unit)
            }

            return Result.Error(AppError.PermissionError.UriPermissionNotGranted)
        }

        return Result.Success(Unit)
    }
}
