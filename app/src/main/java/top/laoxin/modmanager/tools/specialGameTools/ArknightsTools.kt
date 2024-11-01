package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.DocumentFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.ShizukuFileTools
import java.io.File

object ArknightsTools : BaseSpecialGameTools {

    private var CHECK_FILEPATH = ""
    private const val CHECK_FILENAME_1 = "persistent_res_list.json"
    private const val CHECK_FILENAME_2 = "hot_update_list.json"
    private val gson: Gson = GsonBuilder()
        /*.disableHtmlEscaping()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)*/
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
        val manifestName: String = "",
        val manifestVersion: String = "",
        val abInfos: MutableList<AbInfo> = mutableListOf(),
    )

    data class AbInfo(
        val name: String?,
        val hash: String?,
        val md5: String?,
        val totalSize: Long?,
        val abSize: Long?,
        val thash: String?,
        val type: String?,
        val pid: String?,
        val cid: Int?,
        val cat: Int?,
        val meta: Int?,
    )

    override fun specialOperationEnable(mod: ModBean, packageName: String): Boolean {
        CHECK_FILEPATH = "${ModTools.ROOT_PATH}/Android/data/$packageName/files/AB/Android/"
        val unZipPath =
            ModTools.MODS_UNZIP_PATH + packageName + "/" + File(mod.path!!).nameWithoutExtension + "/"
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
        val checkFile1 = File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1)
        val checkFile2 = File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2)
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
                File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1).readText(),
                PersistentRes::class.java
            )
            hotUpdate = gson.fromJson(
                File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2).readText(),
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
        backups: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ): Boolean {
        CHECK_FILEPATH = "${ModTools.ROOT_PATH}/Android/data/$packageName/files/AB/Android/"
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
        backups.forEachIndexed { index, backupBean ->
            // 计算md5
            val md5 = calculateMD5(File(backupBean.backupPath!!).inputStream())
            // 读取文件大小
            val fileSize = File(backupBean.backupPath).length()
            val checkFileName =
                (File(backupBean.backupPath).parentFile?.name ?: "") + "/" + backupBean.filename
            flag.add(modifyCheckFile(checkFileName, md5!!, fileSize))
            onProgressUpdate("${index + 1}/${backups.size}")
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

    override fun specialOperationCreateMods(gameInfo: GameInfoBean): List<ModBeanTemp> {
        TODO("Not yet implemented")
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
        val checkPermission = PermissionTools.checkPermission(CHECK_FILEPATH)
        return if (checkPermission == PathType.DOCUMENT) {
            fileTools.copyFileByFD(
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1,
                CHECK_FILEPATH + CHECK_FILENAME_1
            )
            fileTools.copyFileByFD(
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2,
                CHECK_FILEPATH + CHECK_FILENAME_2
            )
        } else {
            fileTools.copyFile(
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1,
                CHECK_FILEPATH + CHECK_FILENAME_1
            )
            fileTools.copyFile(
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2,
                CHECK_FILEPATH + CHECK_FILENAME_2
            )
        }
    }

    private fun moveCheckFileToAppPath(): Boolean {
        val checkPermission = PermissionTools.checkPermission(CHECK_FILEPATH)
        Log.d("ArknightsTools", "文件权限: $checkPermission")
        // 通过documentFile读取文件
        return if (checkPermission == PathType.DOCUMENT) {
            fileTools.copyFileByDF(
                CHECK_FILEPATH + CHECK_FILENAME_1,
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1
            )
            fileTools.copyFileByDF(
                CHECK_FILEPATH + CHECK_FILENAME_2,
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2
            )
        } else {
            fileTools.copyFile(
                CHECK_FILEPATH + CHECK_FILENAME_1,
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1
            )
            fileTools.copyFile(
                CHECK_FILEPATH + CHECK_FILENAME_2,
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2
            )
        }
    }

    private fun initialFileTools(): Boolean {
        val checkPermission = PermissionTools.checkPermission(CHECK_FILEPATH)
        return if (checkPermission == PathType.FILE) {
            Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
            fileTools = FileTools
            true
        } else if (checkPermission == PathType.DOCUMENT) {
            fileTools = DocumentFileTools
            true
        } else if (checkPermission == PathType.SHIZUKU) {
            fileTools = ShizukuFileTools
            true
        } else {
            Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
            false
        }
    }
}