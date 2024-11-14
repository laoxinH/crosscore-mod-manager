package top.laoxin.modmanager.tools

import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.RemoteException
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.constant.SpecialGame
import top.laoxin.modmanager.exception.CopyStreamFailedException
import top.laoxin.modmanager.listener.ProgressUpdateListener
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.DocumentFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.ShizukuFileTools
import top.laoxin.modmanager.useservice.IFileExplorerService
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Date

object ModTools {
    val ROOT_PATH: String = Environment.getExternalStorageDirectory().path
    val MY_APP_PATH =
        (ROOT_PATH + "/Android/data/" + (App.get().packageName ?: "")).toString() + "/"
    val BACKUP_PATH = MY_APP_PATH + "backup/"
    private val MODS_TEMP_PATH = MY_APP_PATH + "temp/"
    val MODS_UNZIP_PATH = MY_APP_PATH + "temp/unzip/"
    private val MODS_ICON_PATH = MY_APP_PATH + "icon/"
    private val MODS_IMAGE_PATH = MY_APP_PATH + "images/"
    const val GAME_CONFIG = "GameConfig/"
    const val DOWNLOAD_MOD_PATH = "/Download/Mods/"
    val GAME_CHECK_FILE_PATH = MY_APP_PATH + "gameCheckFile/"
    private var specialPathReadType: Int = PathType.DOCUMENT
    var iFileExplorerService: IFileExplorerService? = null
    private val PACKAGE_MANAGER: PackageManager =
        App.get().packageManager ?: throw NullPointerException()
    private const val TAG = "ModTools"
    private var fileTools: BaseFileTools? = null
    var progressUpdateListener: ProgressUpdateListener? = null

    fun setModsToolsSpecialPathReadType(type: Int) {
        when (type) {
            PathType.FILE -> {
                specialPathReadType = PathType.FILE
                fileTools = FileTools
            }

            PathType.DOCUMENT -> {
                specialPathReadType = PathType.DOCUMENT
                fileTools = DocumentFileTools
            }

            PathType.SHIZUKU -> {
                specialPathReadType = PathType.SHIZUKU
                fileTools = ShizukuFileTools
            }
        }
    }


    suspend fun backupGameFiles(
        gameModPath: String,
        modBean: ModBean,
        gameBackupPath: String,
    ): List<BackupBean> {
        val list: MutableList<BackupBean> = mutableListOf()
        // 通过ZipTools解压文件到modTempPath
        val checkPermission = PermissionTools.checkPermission(gameModPath)
        setModsToolsSpecialPathReadType(checkPermission)
        Log.d(TAG, "游戏mod路径: ${modBean.modFiles}")
        modBean.modFiles?.forEachIndexed { index: Int, it: String ->
            val file = File(it)
            val backupPath =
                BACKUP_PATH + gameBackupPath + File(modBean.gameModPath!!).name + "/" + file.name
            val gamePath = gameModPath + file.name
            if (!File(backupPath).exists()) {
                withContext(Dispatchers.IO) {
                    if (if (specialPathReadType == PathType.DOCUMENT) {
                            fileTools?.copyFileByDF(gamePath, backupPath) == true
                        } else {
                            fileTools?.copyFile(gamePath, backupPath) == true
                        } && fileTools?.isFileExist(backupPath) == true
                    ) {
                        list.add(
                            BackupBean(
                                id = 0,
                                filename = file.name,
                                gamePath = modBean.gameModPath,
                                gameFilePath = gamePath,
                                backupPath = backupPath,
                                gamePackageName = modBean.gamePackageName,
                                modName = modBean.name
                            )
                        )
                        progressUpdateListener?.onProgressUpdate("${index + 1}/${modBean.modFiles.size}")
                    }
                }
            }

        }
        return list
    }


    suspend fun copyModFiles(
        modBean: ModBean,
        gameModPath: String,
        unZipPath: String,
    ): Boolean {
        val checkPermission = PermissionTools.checkPermission(gameModPath)
        setModsToolsSpecialPathReadType(checkPermission)
        val flags = mutableListOf<Boolean>()
        modBean.modFiles?.forEachIndexed { index: Int, modFilePath: String ->
            val modFile = File(unZipPath + modFilePath)
            val gameFile = File(gameModPath + modFile.name)
            var flag = false
            flag = if (specialPathReadType == PathType.DOCUMENT) {
                fileTools?.copyFileByFD(modFile.absolutePath, gameFile.absolutePath) == true

            } else {
                fileTools?.copyFile(modFile.absolutePath, gameFile.absolutePath) == true

            }
            flags.add(flag)
            progressUpdateListener?.onProgressUpdate("${index + 1}/${modBean.modFiles.size}")
        }
        return flags.all { it }

    }

