package top.laoxin.modmanager.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.SpecialGameService

/** 应用信息服务 封装 Android PackageManager 相关 API */
@Singleton
class AppInfoService
@Inject
constructor(
    private val specialGameService: SpecialGameService,
    @param:ApplicationContext private val context: Context
) {

    /** 获取当前应用包名 */
    fun getPackageName(): String {
        return context.packageName
    }

    /**
     * 判断应用是否已安装
     * @param packageName 应用包名
     * @return Result<Boolean> 是否已安装
     */
    fun isAppInstalled(packageName: String): Result<Unit> {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            Result.Success(Unit)
        } catch (_: PackageManager.NameNotFoundException) {
            Result.Error(AppError.FileError.FileNotFound("packageName not found: $packageName"))
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * 获取应用版本号
     * @param packageName 应用包名
     * @return Result<String> 版本名称
     */
    fun getVersionName(packageName: String): Result<String> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
           // Log.i("AppInfoService", "getVersionName: packageName: $packageName, versionName: ${packageInfo.versionName}")
            Result.Success(packageInfo.versionName ?: "unknown")
        } catch (_: PackageManager.NameNotFoundException) {
            Result.Error(AppError.FileError.FileNotFound("packageName not found: $packageName"))
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * 获取应用图标
     * @param packageName 应用包名
     * @return Result<ImageBitmap> 应用图标
     */
    fun getAppIcon(packageName: String): ImageBitmap {
        Log.i("AppInfoService", "getAppIcon: packageName: $packageName")
         try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            var drawable = packageInfo.applicationInfo?.loadIcon(context.packageManager)
            val bitmap =
                when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    is AdaptiveIconDrawable -> {
                        createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).also { bitmap ->
                            val canvas = Canvas(bitmap)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                        }
                    }

                    else -> {
                        drawable =
                            context.resources.getDrawable(
                                R.drawable.app_icon,
                                context.theme
                            )
                        drawable.toBitmap()
                    }
                }
            return bitmap.asImageBitmap()

        } catch (e: Exception) {
            val drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
            return drawable.toBitmap().asImageBitmap()
        }
    }

    /** 启动前台服务（用于特殊游戏） */
    private fun startService() {
//        val intent = Intent(context, ProjectSnowStartService::class.java)
//        context.stopService(intent)
//        context.startForegroundService(intent)
    }



    /**
     * 启动游戏
     * @param gameInfo 游戏信息
     * @return Result<Unit> 启动结果
     */
    fun startGame(gameInfo: GameInfoBean): Result<Unit> {
        val intent = context.packageManager.getLaunchIntentForPackage(gameInfo.packageName)
            ?: return Result.Error(AppError.FileError.FileNotFound("launch intent not found: ${gameInfo.packageName}"))
        if (specialGameService.needGameService(gameInfo.packageName)) {
            startService()
        }
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }
}
