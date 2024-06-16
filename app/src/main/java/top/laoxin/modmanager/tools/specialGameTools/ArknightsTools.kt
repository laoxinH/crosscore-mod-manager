package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import com.google.gson.reflect.TypeToken
import top.laoxin.modmanager.App
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.data.backups.BackupRepository
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.DocumentFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.ShizukuFileTools
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object ArknightsTools : BaseSpecialGameTools {

    private val CHECK_FILEPATH = ModTools.ROOT_PATH + "/Android/data/com.hypergryph.arknights/files/AB/Android/"
    private const val CHECK_FILENAME_1 = "persistent_res_list.json"
    private const val CHECK_FILENAME_2 = "hot_update_list.json"
    val gson = GsonBuilder()
        /*.disableHtmlEscaping()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)*/
        .create()
    private val app = App.get()
    lateinit var fileTools : BaseFileTools
    data class HotUpdate(
        var fullPack: FullPack = FullPack(),
        var versionId: String = "",
        var abInfos: MutableList<AbInfo> = mutableListOf(),
        var countOfTypedRes : String = "",
        var packInfos : MutableList<AbInfo> = mutableListOf()
    )
    data class PersistentRes(
        val abInfos: MutableList<AbInfo> = mutableListOf(),

        )

    data class FullPack(
        val totalSize: Long = 0,
        val abSize : Long = 0,
        val type : String = "",
        val cid : Int = -1,
    )

    data class AbInfo(
        val name : String? ,
        val hash : String? ,
        val md5 : String?,
        val totalSize : Long?,
        val abSize : Long?,
        val thash : String?,
        val type : String?,
        val pid : String?,
        val cid : Int?,


        )
    override fun specialOperationEnable(mod: ModBean, packageName: String) : Boolean{
        val unZipPath = ModTools.MODS_UNZIP_PATH + packageName + "/" + File(mod.path!!).nameWithoutExtension + "/"
        val flag : MutableList<Boolean> = mutableListOf()
        for (modFile in  mod.modFiles!!){
            val modFilePath = if (mod.isZipFile) {
                unZipPath + modFile
            } else {
                modFile
            }
            // 计算md5
            var md5 = calculateMD5(File(modFilePath).inputStream())
            if (md5 == null){
                getZipFileInputStream(zipFilePath =  mod.path,fileName = modFile, password = mod.password!!)?.let {
                    md5 = calculateMD5(it)
                }
            }

            // 读取文件大小
            var fileSize = try {
                File(modFilePath).length()
            } catch (e: Exception) {
                null
            }
            if (fileSize == null) {
                getZipFileInputStream(zipFilePath = mod.path, fileName = modFile, password = mod.password!!)?.let {
                    fileSize = getInputStreamSize(it)
                }
            }

            val checkFileName = File(mod.gameModPath!!).name + "/" + File(modFilePath).name
            Log.d("ArknightsTools", "checkFileName: $checkFileName")
            Log.d("ArknightsTools", "md5: $md5")
            Log.d("ArknightsTools", "fileSize: $fileSize")
            flag.add(modifyCheckFile(checkFileName, md5!!, fileSize!!))
        }
        return flag.all { it }
    }

    override fun specialOperationDisable(backup: List<BackupBean>, packageName: String): Boolean {
        val flag : MutableList<Boolean> = mutableListOf()
        for (backupBean in backup) {
            // 计算md5
            var md5 = calculateMD5(File(backupBean.backupPath!!).inputStream())
            // 读取文件大小
            var fileSize = try {
                File(backupBean.backupPath).length()
            } catch (e: Exception) {
                0
            }
            val checkFileName = (File(backupBean.backupPath).parentFile?.name ?: "") + "/" + backupBean.filename
            Log.d("ArknightsTools", "checkFileName: $checkFileName")
            Log.d("ArknightsTools", "md5: $md5")
            Log.d("ArknightsTools", "fileSize: $fileSize")
            flag.add(modifyCheckFile(checkFileName, md5!!, fileSize))

        }
        return flag.all { it }

    }

    // 修改check文件
    private fun modifyCheckFile(fileName: String, md5: String, fileSize: Long) : Boolean {
        try {
            val checkPermission = PermissionTools.checkPermission(CHECK_FILEPATH)
            if (checkPermission == PathType.FILE) {
                Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
                fileTools = FileTools

            } else if (checkPermission == PathType.DOCUMENT) {
                fileTools = DocumentFileTools
            } else if (checkPermission == PathType.SHIZUKU) {
                fileTools = ShizukuFileTools
            } else {
                Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
                return false
            }
            // 通过documentFile读取文件
            if (checkPermission == PathType.FILE) {
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

            val checkFile1 = File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1)
            val checkFile2 = File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2)

            val checkFile1Map = gson.fromJson(
                File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1).readText(),
                PersistentRes::class.java
            )
            val checkFile2Map = gson.fromJson(
                File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2).readText(),
                HotUpdate::class.java
            )
            val abInfos1 = checkFile1Map.abInfos
            val abInfos2 = checkFile2Map.abInfos
            var index1 = -1
            abInfos1.forEachIndexed { index, mutableMap ->
                if (mutableMap.name == fileName) {
                    index1 = index
                }
            }
            Log.d("ArknightsTools", "index1: $index1")
            abInfos1[index1] = abInfos1[index1].copy(
                md5 = md5,
                abSize = fileSize
            )
            index1 = -1
            abInfos2.forEachIndexed { index, mutableMap ->
                if (mutableMap.name == fileName) {
                    index1 = index
                }
            }
            abInfos2[index1] = abInfos2[index1].copy(
                md5 = md5,
                abSize = fileSize
            )
            gson.toJson(checkFile1Map, PersistentRes::class.java).let { it ->
                if (checkFile1.exists()) {
                    checkFile1.delete()
                    checkFile1.createNewFile()
                    checkFile1.writeText(it)

                    if (checkPermission == PathType.DOCUMENT) {
                        fileTools.copyFileByFD(
                            ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1,
                            CHECK_FILEPATH + CHECK_FILENAME_1
                        )
                    } else {
                        fileTools.copyFile(
                            ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_1,
                            CHECK_FILEPATH + CHECK_FILENAME_1
                        )
                    }
                }
            }
            gson.toJson(checkFile2Map, HotUpdate::class.java).let {
                if (checkFile2.exists()) {
                    checkFile2.delete()
                    checkFile2.createNewFile()
                    checkFile2.writeText(it)
                    if (checkPermission == PathType.DOCUMENT) {
                        fileTools.copyFileByFD(
                            ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2,
                            CHECK_FILEPATH + CHECK_FILENAME_2
                        )
                    } else {
                        fileTools.copyFile(
                            ModTools.MODS_UNZIP_PATH + CHECK_FILENAME_2,
                            CHECK_FILEPATH + CHECK_FILENAME_2
                        )
                    }

                }
            }
            return true
        } catch (e: Exception) {
            Log.e("ArknightsTools", "modifyCheckFile: ${e.message}")
            return false
        }
    }


}