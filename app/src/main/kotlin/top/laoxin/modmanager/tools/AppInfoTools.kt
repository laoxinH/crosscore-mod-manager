package top.laoxin.modmanager.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import top.laoxin.modmanager.userService.gamestart.ProjectSnowStartService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoTools @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val specialGameToolsManager: SpecialGameToolsManager,
    @param:ApplicationContext private val context: Context
) {

    fun getPackageName(): String {
        return context.packageName
    }

    // 通过报名判断app是否安装
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    // 获取app版本号
    fun getVersionName(packageName: String): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    // 通过包名读取应用信息
    fun getAppIcon(packageName: String): ImageBitmap {
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            var drawable = packageInfo.applicationInfo?.loadIcon(context.packageManager)
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                is AdaptiveIconDrawable -> {
                    createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).also { bitmap ->
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(
                            0, 0, canvas.width, canvas.height
                        )
                        drawable.draw(canvas)
                    }
                }

                else -> {
                    drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
                    drawable.toBitmap()
                }
            }
            return bitmap.asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            val drawable = context.resources.getDrawable(R.drawable.app_icon, context.theme)
            val bitmap = drawable.toBitmap()
            return bitmap.asImageBitmap()
        }
    }

    fun startService() {
        val intent = Intent(context, ProjectSnowStartService::class.java)
        context.stopService(intent)
        context.startForegroundService(intent)
    }

    fun startGame() {
        // 通过包名获取应用信息
        val gameInfo = gameInfoManager.getGameInfo()
        val intent = context.packageManager.getLaunchIntentForPackage(gameInfo.packageName)
        if (intent != null) {

            specialGameToolsManager.getSpecialGameTools(gameInfo.packageName)?.let { entry ->
                when (entry.specialOperationBeforeStartGame(gameInfo)) {
                    ResultCode.SUCCESS -> {
                        if (entry.needGameService()) {
                            startService()
                        }
                    }

                    ResultCode.GAME_UPDATE -> {
                        ToastUtils.longCall(
                            context.getString(
                                R.string.tosat_game_already_update,
                                gameInfo.gameName
                            )
                        )
                        return
                    }

                    ResultCode.NO_PERMISSION -> {
                        ToastUtils.longCall(R.string.toast_has_no_prim)
                        return
                    }
                }

            }

        }
        context.startActivity(intent)
    }
}