    suspend fun restoreGameFiles(backups: List<BackupBean?>) {
        if (backups.isEmpty()) return
        val checkPermission = PermissionTools.checkPermission(backups[0]?.gameFilePath ?: "")
        setModsToolsSpecialPathReadType(checkPermission)
        val flags = mutableListOf<Boolean>()
        backups.forEachIndexed { index, backup ->
            if (checkPermission == PathType.DOCUMENT && backup != null) {
                flags.add(
                    fileTools?.copyFileByFD(
                        backup.backupPath!!, backup.gameFilePath!!
                    ) == true
                )
            } else if (backup != null) {
                flags.add(
                    fileTools?.copyFile(
                        backup.backupPath!!, backup.gameFilePath!!
                    ) == true
                )
            }
            progressUpdateListener?.onProgressUpdate("${index}/${backups.size}")

        }
        if (!flags.all { it }) {
            throw IOException(App.get().getString(R.string.toast_restore_failed))
        }
    }

    suspend fun deleteTempFile(): Boolean {
        return if (File(MODS_TEMP_PATH).exists()) {
            setModsToolsSpecialPathReadType(PathType.FILE)
            fileTools?.deleteFile(MODS_TEMP_PATH) == true
        } else {
            true
        }
    }

    suspend fun deleteBackupFiles(
        gameInfo: GameInfoBean
    ): Boolean {

        return if (File(BACKUP_PATH + gameInfo.packageName).exists()) {
            setModsToolsSpecialPathReadType(PathType.FILE)
            kotlin.runCatching { fileTools?.deleteFile(BACKUP_PATH + gameInfo.packageName) }.isSuccess
        } else {
            true
        }
    }

