package top.laoxin.modmanager.tools

import android.content.pm.PackageManager
import android.os.Environment
import android.os.RemoteException
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.BeanFile
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.bean.ModBeanTemp
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.userservice.FileExplorerService
import top.laoxin.modmanager.useservice.IFileExplorerService
import java.io.File
import java.io.IOException
import kotlin.math.max

object ModTools {
    val ROOT_PATH: String = Environment.getExternalStorageDirectory().path
    val MY_APP_PATH =
        (ROOT_PATH + "/Android/data/" + (App.get().packageName ?: "")).toString() + "/"
    private val BACKUP_PATH = MY_APP_PATH + "backup/"
    private val MODS_TEMP_PATH = MY_APP_PATH + "temp/"
    val MODS_UNZIP_PATH = MY_APP_PATH + "temp/unzip/"
    private val MODS_ICON_PATH = MY_APP_PATH + "icon/"
    private val MODS_IMAGE_PATH = MY_APP_PATH + "images/"
    val QQ_DOWNLOAD_PATH =
        (ROOT_PATH + "/Android/data/" + (App.get().packageName ?: "")).toString() + "/"
    val DOWNLOAD_MOD_PATH_JC = "/Download/Mods/crosscore/"
    var specialPathReadType: Int = PathType.DOCUMENT
    var iFileExplorerService: IFileExplorerService? = null
    private val PACKAGE_MANAGER: PackageManager =
        App.get().getPackageManager() ?: throw NullPointerException()
    private val COMPARATOR: Comparator<BeanFile> = object : Comparator<BeanFile> {
        override fun compare(o1: BeanFile, o2: BeanFile): Int {
            val name1: String? = o1.name
            val name2: String? = o2.name
            if (name1 != null && name2 != null) {
                val compareCount =
                    max(name1.length.toDouble(), name2.length.toDouble()).toInt()
                for (i in 0 until compareCount) {
                    val code1 = getCharCode(name1, i)
                    val code2 = getCharCode(name2, i)
                    if (code1 != code2) {
                        return code1 - code2
                    }
                }
            }
            return 0
        }

        private fun getCharCode(str: String, index: Int): Int {
            if (index >= str.length) {
                return -1
            }
            val c = str[index]
            return if (Character.isLetter(c)) {
                if (Character.isLowerCase(c)) {
                    c.uppercaseChar().code
                } else {
                    c.code
                }
            } else {
                -1
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
        pathType: Int,
    ): List<BackupBean> {
        val list: MutableList<BackupBean> = ArrayList<BackupBean>()
        // 通过ZipTools解压文件到modTempPath

        Log.d("FileTools", "游戏mod路径: ${modBean.modFiles}")
        try {
            modBean.modFiles?.forEach {
                val file = File(it)
                val backupPath = BACKUP_PATH + file.name
                val gamePath = gameModPath + file.name
                if (File(backupPath).exists()) {
                    list.add(BackupBean(0, file.name, gamePath, backupPath, modBean.name))
                } else {
                    when (pathType) {
                        PathType.FILE -> {
                            if (FileTools.copyFileByFile(gameModPath + file.name, backupPath)) {
                                list.add(
                                    BackupBean(
                                        0,
                                        file.name,
                                        gamePath,
                                        backupPath,
                                        modBean.name
                                    )
                                )
                            }
                        }

                        PathType.DOCUMENT -> {
                            if (FileTools.copyFileByDocument(gameModPath + file.name, backupPath)) {
                                list.add(
                                    BackupBean(
                                        0,
                                        file.name,
                                        gamePath,
                                        backupPath,
                                        modBean.name
                                    )
                                )
                            }
                        }

                        PathType.SHIZUKU -> {
                            if (FileTools.copyFileByShizuku(gameModPath + file.name, backupPath)) {
                                list.add(
                                    BackupBean(
                                        0,
                                        file.name,
                                        gamePath,
                                        backupPath,
                                        modBean.name
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("FileTools", "备份mod文件失败: ${e.printStackTrace()}")
            e.printStackTrace()
        }

        return list
    }

    private fun fileIsExistByShuzuku(paht: String): Boolean {
        try {
            return iFileExplorerService?.fileExists(paht) ?: false
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return false

    }


    suspend fun copyModFileToTemp(modBean: ModBean): String? {
        val path = MODS_TEMP_PATH /*+ Date().time*/ + File(modBean.path!!).name
        val s = MODS_UNZIP_PATH + File(modBean.path!!).name
        Log.d("FileTools", "解压文件路径: $s ${File(s).exists()}")
        if (File(MODS_UNZIP_PATH + File(modBean.path).name).exists()) return path

        return if (FileTools.copyFileByShizuku(modBean.path, path)) {
            path
        } else {
            null
        }
    }

    suspend fun copyModFiles(
        modBean: ModBean,
        gameModPath: String,
        unZipPath: String,
        pathType: Int
    ): Boolean {

        val flags = mutableListOf<Boolean>()
        for (modFilePath in modBean.modFiles ?: emptyList()) {
            try {
                val modFile = File(unZipPath + modFilePath)
                val gameFile = File(gameModPath + modFile.name)
                when (pathType) {
                    PathType.FILE -> {
                        flags.add(
                            FileTools.copyFileByFile(
                                modFile.absolutePath,
                                gameFile.absolutePath
                            )
                        )

                    }

                    PathType.DOCUMENT -> {
                        flags.add(
                            FileTools.copyFileByDocument(
                                modFile.absolutePath,
                                gameFile.absolutePath
                            )
                        )
                    }

                    PathType.SHIZUKU -> {

                        flags.add(
                            FileTools.copyFileByShizuku(
                                modFile.absolutePath,
                                gameFile.absolutePath
                            )
                        )
                    }
                }
                flags.add(true)


            } catch (e: AccessDeniedException) {
                Log.e("FileTools", "复制mod文件失败开始删除: $e")
                FileTools.deleteFileByFile(unZipPath)


            } catch (e: IOException) {
                Log.e("FileTools", "复制mod文件失败: $e")
                flags[modBean.modFiles?.indexOf(modFilePath) ?: 0] = false
            }
        }
        return flags.all {
            return it
        }
    }

    suspend fun restoreGameFiles(backups: List<BackupBean?>, pathType: Int): Boolean {
        val flags = mutableListOf<Boolean>()
        for (backup in backups) {
            if (backup != null) {
                when (pathType) {
                    PathType.FILE -> {
                        flags.add(
                            FileTools.copyFileByFile(
                                backup.backupPath!!,
                                backup.gamePath!!
                            )
                        )
                    }

                    PathType.DOCUMENT -> {
                        flags.add(
                            FileTools.copyFileByDocument(
                                backup.backupPath!!,
                                backup.gamePath!!
                            )
                        )
                    }

                    PathType.SHIZUKU -> {
                        flags.add(
                            FileTools.copyFileByShizuku(
                                backup.backupPath,
                                backup.gamePath
                            )
                        )
                    }
                }
            }
        }
        return flags.all {
            return it
        }
    }

    suspend fun deleteTempFile(): Boolean {
        return if (File(MODS_TEMP_PATH).exists()) {
            FileTools.deleteFileByFile(MODS_TEMP_PATH)
        } else {
            true
        }
    }

    suspend fun deleteBackupFiles(): Boolean {

        return if (File(BACKUP_PATH).exists()) {
            FileTools.deleteFileByFile(BACKUP_PATH)
        } else {
            true
        }


    }

    // 删除缓存文件
    suspend fun deleteCache(): Boolean {

        return if (File(MODS_IMAGE_PATH).exists()) {
            FileTools.deleteFileByFile(MODS_ICON_PATH)
            FileTools.deleteFileByFile(MODS_IMAGE_PATH)
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
            FileTools.copyFileByFile(iconPath, MODS_ICON_PATH + md5 + file.name)
            bean = bean.copy(icon = MODS_ICON_PATH + md5 + file.name)
        }
        // 读取images文件输入流
        if (bean.images != null) {
            val images = mutableListOf<String>()
            for (image in bean.images!!) {
                val imagePath = unZipPath + image
                val file = File(imagePath)
                val md5 = ZipTools.calculateMD5(file.inputStream())
                FileTools.copyFileByFile(imagePath, MODS_IMAGE_PATH + md5 + file.name)
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
        scanPath: String,
        gameModPath: String,
        downloadModPath: String
    ): Boolean {
        return try {
            iFileExplorerService?.scanMods(scanPath, MY_APP_PATH, gameModPath, downloadModPath)
                ?: false
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
    suspend fun scanModsByDocumentFile(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String
    ): Boolean {

        val pathUri = FileTools.pathToUri(scanPath)
        Log.d("FileTools", "文件Uri: $pathUri")
        val scanModsFile = DocumentFile.fromTreeUri(App.get(), pathUri)
        val gameFile = DocumentFile.fromTreeUri(App.get(), FileTools.pathToUri(gameModPath))
        val gameFileName = gameFile?.listFiles()?.map { it.name }
        Log.d("FileTools", "游戏文件: $gameFileName")

        if (scanModsFile != null) {
            val documentFiles = scanModsFile.listFiles()
            for (df in documentFiles) {
                if (df.isDirectory) continue
                val fileInputStream = FileTools.app.contentResolver.openInputStream(df?.uri!!)
                val file = File(downloadModPath, df.name!!)
                if (file.parentFile?.exists() != true) file.parentFile?.mkdirs()
                Log.d("FileTools", "文件名称df: ${df.name}")

                if (!file.exists()) withContext(Dispatchers.IO) {
                    file.createNewFile()
                    val outputStream = file.outputStream()
                    fileInputStream?.copyTo(outputStream)
                    fileInputStream?.close()
                    outputStream.close()
                }

                val fileHeaders = ZipFile(file).fileHeaders
                if (!fileHeaders.any { File(it.fileName).name in (gameFileName ?: emptyList()) }) {
                    file.delete()
                }
            }
        }
        return true
    }

    // 通过file扫描mods
    suspend fun scanModsByFile(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String
    ): Boolean {
        val files = File(scanPath).listFiles()
        if (files != null) {
            // 判断file是否为压缩文件
            for (f in files) {
                if (f.isDirectory) continue
                val zipFile = ZipTools.createZipFile(f)
                if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                    // 读取zip文件中的mod文件
                    val modTempMap = createModTempMapByFile(zipFile, gameModPath)
                    if (modTempMap.isNotEmpty()) FileTools.moveFileByFile(
                        zipFile.file.path,
                        downloadModPath + zipFile.file.name
                    )
                }
            }
        }
        return true
    }

    // 通过file扫描mods
    suspend fun scanDownload(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String,
        pathType: Int
    ): Boolean {
        val files = File(scanPath).listFiles()
        if (files != null) {
            // 判断file是否为压缩文件
            for (f in files) {
                if (f.isDirectory) continue
                val zipFile = ZipTools.createZipFile(f)
                if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                    // 读取zip文件中的mod文件
                    val modTempMap = when (pathType) {
                        PathType.FILE -> createModTempMapByFile(zipFile, gameModPath)
                        PathType.DOCUMENT -> createModTempMapByDocumentFile(zipFile, gameModPath)
                        else -> mutableMapOf()
                    }
                    if (modTempMap.isNotEmpty()) FileTools.moveFileByFile(
                        zipFile.file.path,
                        downloadModPath + zipFile.file.name
                    )
                }
            }
        }
        return true
    }

    // 通过File扫描mods
    suspend fun createModsByFile(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String,
        pathType: Int
    ): MutableList<ModBean> {
        val list: MutableList<ModBean> = mutableListOf()
        val files = File(scanPath).listFiles()
        if (files != null) {
            // 判断file是否为压缩文件
            for (f in files) {
                Log.d("FileTools", "文件: ${f.name}")
                if (f.isDirectory) continue
                val zipFile = ZipTools.createZipFile(f)
                if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                    // 读取zip文件中的mod文件
                    val modTempMap = createModTempMapByFile(zipFile, gameModPath)
                    val mods: List<ModBean> =
                        ZipTools.readModBeans(zipFile, modTempMap, MODS_IMAGE_PATH, downloadModPath)
                    list.addAll(mods)
                }
            }
        }
        return list
    }

    suspend fun createModsByDocumentFile(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String
    ): MutableList<ModBean> {
        val list: MutableList<ModBean> = mutableListOf()
        val files = File(scanPath).listFiles()
        if (files != null) {
            // 判断file是否为压缩文件
            for (f in files) {
                Log.d("FileTools", "文件: ${f.name}")
                if (f.isDirectory) continue
                val zipFile = ZipTools.createZipFile(f)
                if (zipFile != null && ZipTools.isZipFile(zipFile)) {
                    // 读取zip文件中的mod文件
                    val modTempMap = createModTempMapByDocumentFile(zipFile, gameModPath)
                    val mods: List<ModBean> =
                        ZipTools.readModBeans(zipFile, modTempMap, MODS_IMAGE_PATH, downloadModPath)
                    list.addAll(mods)
                }
            }
        }
        return list
    }

    // 通过shizuku 扫描mods
    suspend fun createModsByShizuku(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String
    ): MutableList<ModBean>? {
        try {
            return iFileExplorerService?.listFiles(
                scanPath,
                MODS_IMAGE_PATH,
                gameModPath,
                downloadModPath
            )
        } catch (e: NullPointerException) {
            ToastUtils.longCall(R.string.toast_shizuku_load_file_failed)
            e.printStackTrace()
            return null
        } catch (e: RemoteException) {
            ToastUtils.longCall(R.string.toast_shizuku_load_file_failed)
            e.printStackTrace()
            return null
        }

    }

    fun createModTempMapByDocumentFile(
        zipFile: ZipFile,
        gameModPath: String
    ): MutableMap<String, ModBeanTemp> {
        // 创建一个ModBeanTemp的map
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()
        val fileHeaders = zipFile.fileHeaders
        for (fileHeaderObj in fileHeaders) {
            val modName = fileHeaderObj.fileName.substringAfterLast("/")
            //val gameFile = File(gameModPath,modName)
            val gameFile = DocumentFile.fromTreeUri(
                App.get(),
                FileTools.pathToUri(File(gameModPath, modName).absolutePath)
            )
            if (gameFile != null) {
                if (gameFile.exists() && gameFile.isFile) {
                    val modEntries = File(ZipTools.getFileName(fileHeaderObj))
                    val key = modEntries.parent ?: zipFile.file.name
                    val modBeanTemp = modBeanTempMap[key]
                    if (modBeanTemp == null) {
                        val beanTemp = ModBeanTemp(
                            name = zipFile.file.nameWithoutExtension + if (modEntries.parentFile == null) "" else "(" + modEntries.parentFile!!.path.replace(
                                "/",
                                "|"
                            ) + ")", // 如果是根目录则不加括号
                            iconPath = null,
                            readmePath = null,
                            modFiles = mutableListOf(fileHeaderObj.fileName),
                            images = mutableListOf(),
                            fileReadmePath = null,
                            isEncrypted = ZipTools.isEncrypted(zipFile)
                        )
                        modBeanTempMap[key] = beanTemp
                    } else {
                        modBeanTemp.modFiles.add(fileHeaderObj.fileName)
                    }
                }
            }
        }
        Log.d("ZipTools", "modBeanTempMap: $modBeanTempMap")
        // 判断是否存在readme.txt文件
        for (fileHeaderObj in fileHeaders) {
            val fileName = ZipTools.getFileName(fileHeaderObj)
            if (fileName.substringAfterLast("/").equals("readme.txt", ignoreCase = true)) {
                Log.d("ZipTools", "readme文件路径: $fileName")
                val key = File(fileName).parent ?: zipFile.file.name
                Log.d("ZipTools", "readme文件key: $key")
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp != null) {
                    modBeanTemp.readmePath = fileName
                }
            } else if (fileName.contains(".jpg", ignoreCase = true)
                || fileName.contains(".png", ignoreCase = true)
                || fileName.contains(".gif", ignoreCase = true)
                || fileName.contains(".jpeg", ignoreCase = true)
            ) {
                val key = File(fileName).parent ?: zipFile.file.name
                val modBeanTemp = modBeanTempMap[key]
                modBeanTemp?.images?.add(fileName)
                modBeanTemp?.iconPath = fileName
            } else if (fileName.equals("readme.txt", ignoreCase = true)) {
                modBeanTempMap.forEach {
                    val modBeanTemp = it.value
                    modBeanTemp.fileReadmePath = fileName
                }
            }
        }
        return modBeanTempMap
    }

    fun createModTempMapByFile(
        zipFile: ZipFile,
        gameModPath: String
    ): MutableMap<String, ModBeanTemp> {
        // 创建一个ModBeanTemp的map
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()
        val fileHeaders = zipFile.fileHeaders
        for (fileHeaderObj in fileHeaders) {
            val modName = fileHeaderObj.fileName.substringAfterLast("/")
            val gameFile = File(gameModPath, modName)
            if (gameFile.exists() && gameFile.isFile) {
                val modEntries = File(ZipTools.getFileName(fileHeaderObj))
                val key = modEntries.parent ?: zipFile.file.name
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp == null) {
                    val beanTemp = ModBeanTemp(
                        name = zipFile.file.nameWithoutExtension + if (modEntries.parentFile == null) "" else "(" + modEntries.parentFile!!.path.replace(
                            "/",
                            "|"
                        ) + ")", // 如果是根目录则不加括号
                        iconPath = null,
                        readmePath = null,
                        modFiles = mutableListOf(fileHeaderObj.fileName),
                        images = mutableListOf(),
                        fileReadmePath = null,
                        isEncrypted = ZipTools.isEncrypted(zipFile)
                    )
                    modBeanTempMap[key] = beanTemp
                } else {
                    modBeanTemp.modFiles.add(fileHeaderObj.fileName)
                }
            }
        }
        Log.d("ZipTools", "modBeanTempMap: $modBeanTempMap")
        // 判断是否存在readme.txt文件
        for (fileHeaderObj in fileHeaders) {
            val fileName = ZipTools.getFileName(fileHeaderObj)
            if (fileName.substringAfterLast("/").equals("readme.txt", ignoreCase = true)) {
                Log.d("ZipTools", "readme文件路径: $fileName")
                val key = File(fileName).parent ?: zipFile.file.name
                Log.d("ZipTools", "readme文件key: $key")
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp != null) {
                    modBeanTemp.readmePath = fileName
                }
            } else if (fileName.contains(".jpg", ignoreCase = true)
                || fileName.contains(".png", ignoreCase = true)
                || fileName.contains(".gif", ignoreCase = true)
                || fileName.contains(".jpeg", ignoreCase = true)
            ) {
                val key = File(fileName).parent ?: zipFile.file.name
                val modBeanTemp = modBeanTempMap[key]
                modBeanTemp?.images?.add(fileName)
                modBeanTemp?.iconPath = fileName
            } else if (fileName.equals("readme.txt", ignoreCase = true)) {
                modBeanTempMap.forEach {
                    val modBeanTemp = it.value
                    modBeanTemp.fileReadmePath = fileName
                }
            }
        }
        return modBeanTempMap
    }

    suspend fun scanMods(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String,
        pathType: Int
    ): Boolean {
        return when (pathType) {
            PathType.SHIZUKU -> {
                scanModsByShizuku(scanPath, gameModPath, downloadModPath)
            }

            PathType.DOCUMENT -> {
                scanModsByDocumentFile(scanPath, gameModPath, downloadModPath)
            }

            PathType.FILE -> {
                scanModsByFile(scanPath, gameModPath, downloadModPath)
            }

            else -> {
                false
            }
        }

    }


    suspend fun copyModStreamByShizuku(
        modTempPath: String,
        gameModPath: String,
        files: List<String>,
        password: String?
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


    // 写入反和谐文件
    suspend fun writeAntiHarmonyFile(content: String, packageName: String, pathType: Int): Boolean {

        //val packageName = "com.megagame.crosscore"
        Log.d("ConsoleViewModel", "writeAntiHarmonyFile: $content")
        val path = "${ROOT_PATH}/Android/data/${packageName}/files/"
        val filename = "internation.txt"
        val filename2 = "internation_close.txt"

        Log.d("ConsoleViewModel", "文件类型: $pathType")
        return when (pathType) {
            PathType.SHIZUKU -> {
                FileTools.deleteFileByShizuku(path + filename2)
                FileTools.writeFileByShizuku(
                    path,
                    filename,
                    content
                ) ?: false
            }

            PathType.DOCUMENT -> {
                FileTools.deleteFileByDocument(path + filename2)
                FileTools.writeFileByDocument(
                    path,
                    filename,
                    content
                )
            }

            PathType.FILE -> {
                FileTools.deleteFileByFile(path + filename2)
                FileTools.writeFileByFile(
                    path,
                    filename,
                    content
                )
            }

            else -> {
                false
            }
        }

    }

    suspend fun createMods(
        scanPath: String,
        gameModPath: String,
        downloadModPath: String,
        pathType: Int
    ): MutableList<ModBean> {
        when (pathType) {
            PathType.SHIZUKU -> {
                return createModsByShizuku(scanPath, gameModPath, downloadModPath)
                    ?: mutableListOf()
            }

            PathType.DOCUMENT -> {
                return createModsByDocumentFile(scanPath, gameModPath, downloadModPath)
            }

            PathType.FILE -> {
                return createModsByFile(scanPath, gameModPath, downloadModPath, pathType)
            }

            else -> {
                return mutableListOf()
            }
        }

    }

    // 通过流写入mod文件
    suspend fun copyModsByStream(path: String, gameModPath: String, modFiles: List<String>, password: String?, pathType: Int): Boolean {
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
        path: String,
        gameModPath: String,
        modFiles: List<String>,
        password: String?
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
        path: String,
        gameModPath: String,
        modFiles: List<String>,
        password: String?
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
        modTempPath: String,
        gameModPath: String,
        files: String?,
        password: String?
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
        modTempPath: String,
        gameModPath: String,
        files: String?,
        password: String?
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
                    val gameFile = DocumentFile.fromTreeUri(
                        App.get(),
                        FileTools.pathToUri(gameModPath)
                    )
                    val fileOutputStream = FileTools.app.contentResolver.openOutputStream(gameFile?.uri!!)
                    val inputStream = zipFile.getInputStream(fileHeaderObj)
                    if (fileOutputStream != null) {
                        inputStream?.copyTo(fileOutputStream)
                    }

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
}
