package top.laoxin.modmanager.domain.service

import androidx.compose.ui.graphics.ImageBitmap
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.Result

/** 应用信息服务接口 封装 Android PackageManager 相关 API */
interface AppInfoService {

    /** 获取当前应用包名 */
    fun getPackageName(): String

    /**
     * 判断应用是否已安装
     * @param packageName 应用包名
     * @return Result<Unit> 是否已安装
     */
    fun isAppInstalled(packageName: String): Result<Unit>

    /**
     * 获取应用版本号
     * @param packageName 应用包名
     * @return Result<String> 版本名称
     */
    fun getVersionName(packageName: String): Result<String>

    /**
     * 获取应用图标
     * @param packageName 应用包名
     * @return ImageBitmap 应用图标
     */
    fun getAppIcon(packageName: String): ImageBitmap

    /**
     * 启动游戏
     * @param gameInfo 游戏信息
     * @return Result<Unit> 启动结果
     */
    fun startGame(gameInfo: GameInfoBean): Result<Unit>
}

