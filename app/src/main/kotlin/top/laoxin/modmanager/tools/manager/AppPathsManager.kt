package top.laoxin.modmanager.tools.manager

import android.os.Environment
import top.laoxin.modmanager.App
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPathsManager @Inject constructor() {
    private val rootPath: String = Environment.getExternalStorageDirectory().path
    private val myAppPath: String = "$rootPath/Android/data/${App.get().packageName}/"
    private val backupPath: String = myAppPath + "backup/"
    private val modsTempPath: String = myAppPath + "temp/"
    private val modsUnzipPath: String = myAppPath + "temp/unzip/"
    private val modsIconPath: String = myAppPath + "icon/"
    private val modsImagePath: String = myAppPath + "images/"
    private val gameCheckFilePath: String = myAppPath + "gameCheckFile/"

    companion object {
        const val GAME_CONFIG_Path = "GameConfig/"
        const val DOWNLOAD_MOD_PATH = "/Download/Mods/"
        var MOD_PATH = ""
    }

    fun getRootPath(): String {
        return rootPath
    }

    fun getMyAppPath(): String {
        return myAppPath
    }

    fun getBackupPath(): String {
        return backupPath
    }

    fun getModsTempPath(): String {
        return modsTempPath
    }

    fun getModsUnzipPath(): String {
        return modsUnzipPath
    }

    fun getModsIconPath(): String {
        return modsIconPath
    }

    fun getModsImagePath(): String {
        return modsImagePath
    }

    fun getGameCheckFilePath(): String {
        return gameCheckFilePath
    }

    fun getGameConfig(): String {
        return GAME_CONFIG_Path
    }

    fun getDownloadModPath(): String {
        return DOWNLOAD_MOD_PATH
    }

    fun setModPath(path: String) {
        MOD_PATH = rootPath + path
    }

    fun getModPath(): String {
        return MOD_PATH
    }
}