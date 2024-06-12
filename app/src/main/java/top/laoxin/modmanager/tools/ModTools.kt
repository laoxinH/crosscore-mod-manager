package top.laoxin.modmanager.tools

import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.RemoteException
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.tools.ZipTools.getFileName
import top.laoxin.modmanager.tools.ZipTools.getFirstImageFromZip
import top.laoxin.modmanager.tools.ZipTools.getImagesFromZip
import top.laoxin.modmanager.tools.fileToolsInterface.BaseFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.DocumentFileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.FileTools
import top.laoxin.modmanager.tools.fileToolsInterface.impl.ShizukuFileTools
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel
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
    private val BACKUP_PATH = MY_APP_PATH + "backup/"
    private val MODS_TEMP_PATH = MY_APP_PATH + "temp/"
    val MODS_UNZIP_PATH = MY_APP_PATH + "temp/unzip/"
    private val MODS_ICON_PATH = MY_APP_PATH + "icon/"
    private val MODS_IMAGE_PATH = MY_APP_PATH + "images/"
    val GAME_CONFIG = "GameConfig/"
    val QQ_DOWNLOAD_PATH =
        (ROOT_PATH + "/Android/data/" + (App.get().packageName ?: "")).toString() + "/"
    const val DOWNLOAD_MOD_PATH = "/Download/Mods/"
    var specialPathReadType: Int = PathType.DOCUMENT
    var iFileExplorerService: IFileExplorerService? = null
    private val PACKAGE_MANAGER: PackageManager =
        App.get().getPackageManager() ?: throw NullPointerException()
    private const val TAG = "ModTools"
    private var fileTools: BaseFileTools? = null

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


    fun isDataPath(path: String): Boolean {
        return (("$ROOT_PATH/Android/data") == path)
    }

    fun isObbPath(path: String): Boolean {
        return (("$ROOT_PATH/Android/obb") == path)
    }

    private fun isUnderDataPath(path: String): Boolean {
        return path.contains("$ROOT_PATH/Android/data/")
    }

    private fun isUnderObbPath(path: String): Boolean {
        return path.contains("$ROOT_PATH/Android/obb/")
    }

    /**
     * 如果字符串是应用包名，返回字符串，反之返回null
     */
    fun getPathPackageName(name: String?): String? {
        try {
            PACKAGE_MANAGER.getPackageInfo((name)!!, 0)
            return name
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }


    suspend fun backupGameFiles(
        gameModPath: String,
        modBean: ModBean,
        gameBackupPath: String,
    ): List<BackupBean> {
        val list: MutableList<BackupBean> = ArrayList<BackupBean>()
        // 通过ZipTools解压文件到modTempPath
        val checkPermission = PermissionTools.checkPermission(gameModPath)
        setModsToolsSpecialPathReadType(checkPermission)
        Log.d(TAG, "游戏mod路径: ${modBean.modFiles}")
        try {
            modBean.modFiles?.forEach {
                val file = File(it)
                val backupPath =
                    BACKUP_PATH + gameBackupPath + File(modBean.gameModPath!!).name + "/" + file.name
                val gamePath = gameModPath + file.name
                if (File(backupPath).exists()) {
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
                } else {
                    if (if (specialPathReadType == PathType.DOCUMENT) {
                            fileTools?.copyFileByDF(
                                gamePath, backupPath
                            ) ?: false
                        } else {
                            fileTools?.copyFile(
                                gamePath, backupPath
                            ) ?: false
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
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("FileTools", "备份mod文件失败: ${e.printStackTrace()}")
            e.printStackTrace()
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
        for (modFilePath in modBean.modFiles ?: emptyList()) {
            try {
                val modFile = File(unZipPath + modFilePath)
                val gameFile = File(gameModPath + modFile.name)
                flags.add(
                    if (specialPathReadType == PathType.DOCUMENT) {
                        fileTools?.copyFileByFD(
                            modFile.absolutePath, gameFile.absolutePath
                        ) ?: false
                    } else {
                        fileTools?.copyFile(
                            modFile.absolutePath, gameFile.absolutePath
                        ) ?: false
                    }

                )

            } catch (e: AccessDeniedException) {
                Log.e(TAG, "复制mod文件失败开始删除: $e")
                fileTools?.deleteFile(unZipPath)


            } catch (e: IOException) {
                Log.e(TAG, "复制mod文件失败: $e")
                flags.add(false)
            }
        }
        return flags.all {
            return it
        }
    }

    suspend fun restoreGameFiles(backups: List<BackupBean?>): Boolean {
        val checkPermission = PermissionTools.checkPermission(backups[0]?.gameFilePath ?: "")
        setModsToolsSpecialPathReadType(checkPermission)
        val flags = mutableListOf<Boolean>()
        for (backup in backups) {
            if (backup != null) {
                flags.add(
                    fileTools?.copyFile(
                        backup.backupPath!!, backup.gameFilePath!!
                    ) ?: false
                )
            }
        }
        return flags.all {
            return it
        }
    }

    suspend fun deleteTempFile(): Boolean {
        return if (File(MODS_TEMP_PATH).exists()) {
            setModsToolsSpecialPathReadType(PathType.FILE)
            fileTools?.deleteFile(MODS_TEMP_PATH) ?: false
        } else {
            true
        }
    }

    suspend fun deleteBackupFiles(
        gameInfo: GameInfo
    ): Boolean {

        return if (File(BACKUP_PATH + gameInfo.packageName).exists()) {
            setModsToolsSpecialPathReadType(PathType.FILE)
            kotlin.runCatching { fileTools?.deleteFile(BACKUP_PATH + gameInfo.packageName) }.isSuccess
        } else {
            true
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
        // 读取readme
        var bean = readReadmeFile(unZipPath, modBean)
        bean = bean.copy(description = "MOD已解密")
        // 读取icon文件输入流
        if (bean.icon != null) {
            val iconPath = unZipPath + bean.icon
            val file = File(iconPath)
            val md5 = ZipTools.calculateMD5(file.inputStream())
            fileTools?.copyFile(iconPath, MODS_ICON_PATH + md5 + file.name)
            bean = bean.copy(icon = MODS_ICON_PATH + md5 + file.name)
        }
        // 读取images文件输入流
        if (bean.images != null) {
            val images = mutableListOf<String>()
            for (image in bean.images!!) {
                val imagePath = unZipPath + image
                val file = File(imagePath)
                val md5 = ZipTools.calculateMD5(file.inputStream())
                fileTools?.copyFile(imagePath, MODS_IMAGE_PATH + md5 + file.name)
                images.add(MODS_IMAGE_PATH + md5 + file.name)
            }
            bean = bean.copy(images = images)
        }
        return bean
    }


    // 读取readme文件
    suspend fun readReadmeFile(unZipPath: String, modBean: ModBean): ModBean {
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
    suspend fun scanModsByShizuku(
        scanPath: String, gameInfo: GameInfo
    ): Boolean {
        return try {
            iFileExplorerService?.scanMods(scanPath, gameInfo) ?: false
        } catch (e: NullPointerException) {
            ToastUtils.longCall(R.string.toast_shizuku_load_file_failed)
            e.printStackTrace()
            false
        } catch (e: RemoteException) {
            ToastUtils.longCall(R.string.toast_shizuku_load_file_failed)
            e.printStackTrace()
            false
        }
    }

    // 通过DocumentFile扫描mods
    suspend fun scanMods(
        scanPath: String,
        gameInfo: GameInfo,
    ): Boolean {
        Log.d(TAG, "gameInfo: $gameInfo")
        Log.d(TAG, "scanPath: $scanPath")
        val checkPermission = PermissionTools.checkPermission(scanPath)
        setModsToolsSpecialPathReadType(checkPermission)
        if (specialPathReadType == PathType.SHIZUKU) {
            return scanModsByShizuku(scanPath, gameInfo)
        }
        setModsToolsSpecialPathReadType(PermissionTools.checkPermission(gameInfo.gamePath))
        val gameFiles = mutableListOf<String>()
        gameInfo.gameFilePath.forEach {
            val gameFilesNames = fileTools?.getFilesNames(it)
            gameFiles.addAll(gameFilesNames ?: emptyList())
        }
        var tempScanPath = scanPath
        val tempPathType = specialPathReadType
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
                    if (!(documentFile.isDirectory || (fileTools?.isFileType(documentFile.name!!) == true))) {
                        fileTools?.copyFileByDF(ScanModPath.MOD_PATH_QQ + documentFile.name!!, tempScanPath + documentFile.name!!)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "扫描文件失败: $e")
                }
            }
        }
        setModsToolsSpecialPathReadType(PathType.FILE)
        File(tempScanPath).listFiles()?.forEach { file ->
            setModsToolsSpecialPathReadType(PathType.FILE)
            if (!(file.isDirectory || (fileTools?.isFileType(file.name) == true))) {
                val zipFile = ZipFile(file)
                if (zipFile.isValidZipFile) {
                    zipFile.fileHeaders.forEach zipFile@{
                        val modFileName = File(ZipTools.getFileName(it)).name
                        if (gameFiles.contains(modFileName)) {
                            Log.d(TAG, "开始移动文件: ${gameInfo.modSavePath + file.name}")
                            fileTools?.moveFile(file.absolutePath, gameInfo.modSavePath + file.name)
                            return@zipFile
                        }
                    }
                }
            }
        }
        fileTools?.deleteFile(MODS_TEMP_PATH + "Mods/")
        return true
    }


    fun createModTempMap(
        zipFile: ZipFile?, scanPath: String, files: List<MutableList<String>>, gameInfo: GameInfo
    ): MutableMap<String, ModBeanTemp> {
        val checkPermission = PermissionTools.checkPermission(gameInfo.gamePath)
        setModsToolsSpecialPathReadType(checkPermission)
        // 创建一个ModBeanTemp的map
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()
        for (file in files) {
            val modFileName = File(file[1]).name
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
                            "${pathName}----${(File(file[1]).parentFile?.name ?: "0")}"
                        )
                        if (pathName == (File(file[1]).parentFile?.name ?: "")) {
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
                Log.d("ZipTools", "gameFileMap: ${fileTools?.isFileExist(it.value)}")
                Log.d("ZipTools", "gameFileMap: ${fileTools?.isFile(it.value)}")

                if (fileTools?.isFileExist(it.value) == true && fileTools?.isFile(it.value) == true) {
                    val modEntries = File(file[1].replace(scanPath, ""))
                    val key = modEntries.parent ?: zipFile?.file?.name ?: modEntries.name
                    val modBeanTemp = modBeanTempMap[key]
                    if (modBeanTemp == null) {
                        val beanTemp = ModBeanTemp(
                            name = zipFile?.file?.nameWithoutExtension + if (modEntries.parentFile == null) {
                                if (zipFile == null) modEntries.name else ""
                            } else "(" + modEntries.parentFile!!.path.replace(
                                "/", "|"
                            ) + ")", // 如果是根目录则不加括号
                            iconPath = null,
                            readmePath = null,
                            modFiles = mutableListOf(file[0]),
                            images = mutableListOf(),
                            fileReadmePath = null,
                            isEncrypted = if (zipFile == null) false else ZipTools.isEncrypted(
                                zipFile
                            ),
                            gamePackageName = gameInfo.packageName,
                            modType = it.key,
                            gameModPath = gameInfo.gameFilePath[gameInfo.modType.indexOf(it.key)],
                            isZip = if (zipFile == null) false else ZipTools.isZipFile(zipFile),
                            modPath = if (zipFile == null) File(file[1]).parent
                                ?: file[1] else zipFile.file.absolutePath
                        )
                        modBeanTempMap[key] = beanTemp
                    } else {
                        modBeanTemp.modFiles.add(file[0])
                    }
                }

            }
        }/*File(gameFileModPath,modName)*/
        Log.d(TAG, "modBeanTempMap: $modBeanTempMap")
        // 判断是否存在readme.txt文件
        for (file in files) {
            if (file[1].substringAfterLast("/").equals("readme.txt", ignoreCase = true)) {
                val modEntries = File(file[1].replace(scanPath, ""))
                val key = modEntries.parent ?: zipFile?.file?.name ?: modEntries.name
                Log.d("ZipTools", "readme文件路径: $file")
                Log.d("ZipTools", "readme文件key: $key")
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp != null) {
                    modBeanTemp.readmePath = file[1]
                }
            } else if (file[1].contains(".jpg", ignoreCase = true) || file[1].contains(
                    ".png",
                    ignoreCase = true
                ) || file[1].contains(".gif", ignoreCase = true) || file[1].contains(
                    ".jpeg",
                    ignoreCase = true
                )
            ) {
                val modEntries = File(file[1].replace(scanPath, ""))
                val key = modEntries.parent ?: zipFile?.file?.name ?: modEntries.name
                val modBeanTemp = modBeanTempMap[key]
                modBeanTemp?.images?.add(file[1])
                modBeanTemp?.iconPath = file[1]
            } else if (file[1].equals("readme.txt", ignoreCase = true)) {
                modBeanTempMap.forEach {
                    val modBeanTemp = it.value
                    modBeanTemp.fileReadmePath = file[1]
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
                    iFileExplorerService?.unZipFile(modTempPath, gameModPath, it, password) ?: false
                )
            }

        } catch (e: RemoteException) {
            flags.add(false)
        }
        return flags.all { it }
    }


    // 修改文件权限
    fun chmod(path: String?): Boolean {
        return try {
            iFileExplorerService?.chmod(path) ?: false
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }


    suspend fun createMods(
        scanPath: String,
        gameInfo: GameInfo,
    ): MutableList<ModBean> {
        val checkPermission = PermissionTools.checkPermission(scanPath)
        setModsToolsSpecialPathReadType(checkPermission)
        // 读取游戏文件信息
        //  val gameFilesMap = mutableMapOf<String, List<String>>()
        /*   Log.d(TAG, "scanPath: ${scanPath}")
           gameInfo.gameFilePath.forEachIndexed { index, it ->
               val gameFilesNames = fileTools?.getFilesNames(it)
               gameFilesMap[gameInfo.modType[index]] = gameFilesNames ?: emptyList()
           }
           Log.d(TAG, "游戏文件: ${gameFilesMap}")
   */
        val modsList = mutableListOf<ModBean>()
        File(scanPath).listFiles()?.forEach { file ->
            if (!(file.isDirectory || (fileTools?.isFileType(file.name) == true))) {
                val zipFile = ZipTools.createZipFile(file)
                if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                    val modTempMap = createModTempMap(zipFile,
                        scanPath,
                        zipFile.fileHeaders.map { mutableListOf(it.fileName, getFileName(it)) }
                            .toMutableList(),
                        gameInfo)
                    Log.d(TAG, "modTempMap: $modTempMap")
                    val mods: List<ModBean> = readModBeans(
                        zipFile,
                        scanPath,
                        modTempMap,
                        MODS_IMAGE_PATH,
                    )
                    modsList.addAll(mods)
                }
            }
        }
        Log.d(TAG, "modsList: $modsList")
        return modsList

    }

    suspend fun readModBeans(
        zipFile: ZipFile?,
        scanPath: String,
        modTempMap: MutableMap<String, ModBeanTemp>,
        imagesPath: String,
    ): List<ModBean> {
        if (modTempMap.isEmpty()) {
            return emptyList()
        }

        val list = mutableListOf<ModBean>()
        for (entry in modTempMap.entries) {
            val modBeanTemp = entry.value
            if (modBeanTemp.isEncrypted) {
                val modBean = ModBean(
                    id = 0,
                    name = modBeanTemp.name,
                    version = "已加密",
                    description = "MOD文件已加密, 无法读取详细信息",
                    author = "未知",
                    date = zipFile?.file?.lastModified() ?: Date().time,
                    path = modBeanTemp.modPath,
                    icon = modBeanTemp.iconPath,
                    images = modBeanTemp.images,
                    modFiles = modBeanTemp.modFiles,
                    isEncrypted = modBeanTemp.isEncrypted,
                    password = null,
                    readmePath = modBeanTemp.readmePath,
                    fileReadmePath = modBeanTemp.fileReadmePath,
                    isEnable = false,
                    gamePackageName = modBeanTemp.gamePackageName,
                    gameModPath = modBeanTemp.gameModPath,
                    modType = modBeanTemp.modType,
                    isZipFile = modBeanTemp.isZip
                )
                list.add(modBean)
            } else {
                if (modBeanTemp.readmePath != null) {
                    if (zipFile != null) {
                        val modBean = ZipTools.readModBean(zipFile, modBeanTemp, imagesPath)
                        if (modBean != null) {
                            list.add(modBean)
                        }
                    } else {
                        val modBean = readReadmeFile(
                            scanPath, ModBean(
                                id = 0,
                                name = modBeanTemp.name,
                                version = "未适配",
                                description = "不存在readme描述文件,无法读取详细信息",
                                author = "未知",
                                date = Date().time,
                                path = modBeanTemp.modPath,
                                icon = modBeanTemp.iconPath,
                                images = modBeanTemp.images,
                                modFiles = modBeanTemp.modFiles,
                                isEncrypted = modBeanTemp.isEncrypted,
                                password = null,
                                readmePath = modBeanTemp.readmePath,
                                fileReadmePath = modBeanTemp.fileReadmePath,
                                isEnable = false,
                                gamePackageName = modBeanTemp.gamePackageName,
                                gameModPath = modBeanTemp.gameModPath,
                                modType = modBeanTemp.modType,
                                isZipFile = modBeanTemp.isZip
                            )
                        )
                        list.add(modBean)

                    }

                } else {
                    val modBean = ModBean(
                        id = 0,
                        name = modBeanTemp.name,
                        version = "未适配",
                        description = "不存在readme描述文件,无法读取详细信息",
                        author = "未知",
                        date = zipFile?.file?.lastModified() ?: Date().time,
                        path = modBeanTemp.modPath,
                        icon = if (zipFile == null) modBeanTemp.iconPath else getFirstImageFromZip(
                            zipFile, imagesPath, modBeanTemp.iconPath
                        ),
                        images = if (zipFile == null) modBeanTemp.images else getImagesFromZip(
                            zipFile, imagesPath, modBeanTemp.images
                        ),
                        modFiles = modBeanTemp.modFiles,
                        isEncrypted = modBeanTemp.isEncrypted,
                        password = null,
                        readmePath = modBeanTemp.readmePath,
                        fileReadmePath = modBeanTemp.fileReadmePath,
                        isEnable = false,
                        gamePackageName = modBeanTemp.gamePackageName,
                        gameModPath = modBeanTemp.gameModPath,
                        modType = modBeanTemp.modType,
                        isZipFile = modBeanTemp.isZip
                    )
                    list.add(modBean)
                }
            }

        }
        return list

    }


    // 通过流写入mod文件
    suspend fun copyModsByStream(
        path: String, gameModPath: String, modFiles: List<String>, password: String?, pathType: Int
    ): Boolean {
        val checkPermission = PermissionTools.checkPermission(gameModPath)
        setModsToolsSpecialPathReadType(checkPermission)
        when (pathType) {
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
        return flags.all { it }

    }

    private fun copyModStreamByDocumentFile(
        path: String, gameModPath: String, modFiles: List<String>, password: String?
    ): Boolean {
        val flags: MutableList<Boolean> = ArrayList()
        try {
            modFiles.forEach {
                flags.add(
                    unZipModsByFileHeardByDocument(path, gameModPath, it, password)
                )
            }

        } catch (e: RemoteException) {
            flags.add(false)
        }
        return flags.all { it }

    }

    fun unZipModsByFileHeardByFile(
        modTempPath: String, gameModPath: String, files: String?, password: String?
    ): Boolean {
        val zipFile = ZipFile(modTempPath)
        if (password != null) {
            zipFile.setPassword(password.toCharArray())
        }
        val fileHeaders = zipFile.fileHeaders
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (ZipTools.getFileName(fileHeaderObj) == files) {
                try {
                    ZipTools.createFile(
                        gameModPath,
                        File(fileHeaderObj.fileName).name,
                        zipFile.getInputStream(fileHeaderObj)
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

    fun unZipModsByFileHeardByDocument(
        modTempPath: String, gameModPath: String, files: String?, password: String?
    ): Boolean {
        val zipFile = ZipFile(modTempPath)
        if (password != null) {
            zipFile.setPassword(password.toCharArray())
        }
        val fileHeaders = zipFile.fileHeaders
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (fileHeaderObj.fileName == files) {
                try {
                    var gameFile = DocumentFile.fromTreeUri(
                        App.get(), fileTools?.pathToUri(gameModPath + File(files).name)!!
                    )
                    if (gameFile?.exists() == true) {
                        gameFile.delete()
                        gameFile = DocumentFile.fromTreeUri(
                            App.get(), fileTools?.pathToUri(gameModPath)!!
                        )?.createFile(
                            "application/octet-stream", File(files).name
                        )
                    }

                    val fileOutputStream =
                        App.get().contentResolver.openOutputStream(gameFile?.uri!!)
                    val inputStream = zipFile.getInputStream(fileHeaderObj)
                    inputStream.use { input ->
                        fileOutputStream?.use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                    }
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
    fun antiHarmony(gameInfo: GameInfo, b: Boolean): Boolean {
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

    suspend fun scanDirectoryMods(scanPath: String, gameInfo: GameInfo): List<ModBean> {
        val checkPermission = PermissionTools.checkPermission(gameInfo.gamePath)
        setModsToolsSpecialPathReadType(checkPermission)
        // 遍历scanPath及其子目录中的文件
        try {
            val files = mutableListOf<String>()
            Files.walk(Paths.get(scanPath)).sorted(Comparator.reverseOrder()).forEach {
                files.add(it.toString())
            }
            val filesList = files.map {
                mutableListOf(it, it)
            }
            Log.d(TAG, "所有文件路径: $files")
            val createModTempMap = createModTempMap(null, scanPath, filesList, gameInfo)
            return readModBeans(null, scanPath, createModTempMap, MODS_IMAGE_PATH)
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            return emptyList()
        }
    }

    fun getGameFiles(gameInfo: GameInfo): List<String> {
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
    suspend fun readGameConfig(path: String): List<GameInfo> {
        val gameInfoList = mutableListOf<GameInfo>()
        try {
            val listFiles = File(path + GAME_CONFIG).listFiles()
            File(MY_APP_PATH + GAME_CONFIG).mkdirs()


            for (listFile in listFiles!!) {

                if (listFile.name.endsWith(".json")) {
                    try {

                        withContext(Dispatchers.IO) {
                            Log.d(TAG, "文件${listFile.name + listFile.totalSpace}")
                            val fromJson =
                                Gson().fromJson<GameInfo>(listFile.readText(), GameInfo::class.java)

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
                        Log.e(TAG, "$e")
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
            val gameInfoList = mutableListOf<GameInfo>()
            for (listFile in listFiles!!) {
                if (listFile.name.endsWith(".json")) {
                    try {
                        val fromJson = Gson().fromJson(listFile.readText(), GameInfo::class.java)
                        val checkGameInfo = checkGameInfo(fromJson)
                        gameInfoList.add(checkGameInfo)
                        for (gameInfo in GameInfoConstant.gameInfoList) {
                            if (gameInfo.gameName == checkGameInfo.gameName) {
                                Log.d(TAG, "移除 : $gameInfo")
                                val index = GameInfoConstant.gameInfoList.indexOf(gameInfo)
                                GameInfoConstant.gameInfoList.removeAt(index)
                                GameInfoConstant.gameInfoList.add(index, checkGameInfo)
                                break
                            }
                        }
                        for (gameInfo in gameInfoList) {
                            if (gameInfo.gameName == checkGameInfo.gameName) {
                                gameInfoList.remove(gameInfo)
                                gameInfoList.add(checkGameInfo)
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "$e")
                    }
                }
            }
            val filter =
                gameInfoList.filter { GameInfoConstant.gameInfoList.none { gameInfo -> gameInfo.packageName == it.packageName } }
            GameInfoConstant.gameInfoList.addAll(filter)
        } catch (e: Exception) {
            Log.e(TAG, "读取游戏配置失败: $e")
        }
    }

    // 校验Gson读取的gameInfo的字段
    fun checkGameInfo(gameInfo: GameInfo): GameInfo {
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
                antiHarmonyFile = (ROOT_PATH + "/" + gameInfo.antiHarmonyFile).replace("//", "/")
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

    suspend fun createModsDirectory(gameInfo: GameInfo, path: String) {
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

    fun getVersionName(): String {
        return try {
            val context = App.get()
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "未知"
        }
    }

}
