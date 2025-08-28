package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.BackupBean
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectSnowTools @Inject constructor(
    private val permissionTools: PermissionTools,
    private val fileToolsManager: FileToolsManager,
    private val appPathsManager: AppPathsManager,
) : BaseSpecialGameTools {

    /***
     * 当shizuku服务中执行代码时不能包含任何调用shizuku行为否则报错, 同时也无法访问上下文
     */
    private var checkFilepath = ""
    private var checkFilenameModPath = ""
    private val gson: Gson = GsonBuilder().create()
    private lateinit var fileTools: BaseFileTools

    companion object {
        const val TAG = "ProjectSnowTools"
        const val CHECK_FILENAME = "manifest.json"
    }

    data class MainIFest(
        var version: String,
        var projectVersion: String,
        var pathOffset: String,
        var bUserCache: Boolean,
        var paks: MutableList<Pak> = mutableListOf()
    )

    data class Pak(
        val name: String,
        val hash: String,
        val sizeInBytes: Long,
        val bPrimary: Boolean,
        val base: String = "",
        val diff: String = "",
        val diffSizeBytes: Long = 0,
    )

    override fun specialOperationEnable(mod: ModBean, packageName: String): Boolean {
        checkFilenameModPath =
            appPathsManager.getGameCheckFilePath() + packageName + "/" + CHECK_FILENAME

        val unZipPath =
            appPathsManager.getModsUnzipPath() + packageName + "/" + File(mod.path!!).nameWithoutExtension + "/"
        val flag: MutableList<Boolean> = mutableListOf()

        var paks = mutableListOf<Pak>()
        if (File(checkFilenameModPath).exists()) {
            paks = gson.fromJson(
                File(checkFilenameModPath).readText(),
                MainIFest::class.java
            ).paks
        } else {
            File(checkFilenameModPath).parentFile?.mkdirs()
            File(checkFilenameModPath).createNewFile()
        }
        for (modFile in mod.modFiles!!) {
            val modFilePath = if (mod.isZipFile) {
                unZipPath + modFile
            } else {
                modFile
            }
            // 计算md5
            var md5 = calculateMD5(File(modFilePath).inputStream())
            if (md5 == null) {
                getZipFileInputStream(
                    zipFilePath = mod.path,
                    fileName = modFile,
                    password = mod.password!!
                )?.use {
                    md5 = calculateMD5(it)
                }
            }

            // 读取文件大小
            var fileSize = try {
                File(modFilePath).length()
            } catch (_: Exception) {
                null
            }
            if (fileSize == null) {
                getZipFileInputStream(
                    zipFilePath = mod.path,
                    fileName = modFile,
                    password = mod.password!!
                )?.use {
                    fileSize = getInputStreamSize(it)
                }
            }
            paks.add(Pak(File(modFilePath).name, md5!!, fileSize!!, false))
            MainIFest(
                version = "1.0",
                projectVersion = "1.0",
                pathOffset = "",
                bUserCache = true,
                paks = paks
            ).let {
                File(checkFilenameModPath).writeText(gson.toJson(it, MainIFest::class.java))
            }
        }
        return flag.all { it }

    }

    override fun specialOperationDisable(
        backup: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ): Boolean {
        checkFilenameModPath =
            appPathsManager.getGameCheckFilePath() + packageName + "/" + CHECK_FILENAME

        val mainIFest = gson.fromJson(
            File(checkFilenameModPath).readText(),
            MainIFest::class.java
        )

        val iterator = mainIFest.paks.iterator()
        while (iterator.hasNext()) {
            val pak = iterator.next()
            modBean.modFiles?.forEach {
                if (pak.name == File(it).name) {
                    iterator.remove()
                }
            }
        }

        File(checkFilenameModPath).writeText(gson.toJson(mainIFest, MainIFest::class.java))
        return true
    }

    override fun specialOperationStartGame(gameInfo: GameInfoBean): Boolean {
        Log.d(TAG, "specialOperationStartGame: 开始执行注入")
        checkFilenameModPath =
            appPathsManager.getGameCheckFilePath() + gameInfo.packageName + "/" + CHECK_FILENAME
        checkFilepath =
            "${appPathsManager.getRootPath()}/Android/data/${gameInfo.packageName}/files/"

        if (checkPermission() == PathType.NULL) return false
        val checkPermission = permissionTools.checkPermission(checkFilepath)
        // 通过documentFile读取文件
        if (checkPermission == PathType.DOCUMENT) {
            fileTools.copyFileByDF(
                checkFilepath + CHECK_FILENAME,
                appPathsManager.getModsUnzipPath() + CHECK_FILENAME
            )

        } else {
            fileTools.copyFile(
                checkFilepath + CHECK_FILENAME,
                appPathsManager.getModsUnzipPath() + CHECK_FILENAME
            )
        }
        val modPaks = gson.fromJson(
            File(checkFilenameModPath).readText(),
            MainIFest::class.java
        ).paks
        val mainIFest = gson.fromJson(
            File(appPathsManager.getModsUnzipPath() + CHECK_FILENAME).readText(),
            MainIFest::class.java
        )
        if (modPaks.isEmpty()) return true

        for (modPak in modPaks) {
            if (modPak !in mainIFest.paks) {
                mainIFest.paks.add(0, modPak)
            }
        }
        val mainIFestJson = gson.toJson(mainIFest, MainIFest::class.java)
        // mainIFest.paks.addAll(0,modPaks)
        val startTime = System.currentTimeMillis()
        while (true) {
            // Log.d("ProjectSnowTools", "specialOperationStartGame: 执行注入")
            fileTools.writeFile(
                checkFilepath,
                CHECK_FILENAME,
                mainIFestJson
            )
            // Thread.sleep(50)
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime > 40000) {
                Log.d(TAG, "specialOperationStartGame: 注入结束")
                break
            }
        }

        return true
    }

    override fun specialOperationScanMods(gameInfo: String, modFileName: String): Boolean {
        return modFileName.endsWith(".pak")
    }

    override fun specialOperationSelectGame(gameInfo: GameInfoBean): Boolean {
        Log.d("ProjectSnowTools", "特殊:$gameInfo ")
        if (checkPermission() == PathType.NULL) return false
        val gameFilepath = "${gameInfo.gamePath}/files/${getGameFileDir(gameInfo)}/"
        val name = getGameFileDir(gameInfo)
        //Log.d("ProjectSnowTools", "特殊: $gameFilepath--$name")
        if (!fileTools.createDictionary("$gameFilepath/test")) {
            fileTools.changDictionaryName(gameFilepath, name + "1")
            File(gameFilepath).parentFile?.parentFile?.let { fileTools.createDictionary(it.absolutePath) }
            File(gameFilepath).parentFile?.let { fileTools.createDictionary(it.absolutePath) }
            fileTools.createDictionary(gameFilepath)
        } else {
            Log.d("ProjectSnowTools", "开始删除测试文件")
            fileTools.deleteFile("$gameFilepath/test")
        }
        return true
    }

    override fun specialOperationNeedOpenVpn(): Boolean {
        return true
    }

    override fun needGameService(): Boolean {
        return true
    }

    override fun specialOperationBeforeStartGame(gameInfo: GameInfoBean): Int {
        if (checkPermission() == PathType.NULL) return ResultCode.NO_PERMISSION
        val gameFilepath = "${gameInfo.gamePath}/files/${getGameFileDir(gameInfo)}/"
        if (!fileTools.createDictionary("$gameFilepath/test")) {
            return ResultCode.GAME_UPDATE
        }
        return ResultCode.SUCCESS
    }

    override fun specialOperationUpdateGameInfo(gameInfo: GameInfoBean): GameInfoBean {
        return gameInfo.copy(
            gameFilePath = gameInfo.gameFilePath.map {
                (it + File.separator + getGameFileDir(gameInfo) + File.separator).replace("//", "/")
            }
        )
    }

    private fun checkPermission(): Int {
        val checkPermission = permissionTools.checkPermission(checkFilepath)
        when (checkPermission) {
            PathType.FILE -> {
                fileTools = fileToolsManager.getFileTools()
                return PathType.FILE
            }

            PathType.DOCUMENT -> {
                fileTools = fileToolsManager.getDocumentFileTools()
                return PathType.DOCUMENT
            }

            PathType.SHIZUKU -> {
                fileTools = fileToolsManager.getShizukuFileTools()
                return PathType.SHIZUKU
            }

            else -> {
                Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
                return PathType.NULL
            }
        }
    }

    private fun getGameFileDir(gameInfo: GameInfoBean): String {
        val version = gameInfo.version
        val regex = """^(\d+\.\d+\.\d+)""".toRegex()
        val matchResult = regex.find(version)
        val result = matchResult?.value ?: ""
        val modifiedResult =
            result.split('.').toMutableList().apply { this[2] = "0" }.joinToString(".")
        return modifiedResult
    }

}