package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.BackupBean
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArknightsTools @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val permissionTools: PermissionTools,
    private val fileToolsManager: FileToolsManager
) : BaseSpecialGameTools {

    private var checkFilepath = ""
    private val checkFile1 = "persistent_res_list.json"
    private val checkFile2 = "hot_update_list.json"

    private val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
        .create()

    private lateinit var fileTools: BaseFileTools
    private lateinit var hotUpdate: HotUpdate
    private lateinit var persistentRes: PersistentRes

    data class HotUpdate(
        var versionId: String = "",
        var abInfos: MutableList<AbInfo> = mutableListOf(),
        var manifestName: String = "",
        var manifestVersion: String = "",
        var packInfos: MutableList<AbInfo> = mutableListOf()
    )

    data class PersistentRes(
        var manifestName: String = "",
        var manifestVersion: String = "",
        var abInfos: MutableList<AbInfo> = mutableListOf(),
    )

    data class AbInfo(
        var name: String? = null,
        var hash: String? = null,
        var md5: String? = null,
        var totalSize: Long? = null,
        var abSize: Long? = null,
        var type: String? = null,
        var pid: String? = null,
        var cid: Long? = null,
        var cat: Long? = null,
        var meta: Long? = null,
    )

    override fun specialOperationEnable(mod: ModBean, packageName: String): Boolean {
        checkFilepath =
            "${appPathsManager.getRootPath()}/Android/data/$packageName/files/Bundles/"
        val unZipPath =
            appPathsManager.getModsUnzipPath() + packageName + "/" + File(mod.path!!).nameWithoutExtension + "/"
        val flag: MutableList<Boolean> = mutableListOf()
        if (!initialFileTools()) {
            throw Exception("初始化文件工具失败")
        }
        if (!moveCheckFileToAppPath()) {
            throw Exception("复制校验JSON文件失败")
        }
        if (!loadCheckFile()) {
            throw Exception("加载校验JSON文件失败")
        }
        mod.modFiles?.forEachIndexed { index: Int, modFile: String ->
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
            val fileSize = File(modFilePath).length()

            val checkFileName = File(mod.gameModPath!!).name + "/" + File(modFilePath).name
            Log.d("ArknightsTools", "checkFileName: $checkFileName")
            Log.d("ArknightsTools", "md5: $md5")
            Log.d("ArknightsTools", "fileSize: $fileSize")
            flag.add(modifyCheckFile(checkFileName, md5!!, fileSize))
            onProgressUpdate("${index + 1}/${mod.modFiles.size}")
        }
        if (!writeCheckFile()) {
            throw Exception("写入校验JSON失败")
        }
        return if (flag.all { it }) {
            moveCheckFileToGamePath()
        } else {
            false
        }
    }

    private fun writeCheckFile(): Boolean {
        val checkFile1 = File(appPathsManager.getModsUnzipPath() + checkFile1)
        val checkFile2 = File(appPathsManager.getModsUnzipPath() + checkFile2)
        try {
            gson.toJson(persistentRes, PersistentRes::class.java).let { it ->
                if (checkFile1.exists()) {
                    checkFile1.delete()
                    checkFile1.createNewFile()
                    checkFile1.writeText(it)
                }
            }
            gson.toJson(hotUpdate, HotUpdate::class.java).let {
                if (checkFile2.exists()) {
                    checkFile2.delete()
                    checkFile2.createNewFile()
                    checkFile2.writeText(it)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("ArknightsTools", "writeCheckFile: $e")
            logRecord("ArknightsTools-writeCheckFile: $e")
            return false
        }
    }

    private fun loadCheckFile(): Boolean {
        try {
            persistentRes = gson.fromJson(
                File(appPathsManager.getModsUnzipPath() + checkFile1).readText(),
                PersistentRes::class.java
            )
            hotUpdate = gson.fromJson(
                File(appPathsManager.getModsUnzipPath() + checkFile2).readText(),
                HotUpdate::class.java
            )
            return true
        } catch (e: Exception) {
            Log.e("ArknightsTools", "loadCheckFile: $e")
            logRecord("ArknightsTools-loadCheckFile: $e")
            return false
        }
    }

    override fun specialOperationDisable(
        backup: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ): Boolean {
        checkFilepath =
            "${appPathsManager.getRootPath()}/Android/data/$packageName/files/Bundles/"
        if (!initialFileTools()) {
            throw Exception("初始化文件工具失败")
        }
        if (!moveCheckFileToAppPath()) {
            throw Exception("复制校验文件失败")
        }
        if (!loadCheckFile()) {
            throw Exception("加载校验文件失败")
        }
        val flag: MutableList<Boolean> = mutableListOf()
        backup.forEachIndexed { index, backupBean ->
            // 计算md5
            val md5 = calculateMD5(File(backupBean.backupPath!!).inputStream())
            // 读取文件大小
            val fileSize = File(backupBean.backupPath).length()
            val checkFileName =
                (File(backupBean.backupPath).parentFile?.name ?: "") + "/" + backupBean.filename
            flag.add(modifyCheckFile(checkFileName, md5!!, fileSize))
            onProgressUpdate("${index + 1}/${backup.size}")
        }
        if (!writeCheckFile()) {
            throw Exception("写入校验JSON失败")
        }
        return if (flag.all { it }) {
            moveCheckFileToGamePath()
        } else {
            false
        }
    }

    override fun specialOperationStartGame(gameInfo: GameInfoBean): Boolean {
        return true
    }

    override fun specialOperationScanMods(gameInfo: String, modFileName: String): Boolean {
        return false
    }

    override fun specialOperationSelectGame(gameInfo: GameInfoBean): Boolean {
        return true
    }

    override fun specialOperationNeedOpenVpn(): Boolean {
        return false
    }

    override fun needGameService(): Boolean {
        return false
    }

    override fun specialOperationUpdateGameInfo(gameInfo: GameInfoBean): GameInfoBean {
        return gameInfo
    }

    override fun specialOperationBeforeStartGame(gameInfo: GameInfoBean): Int {
        return ResultCode.SUCCESS
    }


    // 修改check文件
    private fun modifyCheckFile(fileName: String, md5: String, fileSize: Long): Boolean {
        try {
            val abInfos1 = persistentRes.abInfos
            val abInfos2 = hotUpdate.abInfos
            abInfos1.replaceAll {
                if (it.name?.contains(fileName) == true) {
                    it.copy(
                        md5 = md5,
                        abSize = fileSize
                    )
                } else {
                    it
                }
            }
            abInfos2.replaceAll {
                if (it.name?.contains(fileName) == true) {
                    it.copy(
                        md5 = md5,
                        abSize = fileSize
                    )
                } else {
                    it
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            logRecord("ArknightsTools-modifyCheckFile: $e")
            return false
        }
    }

    private fun moveCheckFileToGamePath(): Boolean {
        val checkPermission = permissionTools.checkPermission(checkFilepath)
        return if (checkPermission == PathType.DOCUMENT) {
            fileTools.copyFileByFD(
                appPathsManager.getModsUnzipPath() + checkFile1,
                checkFilepath + checkFile1
            )
            fileTools.copyFileByFD(
                appPathsManager.getModsUnzipPath() + checkFile2,
                checkFilepath + checkFile2
            )
        } else {
            fileTools.copyFile(
                appPathsManager.getModsUnzipPath() + checkFile1,
                checkFilepath + checkFile1
            )
            fileTools.copyFile(
                appPathsManager.getModsUnzipPath() + checkFile2,
                checkFilepath + checkFile2
            )
        }
    }

    private fun moveCheckFileToAppPath(): Boolean {
        val checkPermission = permissionTools.checkPermission(checkFilepath)
        Log.d("ArknightsTools", "文件权限: $checkPermission")
        // 通过documentFile读取文件
        return if (checkPermission == PathType.DOCUMENT) {
            fileTools.copyFileByDF(
                checkFilepath + checkFile1,
                appPathsManager.getModsUnzipPath() + checkFile1
            )
            fileTools.copyFileByDF(
                checkFilepath + checkFile2,
                appPathsManager.getModsUnzipPath() + checkFile2
            )
        } else {
            fileTools.copyFile(
                checkFilepath + checkFile1,
                appPathsManager.getModsUnzipPath() + checkFile1
            )
            fileTools.copyFile(
                checkFilepath + checkFile2,
                appPathsManager.getModsUnzipPath() + checkFile2
            )
        }
    }

    private fun initialFileTools(): Boolean {
        val checkPermission = permissionTools.checkPermission(checkFilepath)
        return when (checkPermission) {
            PathType.FILE -> {
                fileTools = fileToolsManager.getFileTools()
                true
            }

            PathType.DOCUMENT -> {
                fileTools = fileToolsManager.getDocumentFileTools()
                true
            }

            PathType.SHIZUKU -> {
                fileTools = fileToolsManager.getShizukuFileTools()
                true
            }

            else -> {
                Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
                false
            }
        }
    }
}