    suspend fun deleteMods(mods: List<ModBean>): Boolean {
        return try {
            setModsToolsSpecialPathReadType(PathType.FILE)
            mods.forEach {
                fileTools?.deleteFile(it.path!!) == true
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    // 删除缓存文件
    suspend fun deleteCache(): Boolean {

        return if (File(MODS_IMAGE_PATH).exists()) {
            setModsToolsSpecialPathReadType(PathType.FILE)
            kotlin.runCatching {
                fileTools?.deleteFile(MODS_ICON_PATH)
                fileTools?.deleteFile(MODS_IMAGE_PATH)
            }.isSuccess
        } else {
            true
        }

    }

    // 读取mod信息
    suspend fun readModInfo(unZipPath: String, modBean: ModBean): ModBean {
        var bean = modBean.copy(description = "MOD已解密")
        // 读取readme
        bean = readReadmeFile(unZipPath, bean)
        // 读取icon文件输入流
        if (bean.icon != null) {
            val iconPath = unZipPath + bean.icon
            val file = File(iconPath)
            val md5 = MD5Tools.calculateMD5(file.inputStream())
            fileTools?.copyFile(iconPath, MODS_ICON_PATH + md5 + file.name)
            bean = bean.copy(icon = MODS_ICON_PATH + md5 + file.name)
        }
        // 读取images文件输入流
        if (bean.images != null) {
            val images = mutableListOf<String>()
            for (image in bean.images!!) {
                val imagePath = unZipPath + image
                val file = File(imagePath)
                val md5 = MD5Tools.calculateMD5(file.inputStream())
                fileTools?.copyFile(imagePath, MODS_IMAGE_PATH + md5 + file.name)
                images.add(MODS_IMAGE_PATH + md5 + file.name)
            }
            bean = bean.copy(images = images)
        }
        return bean
    }


    // 读取readme文件
    private suspend fun readReadmeFile(unZipPath: String, modBean: ModBean): ModBean {
        // 判断是否存在readme文件
        val infoMap = mutableMapOf<String, String>()
        if (modBean.readmePath != null) {
            val readmeFile = File(unZipPath + modBean.readmePath)
            if (readmeFile.exists()) {
                val reader = readmeFile.bufferedReader()
                val lines = reader.readLines()
                for (line in lines) {
                    val parts = line.split("：")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        infoMap[key] = value
                    }
                }
            }
        } else if (modBean.fileReadmePath != null) {
            val readmeFile = File(unZipPath + modBean.fileReadmePath)
            if (readmeFile.exists()) {
                val reader = readmeFile.bufferedReader()
                val lines = reader.readLines()
                for (line in lines) {
                    val parts = line.split("：")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        infoMap[key] = value
                    }
                }
            }
        }
        return if (infoMap.isNotEmpty()) {
            modBean.copy(
                name = infoMap["名称"],
                description = infoMap["描述"],
                version = infoMap["版本"],
                author = infoMap["作者"]
            )
        } else {
            modBean
        }
    }


    // 通过shizuku扫描mods
    private suspend fun scanModsByShizuku(
        scanPath: String, gameInfo: GameInfoBean
    ): Boolean {
        return try {
            iFileExplorerService?.scanMods(scanPath, gameInfo) == true
        } catch (e: RemoteException) {
            withContext(Dispatchers.Main) {
                Log.e(TAG, "扫描文件失败: $e")
                ToastUtils.longCall(R.string.toast_shizuku_load_file_failed)
            }

            e.printStackTrace()
            false
        }
    }

    // 通过DocumentFile扫描mods
    suspend fun scanMods(scanPath: String, gameInfo: GameInfoBean): Boolean {
        // shizuku扫描
        setModsToolsSpecialPathReadType(PermissionTools.checkPermission(scanPath))
        if (specialPathReadType == PathType.SHIZUKU) {
            return scanModsByShizuku(scanPath, gameInfo)
        }
        var tempScanPath = scanPath
        if (scanPath == ScanModPath.MOD_PATH_QQ) {
            setModsToolsSpecialPathReadType(PathType.DOCUMENT)
            Log.d(TAG, "开始扫描QQ: ")
            tempScanPath = MODS_TEMP_PATH + "Mods/"
            val pathUri = fileTools?.pathToUri(ScanModPath.MOD_PATH_QQ)
            val scanModsFile = DocumentFile.fromTreeUri(App.get(), pathUri!!)
            val documentFiles = scanModsFile?.listFiles()
            Log.d(TAG, "扫描到的文件:$documentFiles ")
            for (documentFile in documentFiles!!) {
                Log.d(TAG, "扫描到的文件: ${documentFile.name}")
                try {
                    if (isScanFile(documentFile)) {
                        fileTools?.copyFileByDF(
                            ScanModPath.MOD_PATH_QQ + documentFile.name!!,
                            tempScanPath + documentFile.name!!
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "扫描文件失败: $e")
                    logRecord("扫描$scanPath--文件名${documentFile.name} 失败: $e")
                }
            }
        }
        setModsToolsSpecialPathReadType(PathType.FILE)
        File(tempScanPath).listFiles()?.forEach { file ->
            if (ArchiveUtil.isArchive(file.absolutePath)) {
                for (filename in ArchiveUtil.listInArchiveFiles(file.absolutePath)) {
                    if (gameInfo.gameFilePath.map { it + File(filename).name }.any {
                            setModsToolsSpecialPathReadType(PermissionTools.checkPermission(it))
                            fileTools?.isFileExist(it) == true
                        } || specialOperationScanMods(gameInfo.packageName, filename)) {
                        Log.d(TAG, "开始移动文件: ${gameInfo.modSavePath + file.name}")
                        setModsToolsSpecialPathReadType(PathType.FILE)
                        fileTools?.moveFile(file.absolutePath, gameInfo.modSavePath + file.name)
                        break
                    }
                }
            }
        }
        fileTools?.deleteFile(MODS_TEMP_PATH + "Mods/")
        return true

    }


    private fun createModTempMap(
        filepath: String?,
        scanPath: String,
        files: List<String>,
        gameInfo: GameInfoBean
    ): MutableMap<String, ModBeanTemp> {
        val archiveFile: File? = filepath?.let { File(it) }
        setModsToolsSpecialPathReadType(PermissionTools.checkPermission(gameInfo.gamePath))
        // 创建一个ModBeanTemp的map
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()
        for (file in files) {
            val modFileName = File(file).name
            val gameFileMap = mutableMapOf<String, String>()
            if (!gameInfo.isGameFileRepeat) {
                gameFileMap.apply {
                    gameInfo.gameFilePath.forEachIndexed { index, _ ->
                        put(
                            gameInfo.modType[index],
                            File(gameInfo.gameFilePath[index], modFileName).absolutePath
                        )
                    }
                }
            } else {
                gameFileMap.apply {
                    gameInfo.gameFilePath.forEachIndexed { index, it ->
                        val pathName = File(it).name
                        Log.d(
                            "ZipTools",
                            "${pathName}----${(File(file).parentFile?.name ?: "0")}"
                        )
                        if (pathName == (File(file).parentFile?.name ?: "")) {
                            put(
                                gameInfo.modType[index],
                                File(gameInfo.gameFilePath[index], modFileName).absolutePath
                            )
                        }
                    }
                }
            }

            Log.d("ZipTools", "gameFileMap: $gameFileMap")
            gameFileMap.forEach {

                if ((fileTools?.isFileExist(it.value) == true && fileTools?.isFile(it.value) == true)
                    || specialOperationScanMods(gameInfo.packageName, modFileName)
                ) {
                    val modEntries = File(file.replace(scanPath, ""))
                    val key = modEntries.parent ?: archiveFile?.name ?: modEntries.name
                    val modBeanTemp = modBeanTempMap[key]
                    if (modBeanTemp == null) {
                        val beanTemp = ModBeanTemp(
                            name = (archiveFile?.nameWithoutExtension + if (modEntries.parentFile == null) {
                                if (archiveFile == null) modEntries.name else ""
                            } else "(" + modEntries.parentFile!!.path.replace(
                                "/",
                                "|"
                            ) + ")").replace(
                                "null",
                                App.get().getString(R.string.mod_temp_bean_name_dictionary_desc)
                            ), // 如果是根目录则不加括号
                            iconPath = null,
                            readmePath = null,
                            modFiles = mutableListOf(file),
                            images = mutableListOf(),
                            fileReadmePath = null,
                            isEncrypted = if (archiveFile == null) false else ArchiveUtil.isArchiveEncrypted(
                                archiveFile.absolutePath
                            ),
                            gamePackageName = gameInfo.packageName,
                            modType = it.key,
                            gameModPath = gameInfo.gameFilePath[gameInfo.modType.indexOf(it.key)],
                            isZip = if (archiveFile == null) false else ArchiveUtil.isArchive(
                                archiveFile.absolutePath
                            ),
                            modPath = if (archiveFile == null) File(file).parent
                                ?: file else archiveFile.absolutePath,
                            virtualPaths = if (archiveFile == null) "" else archiveFile.absolutePath
                                    + File(file).parentFile?.absolutePath
                        )
                        modBeanTempMap[key] = beanTemp
                    } else {
                        modBeanTemp.modFiles.add(file)
                    }
                }

            }
        }/*File(gameFileModPath,modName)*/
        Log.d(TAG, "modBeanTempMap: $modBeanTempMap")
        // 判断是否存在readme.txt文件
        for (file in files) {
            val modEntries = File(file.replace(scanPath, ""))
            val key = modEntries.parent ?: archiveFile?.name ?: modEntries.name
            if (file.substringAfterLast("/").equals("readme.txt", ignoreCase = true)) {
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp != null) {
                    Log.d(TAG, "readmePath: $file")
                    modBeanTemp.readmePath = file
                }
            } else if (file.contains(".jpg", ignoreCase = true) ||
                file.contains(".png", ignoreCase = true) ||
                file.contains(".gif", ignoreCase = true) ||
                file.contains(".jpeg", ignoreCase = true)
            ) {
                val modBeanTemp = modBeanTempMap[key]
                modBeanTemp?.images?.add(file)
                modBeanTemp?.iconPath = file
            } else if (file.equals("readme.txt", ignoreCase = true)) {
                modBeanTempMap.forEach {
                    val modBeanTemp = it.value
                    modBeanTemp.fileReadmePath = file
                }
            }
        }
        return modBeanTempMap
    }


    suspend fun copyModStreamByShizuku(
        modTempPath: String, gameModPath: String, files: List<String>, password: String?
    ): Boolean {
        val flags: MutableList<Boolean> = ArrayList()
        try {
            files.forEach {
                flags.add(
                    iFileExplorerService?.unZipFile(modTempPath, gameModPath, it, password) == true
                )
            }

        } catch (e: RemoteException) {
            flags.add(false)
        }
        if (!flags.all { it }) {
            // 抛出流复制失败异常
            throw CopyStreamFailedException(App.get().getString(R.string.toast_copy_failed))
        }
        return true
    }


    // 修改文件权限
    fun chmod(path: String?): Boolean {
        return try {
            iFileExplorerService?.chmod(path) == true
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }

    // 扫描压缩文件mod
    suspend fun scanArchiveMods(
        scanPath: String,
        gameInfo: GameInfoBean,
    ): MutableList<ModBean> {
        setModsToolsSpecialPathReadType(PermissionTools.checkPermission(scanPath))
        val scanMods = mutableListOf<ModBean>()
        val archiveFiles = mutableListOf<File>()

        // 收集所有符合条件的压缩包，包含子文件夹
        withContext(Dispatchers.IO) {
            Files.walk(Paths.get(scanPath)).use { paths ->
                paths.filter { path ->
                    val file = path.toFile()
                    !file.isDirectory && fileTools?.isExcludeFileType(file.name) == false && ArchiveUtil.isArchive(
                        file.absolutePath
                    )
                }.forEach { path ->
                    archiveFiles.add(path.toFile())
                }
            }
        }

        // 处理收集到的压缩包mod
        for (file in archiveFiles) {
            val modTempMap = createModTempMap(
                file.absolutePath,
                scanPath,
                ArchiveUtil.listInArchiveFiles(file.absolutePath),
                gameInfo
            )
            Log.d(TAG, "modTempMap: $modTempMap")

            val mods = readModBeans(
                file.absolutePath,
                scanPath,
                modTempMap,
            )
            scanMods.addAll(mods)
        }

        Log.d(TAG, "modsList: $scanMods")
        return scanMods
    }

    // 扫描文件夹mod
    suspend fun scanDirectoryMods(scanPath: String, gameInfo: GameInfoBean): List<ModBean> {
        val checkPermission = PermissionTools.checkPermission(gameInfo.gamePath)
        setModsToolsSpecialPathReadType(checkPermission)
        // 遍历scanPath及其子目录中的文件
        try {
            val modBeans = mutableListOf<ModBean>()
            val files = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                Files.walk(Paths.get(scanPath))
            }.sorted(Comparator.reverseOrder()).forEach {
                files.add(it.toString())
            }
            Log.d(TAG, "所有文件路径: $files")
            val createModTempMap = createModTempMap(null, scanPath, files, gameInfo)
            modBeans.addAll(readModBeans(null, scanPath, createModTempMap))
            return modBeans
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            logRecord("扫描文件夹Mod失败: $e")
            return emptyList()
        }
    }

    private suspend fun readModBeans(
        filepath: String?,
        scanPath: String,
        modTempMap: MutableMap<String, ModBeanTemp>,
    ): List<ModBean> {
        if (modTempMap.isEmpty()) {
            return emptyList()
        }
        val archiveFile: File? = filepath?.let { File(it) }
        val list = mutableListOf<ModBean>()
        for (entry in modTempMap.entries) {
            val modBeanTemp = entry.value
            val modBean = ModBean(
                id = 0,
                name = modBeanTemp.name,
                version = "1.0",
                description = App.get().getString(R.string.mod_bean_no_readme),
                author = App.get().getString(R.string.mod_bean_no_author),
                date = archiveFile?.lastModified() ?: Date().time,
                path = modBeanTemp.modPath,
                virtualPaths = modBeanTemp.virtualPaths,
                icon = modBeanTemp.iconPath,
                images = modBeanTemp.images,
                modFiles = modBeanTemp.modFiles,
                isEncrypted = false,
                password = null,
                readmePath = modBeanTemp.readmePath,
                fileReadmePath = modBeanTemp.fileReadmePath,
                isEnable = false,
                gamePackageName = modBeanTemp.gamePackageName,
                gameModPath = modBeanTemp.gameModPath,
                modType = modBeanTemp.modType,
                isZipFile = modBeanTemp.isZip
            )
            if (modBeanTemp.isEncrypted) {
                list.add(
                    modBean.copy(
                        version = App.get().getString(R.string.mod_bean_encrypted_version),
                        description = App.get().getString(R.string.mod_bean_encrypted_desc),
                        author = App.get().getString(R.string.mod_bean_encrypted_version),
                        isEncrypted = true
                    )
                )
            } else {
                if (archiveFile != null) {
                    //val modBean = ZipTools.readModBean(zipFile, modBeanTemp, imagesPath)

                    var mod = modBean
                    if (modBeanTemp.fileReadmePath != null || modBeanTemp.readmePath != null) {
                        var readmeFilename = modBeanTemp.fileReadmePath
                        if (modBeanTemp.readmePath != null) readmeFilename = modBeanTemp.readmePath
                        // 解压readme
                        ArchiveUtil.extractSpecificFile(
                            archiveFile.absolutePath,
                            mutableListOf(readmeFilename!!),
                            MODS_TEMP_PATH
                        )
                        val readmeFile = File(MODS_TEMP_PATH, readmeFilename)
                        mod = readReadmeFile(MODS_TEMP_PATH, mod)
                        readmeFile.delete()
                    }

                    var icon = modBeanTemp.iconPath
                    var images: List<String> = modBeanTemp.images
                    kotlin.runCatching {
                        ArchiveUtil.extractSpecificFile(
                            archiveFile.absolutePath,
                            modBeanTemp.images,
                            MODS_IMAGE_PATH + archiveFile.nameWithoutExtension,

                            )
                        icon =
                            MODS_IMAGE_PATH + archiveFile.nameWithoutExtension + "/" + icon
                        images = modBeanTemp.images.map {
                            MODS_IMAGE_PATH + archiveFile.nameWithoutExtension + "/" + it
                        }
                    }.onFailure {
                        Log.e(TAG, "解压图片失败: $it")
                    }
                    mod = mod.copy(
                        icon = icon,
                        images = images,
                    )
                    list.add(mod)
                } else {
                    list.add(readReadmeFile("", modBean))
                }
            }
        }
        return list

    }


    // 通过流写入mod文件
    suspend fun copyModsByStream(
        path: String,
        gameModPath: String,
        modFiles: List<String>,
        password: String?,

        ): Boolean {
        val checkPermission = PermissionTools.checkPermission(gameModPath)
        Log.d(TAG, "copyModsByStream: $checkPermission")
        when (checkPermission) {
            PathType.SHIZUKU -> {
                return copyModStreamByShizuku(path, gameModPath, modFiles, password)
            }

            PathType.DOCUMENT -> {
                return copyModStreamByDocumentFile(path, gameModPath, modFiles, password)
            }

            PathType.FILE -> {
                return copyModStreamByFile(path, gameModPath, modFiles, password)
            }

            else -> {
                return false
            }
        }

    }

    private fun copyModStreamByFile(
        path: String, gameModPath: String, modFiles: List<String>, password: String?
    ): Boolean {
        val flags: MutableList<Boolean> = ArrayList()
        try {
            modFiles.forEach {
                flags.add(
                    unZipModsByFileHeardByFile(path, gameModPath, it, password)
                )
            }

        } catch (e: RemoteException) {
            flags.add(false)
        }
        if (!flags.all { it }) {
            // 抛出流复制失败异常
            throw CopyStreamFailedException(App.get().getString(R.string.toast_copy_failed))
        }
        return true

    }

    private fun copyModStreamByDocumentFile(
        path: String, gameModPath: String, modFiles: List<String>, password: String?
    ): Boolean {
        val flags: MutableList<Boolean> = ArrayList()
        try {
            modFiles.forEach {
                flags.add(unZipModsByFileHeardByDocument(path, gameModPath, it, password))
            }

        } catch (e: RemoteException) {
            flags.add(false)
        }
        if (!flags.all { it }) {
            // 抛出流复制失败异常
            throw CopyStreamFailedException(App.get().getString(R.string.toast_copy_failed))
        }
        return true

    }

    private fun unZipModsByFileHeardByFile(
        modTempPath: String, gameModPath: String, files: String?, password: String?
    ): Boolean {

        setModsToolsSpecialPathReadType(PathType.FILE)
        val fileHeaders = ArchiveUtil.listInArchiveFiles(modTempPath)
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (fileHeaderObj == files) {
                try {
                    fileTools?.createFileByStream(
                        gameModPath,
                        File(files).name,
                        ArchiveUtil.getArchiveItemInputStream(
                            modTempPath,
                            fileHeaderObj,
                            password
                        ),
                    )
                    flag.add(true)
                } catch (e: Exception) {
                    Log.e("ZipTools", "通过流解压失败: $e")
                    e.printStackTrace()
                    flag.add(false)
                }
            }
        }
        return flag.all { it }
    }

