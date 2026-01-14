package top.laoxin.modmanager.constant

import android.os.Environment
import top.laoxin.modmanager.App

/**
 * 应用路径常量 提供所有不可变的路径常量
 *
 * 注意：动态的 MOD_PATH 应通过 UserPreferencesRepository.selectedDirectory 获取
 */
object PathConstants {
    /** 外部存储根路径 */
    val ROOT_PATH: String = Environment.getExternalStorageDirectory().path

    /** 应用数据目录 */
    val APP_PATH: String = "$ROOT_PATH/Android/data/${App.get().packageName}/"

    /** 备份目录 */
    val BACKUP_PATH: String = APP_PATH + "backup/"

    /** 临时文件目录 */
    val MODS_TEMP_PATH: String = APP_PATH + "temp/"

    /** Mod 解压目录 */
    val MODS_UNZIP_PATH: String = APP_PATH + "temp/unzip/"

    /** Mod 图标目录 */
    val MODS_ICON_PATH: String = APP_PATH + "icon/"

    /** Mod 图片目录 */
    val MODS_IMAGE_PATH: String = APP_PATH + "images/"

    /** 游戏校验文件目录 */
    val GAME_CHECK_FILE_PATH: String = APP_PATH + "gameCheckFile/"

    /** 游戏配置目录名 */
    const val GAME_CONFIG_PATH = "GameConfig/"

    /** 下载 Mod 目录 */
    const val DOWNLOAD_MOD_PATH = "/Download/Mods/"

    /** 扫描 Mod 目录 */
    val SCAN_PATH_QQ = "$ROOT_PATH/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv/"
    val SCAN_PATH_DOWNLOAD = "$ROOT_PATH/Download/"
    /** Android 数据目录 */
    val ANDROID_DATA = "$ROOT_PATH/Android/data/"

    /**
     * 根据相对路径获取完整路径
     * @param relativePath 相对路径 (如 selectedDirectory)
     * @return 完整路径
     */
    fun getFullModPath(relativePath: String): String {
        if (relativePath.contains(ROOT_PATH)) return relativePath
        return ROOT_PATH + relativePath
    }
}
