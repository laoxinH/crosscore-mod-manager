package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.DocumentFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.ShizukuFileTools
import java.io.File

object ProjectSnowTools : BaseSpecialGameTools {

    /***
     * 当shizuku服务中执行代码时不能包含任何调用shizuku行为否则报错, 同时也无法访问上下文
     */
    private var check_filepath = ""
    const val CHECK_FILENAME = "manifest.json"
    private var check_filename_mod_path = ""
    private var check_filename_backup_path = ""

    private val gson: Gson = GsonBuilder()
        /*.disableHtmlEscaping()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)*/
        .create()
    private lateinit var fileTools: BaseFileTools
    // var checkPermission = PermissionTools.checkPermission(check_filepath)

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
        check_filename_mod_path = ModTools.GAME_CHECK_FILE_PATH + packageName + "/" + CHECK_FILENAME
        /* check_filepath = "${ModTools.ROOT_PATH}/Android/data/$packageName/files/$CHECK_FILENAME"
         check_filename_backup_path = ModTools.BACKUP_PATH + packageName + "/backup_" + CHECK_FILENAME
         val backupCheckFile = File(check_filename_backup_path)
         if (checkPermission(check_filepath) == PathType.NULL) return false
         if (!backupCheckFile.exists()) {
             if (backupCheckFile.parentFile?.exists() == false) {
                 backupCheckFile.parentFile?.mkdirs()
             }
             val checkPermission = PermissionTools.checkPermission(check_filepath)
             when (checkPermission) {
                 PathType.FILE -> {
                     fileTools.copyFile(check_filepath, check_filename_backup_path)
                 }
                 PathType.DOCUMENT -> {
                     fileTools.copyFileByDF(check_filepath, check_filename_backup_path)
                 }
                 PathType.SHIZUKU -> {
                     fileTools.copyFile(check_filepath, check_filename_backup_path)
                 }
             }
         }*/


        val unZipPath =
            ModTools.MODS_UNZIP_PATH + packageName + "/" + File(mod.path!!).nameWithoutExtension + "/"
        val flag: MutableList<Boolean> = mutableListOf()

        var paks = mutableListOf<Pak>()
        if (File(check_filename_mod_path).exists()) {
            paks = gson.fromJson(
                File(check_filename_mod_path).readText(),
                MainIFest::class.java
            ).paks
        } else {
            File(check_filename_mod_path).parentFile?.mkdirs()
            File(check_filename_mod_path).createNewFile()
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
            } catch (e: Exception) {
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
                File(check_filename_mod_path).writeText(gson.toJson(it, MainIFest::class.java))
            }
        }
        return flag.all { it }

    }

    override fun specialOperationDisable(
        backup: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ): Boolean {
        check_filename_mod_path = ModTools.GAME_CHECK_FILE_PATH + packageName + "/" + CHECK_FILENAME
        /*        check_filepath = "${ModTools.ROOT_PATH}/Android/data/$packageName/files/$CHECK_FILENAME"
                check_filename_backup_path = ModTools.BACKUP_PATH + packageName + "/backup_" + CHECK_FILENAME
                val backupCheckFile = File(check_filename_backup_path)

                if (backupCheckFile.exists()) {
                    when (checkPermission(check_filepath)) {
                        PathType.FILE -> {
                            fileTools.copyFile(check_filename_backup_path, check_filepath)
                        }
                        PathType.DOCUMENT -> {
                            fileTools.copyFileByFD(check_filename_backup_path, check_filepath)
                        }
                        PathType.SHIZUKU -> {
                            fileTools.copyFile(check_filename_backup_path, check_filepath)
                        }
                        else -> return  false
                    }
                }*/

        val mainIFest = gson.fromJson(
            File(check_filename_mod_path).readText(),
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

        File(check_filename_mod_path).writeText(gson.toJson(mainIFest, MainIFest::class.java))
        return true
    }

    override fun specialOperationStartGame(gameInfo: GameInfoBean): Boolean {
        check_filename_mod_path =
            ModTools.GAME_CHECK_FILE_PATH + gameInfo.packageName + "/" + CHECK_FILENAME
        check_filepath = "${ModTools.ROOT_PATH}/Android/data/${gameInfo.packageName}/files/"

        if (checkPermission(check_filepath) == PathType.NULL) return false
        val checkPermission = PermissionTools.checkPermission(check_filepath)
        // 通过documentFile读取文件
        if (checkPermission == PathType.DOCUMENT) {
            fileTools.copyFileByDF(
                check_filepath + CHECK_FILENAME,
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME
            )

        } else {
            fileTools.copyFile(
                check_filepath + CHECK_FILENAME,
                ModTools.MODS_UNZIP_PATH + CHECK_FILENAME
            )
        }
        val modPaks = gson.fromJson(
            File(check_filename_mod_path).readText(),
            MainIFest::class.java
        ).paks
        val mainIFest = gson.fromJson(
            File(ModTools.MODS_UNZIP_PATH + CHECK_FILENAME).readText(),
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
            Log.d("ProjectSnowTools", "specialOperationStartGame: 开始执行注入")
            fileTools.writeFile(
                check_filepath,
                CHECK_FILENAME,
                mainIFestJson
            )
            // Thread.sleep(50)
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime > 40000) {
                break
            }
        }

        return true
    }

    override fun specialOperationCreateMods(gameInfo: GameInfoBean): List<ModBeanTemp> {
        TODO("Not yet implemented")
    }

    override fun specialOperationScanMods(gameInfo: String, modFileName: String): Boolean {
        return modFileName.endsWith(".pak")
    }

    override fun specialOperationSelectGame(gameInfo: GameInfoBean): Boolean {
        //Log.d("ProjectSnowTools", "特殊:$gameInfo ")
        if (checkPermission(gameInfo.gamePath) == PathType.NULL) return false
        val gameFilepath = "${gameInfo.gamePath}/files/${gameInfo.version}"
        val name = gameInfo.version
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

    private fun checkPermission(path: String): Int {
        val checkPermission = PermissionTools.checkPermission(check_filepath)
        //Log.d("ArknightsTools", "权限类型: $checkPermission--$path")
        if (checkPermission == PathType.FILE) {
            //Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
            fileTools = FileTools
            return PathType.FILE
        } else if (checkPermission == PathType.DOCUMENT) {
            fileTools = DocumentFileTools
            return PathType.DOCUMENT
        } else if (checkPermission == PathType.SHIZUKU) {
            fileTools = ShizukuFileTools
            return PathType.SHIZUKU
        } else {
            Log.e("ArknightsTools", "modifyCheckFile: 没有权限")
            return PathType.NULL
        }

    }


}