    private fun unZipModsByFileHeardByDocument(
        modTempPath: String, gameModPath: String, files: String?, password: String?
    ): Boolean {
        setModsToolsSpecialPathReadType(PathType.DOCUMENT)
        val fileHeaders = ArchiveUtil.listInArchiveFiles(modTempPath)
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (fileHeaderObj == files) {
                try {
                    fileTools?.createFileByStream(
                        gameModPath,
                        File(files).name,
                        ArchiveUtil.getArchiveItemInputStream(
                            modTempPath,
                            fileHeaderObj,
                            password
                        ),
                    )
                    flag.add(true)
                } catch (e: Exception) {
                    Log.e(TAG, "通过流解压失败: $e")
                    e.printStackTrace()
                    flag.add(false)
                }
            }
        }
        return flag.all { it }
    }

    // 反和谐
    fun antiHarmony(gameInfo: GameInfoBean, b: Boolean): Boolean {
        return try {
            if (b) {
                if (fileTools?.isFileExist(BACKUP_PATH + gameInfo.modSavePath + File(gameInfo.antiHarmonyFile).name) != true) {
                    fileTools?.copyFile(
                        gameInfo.antiHarmonyFile,
                        BACKUP_PATH + gameInfo.packageName + "/" + File(gameInfo.antiHarmonyFile).name
                    )
                }

                fileTools?.writeFile(
                    File(gameInfo.antiHarmonyFile).parent!!,
                    File(gameInfo.antiHarmonyFile).name,
                    gameInfo.antiHarmonyContent
                )
            } else {
                fileTools?.copyFile(
                    BACKUP_PATH + gameInfo.packageName + "/" + File(gameInfo.antiHarmonyFile).name,
                    gameInfo.antiHarmonyFile
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "反和谐失败: $e")
            e.printStackTrace()
            false
        }
    }

    fun getGameFiles(gameInfo: GameInfoBean): List<String> {
        val gameFiles = mutableListOf<String>()
        gameInfo.gameFilePath.forEach {
            val gameFilesNames = fileTools?.getFilesNames(it)
            gameFiles.addAll(gameFilesNames ?: emptyList())
        }
        return gameFiles
    }

    fun makeModsDirs() {
        try {
            File("$ROOT_PATH/$DOWNLOAD_MOD_PATH$GAME_CONFIG").mkdirs()
        } catch (e: Exception) {
            Log.e(TAG, "创建文件夹失败: $e")
            e.printStackTrace()
        }
    }

    //
    suspend fun readGameConfig(path: String): List<GameInfoBean> {
        val gameInfoList = mutableListOf<GameInfoBean>()
        try {
            val listFiles = File(path + GAME_CONFIG).listFiles()
            File(MY_APP_PATH + GAME_CONFIG).mkdirs()
            for (listFile in listFiles!!) {
                if (listFile.name.endsWith(".json")) {
                    try {
                        withContext(Dispatchers.IO) {
                            Log.d(TAG, "文件${listFile.name + listFile.totalSpace}")
                            val fromJson =
                                Gson().fromJson<GameInfoBean>(
                                    listFile.readText(),
                                    GameInfoBean::class.java
                                )

                            //val fromJson = Gson().fromJson(listFile.readText(), GameInfo::class.java)
                            val checkGameInfo = checkGameInfo(fromJson)
                            if (File(MY_APP_PATH + GAME_CONFIG + checkGameInfo.packageName + ".json").exists()) {
                                Files.delete(
                                    Paths.get(MY_APP_PATH + GAME_CONFIG + checkGameInfo.packageName + ".json")
                                )
                            }
                            Files.copy(
                                Paths.get(listFile.absolutePath),
                                Paths.get(MY_APP_PATH + GAME_CONFIG + checkGameInfo.packageName + ".json"),
                                StandardCopyOption.REPLACE_EXISTING
                            )

                        }
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall("已读取 : " + listFile.name)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "读取失败${e.printStackTrace()}")
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(listFile.name + ":" + e.message)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                ToastUtils.longCall(
                    App.get().getString(R.string.toast_load_game_config_err, e.message)
                )
            }
            Log.e(TAG, "读取游戏配置失败: $e")
        }
        return gameInfoList
    }

    fun updateGameConfig() {
        try {
            val listFiles = File(MY_APP_PATH + GAME_CONFIG).listFiles()
            val gameInfoList = mutableListOf<GameInfoBean>()
            for (listFile in listFiles!!) {
                if (listFile.name.endsWith(".json")) {
                    try {
                        val fromJson =
                            Gson().fromJson(listFile.readText(), GameInfoBean::class.java)
                        val checkGameInfo = checkGameInfo(fromJson)
                        gameInfoList.add(checkGameInfo)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, "更新配置文件失败$e")
                    }
                }
            }
            val filter =
                gameInfoList.filter { GameInfoConstant.gameInfoList.none { gameInfo -> gameInfo.packageName == it.packageName } }
            Log.d(TAG, "gameInfoList: $filter")
            GameInfoConstant.gameInfoList.addAll(filter)
        } catch (e: Exception) {
            Log.e(TAG, "读取游戏配置失败: $e")
        }
    }

    // 校验Gson读取的gameInfo的字段
    private fun checkGameInfo(gameInfo: GameInfoBean): GameInfoBean {
        Log.d(TAG, "gameInfo: $gameInfo")
        var result = gameInfo.copy()
        if (gameInfo.gameName.isEmpty()) {
            throw Exception("gameName : 游戏名称不能为空")
        }

        if (gameInfo.packageName.isEmpty()) {
            throw Exception("packageName不能为空")
        } else {
            val pattern = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)+([.][a-zA-Z_][a-zA-Z0-9_]*)+\$\n")
            if (pattern.matches(gameInfo.packageName)) {
                throw Exception("packageName包名不合法")
            }
        }

        if (gameInfo.gamePath.isEmpty()) {
            throw Exception("gamePath : 游戏data根目录不能为空")
        } else {
            result = result.copy(
                gamePath = ROOT_PATH + "/Android/data/" + gameInfo.packageName + "/"
            )

        }
        if (gameInfo.antiHarmonyFile.isNotEmpty()) {
            result = result.copy(
                antiHarmonyFile = (ROOT_PATH + "/" + gameInfo.antiHarmonyFile).replace(
                    "//",
                    "/"
                )
            )
        }

        if (gameInfo.modType.isEmpty()) {
            throw Exception("modType不能为空")
        }
        if (gameInfo.gameFilePath.isEmpty()) {
            throw Exception("gameFilePath不能为空")
        } else {
            val paths = mutableListOf<String>()
            for (path in gameInfo.gameFilePath) {
                paths.add("$ROOT_PATH/$path/".replace("//", "/"))
            }
            result = result.copy(gameFilePath = paths)
        }
        if (gameInfo.serviceName.isEmpty()) {
            throw Exception("serviceName不能为空")
        }
        if (gameInfo.gameFilePath.size != gameInfo.modType.size) {
            throw Exception("gameFilePath和modType的列表必须对应")
        }
        return result
    }

    suspend fun createModsDirectory(gameInfo: GameInfoBean, path: String) {
        withContext(Dispatchers.IO) {
            try {
                File(ROOT_PATH + path + gameInfo.packageName).mkdirs()
            } catch (e: Exception) {
                Log.e(TAG, "创建文件夹失败: $e")
            }
        }
    }

    fun getVersionCode(): Int {
        return try {
            val context = App.get()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            } else {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.longVersionCode.toInt() // Use this for Android versions 9.0 (API level 28) and higher
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }

    fun getVersionName(): String? {
        return try {
            val context = App.get()
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "未知"
        }
    }

    fun writeGameConfigFile(downloadGameConfig: GameInfoBean) {
        try {
            val file =
                File(MY_APP_PATH + GAME_CONFIG + downloadGameConfig.packageName + ".json")
            if (file.exists()) {
                file.delete()
            }
            if (file.parentFile?.exists() == false) {
                file.parentFile?.mkdirs()
            }
            file.createNewFile()
            val gson = GsonBuilder()
                .disableHtmlEscaping()
                .create()
            file.writeText(gson.toJson(downloadGameConfig, GameInfoBean::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "写入游戏配置失败: $e")
        }
    }

    suspend fun specialOperationEnable(modBean: ModBean, packageName: String) {
        var specialOperationFlag = true
        withContext(Dispatchers.IO) {
            SpecialGame.entries.forEach {
                if (packageName.contains(it.packageName)) {
                    specialOperationFlag =
                        it.baseSpecialGameTools.specialOperationEnable(modBean, packageName)

                }
            }
        }
        if (!specialOperationFlag) {
            throw Exception(App.get().getString(R.string.toast_special_operation_failed))
        }
    }

    suspend fun specialOperationDisable(
        backupBeans: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ) {
        var specialOperationFlag = true
        SpecialGame.entries.forEach {
            if (packageName.contains(it.packageName)) {
                specialOperationFlag = it.baseSpecialGameTools.specialOperationDisable(
                    backupBeans,
                    packageName,
                    modBean
                )
            }
        }
        if (!specialOperationFlag) {
            throw Exception(App.get().getString(R.string.toast_special_operation_failed))
        }
    }

    fun specialOperationScanMods(packageName: String, modFileName: String): Boolean {
        for (specialGame in SpecialGame.entries) {
            if (packageName.contains(specialGame.packageName)) {
                return specialGame.baseSpecialGameTools.specialOperationScanMods(
                    packageName,
                    modFileName
                )
            }
        }
        return false
    }

    // 判断是否为待扫描的文件
    private fun isScanFile(documentFile: DocumentFile): Boolean {
        setModsToolsSpecialPathReadType(PathType.DOCUMENT)
        if (documentFile.isDirectory) return false
        if (documentFile.name?.let { fileTools?.isExcludeFileType(it) } == true) return false
        return true
    }

    fun getNewMods(mods: List<ModBean>, modsScan: MutableList<ModBean>): List<ModBean> {
        val newMods = mutableListOf<ModBean>()
        for (modBean in modsScan) {
            modBean.isNew(mods)?.let { newMods.add(it) }
        }
        return newMods
    }

    fun getInfoVersion(): Double {
        val file = File(MY_APP_PATH + "informationVersion")
        return if (file.exists()) {
            file.readText().toDouble()
        } else {
            0.0
        }
    }

    fun setInfoVersion(version: Double) {
        val file = File(MY_APP_PATH + "informationVersion")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file.writeText(version.toString())
    }
}
