package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.tools.ModTools
import java.io.File

object ArknightsTools : BaseSpecialGameTools {
    private val CHECK_FILEPATH = ModTools.ROOT_PATH + "/Android/data/com.hypergryph.arknights/files/AB/Android/"
    private const val CHECK_FILENAME_1 = "persistent_res_list.json"
    private const val CHECK_FILENAME_2 = "hot_update_list.json"
    val gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()
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
            flag.add(modifyCheckFile(checkFileName, md5!!, fileSize))

        }
        return flag.all { it }

    }

    // 修改check文件
    private fun modifyCheckFile(fileName: String, md5: String, fileSize: Long) : Boolean {
        try {
            val mapType = object : TypeToken<MutableMap<String, Any>>() {}.type
            val checkFile1 = File(CHECK_FILEPATH + CHECK_FILENAME_1)
            val checkFile2 = File(CHECK_FILEPATH + CHECK_FILENAME_2)
            val checkFile1Map: MutableMap<String, Any> = gson.fromJson(checkFile1.readText(), mapType)
            val checkFile2Map: MutableMap<String, Any> = gson.fromJson(checkFile2.readText(), mapType)
            val abInfos1 = checkFile1Map["abInfos"] as List<MutableMap<String, Any>>
            val abInfos2 = checkFile2Map["abInfos"] as List<MutableMap<String, Any>>
            var index1 = -1
            abInfos1.forEachIndexed { index, mutableMap ->
                if (mutableMap["name"] == fileName) {
                    index1 = index
                }
            }
            abInfos1[index1]["md5"] = md5
            abInfos1[index1]["abSize"] = fileSize
            index1 = -1
            abInfos2.forEachIndexed { index, mutableMap ->
                if (mutableMap["name"] == fileName) {
                    index1 = index
                }
            }
            abInfos2[index1]["md5"] = md5
            abInfos2[index1]["abSize"] = fileSize
            gson.toJson(abInfos1, mapType).let {
                checkFile1.writeText(it)
            }
            gson.toJson(abInfos2, mapType).let {
                checkFile2.writeText(it)
            }
            return true
        } catch (e: Exception) {
            Log.e("ArknightsTools", "modifyCheckFile: ${e.message}")
            return false
        }
    }


}