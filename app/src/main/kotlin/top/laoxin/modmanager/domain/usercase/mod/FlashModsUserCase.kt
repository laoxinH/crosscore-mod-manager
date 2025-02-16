package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.bean.ModBeanTemp
import top.laoxin.modmanager.data.bean.ScanFileBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.data.repository.scanfile.ScanFileRepository
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.filetools.impl.ShizukuFileTools
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import top.laoxin.modmanager.ui.state.UserPreferencesState
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class FlashModsResult(
    val code: Int,
    // 新增mods
    val newMods: List<ModBean>,
    // 删除mods
    val delMods: List<ModBean>,
    // 更新mods
    val updateMods: List<ModBean>,
    // 删除已启用的mods
    val delEnableMods: List<ModBean>,
    // 提示信息
    val message: String,
)

@Singleton
class FlashModsUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val appPathsManager: AppPathsManager,
    private val scanFileRepository: ScanFileRepository,
    private val modRepository: ModRepository,
    private val readModReadmeFileUserCase: ReadModReadmeFileUserCase,
    private val permissionTools: PermissionTools,
    private val flashModsObserverManager: FlashModsObserverManager,
    private val fileToolsManager: FileToolsManager,
    private val specialGameToolsManager: SpecialGameToolsManager,
    private val ensureGameModPathUserCase: EnsureGameModPathUserCase
) {

    companion object {
        const val TAG = "FlashModsUserCase"
    }

    suspend operator fun invoke(
        userPreferencesState: UserPreferencesState,
        mods: List<ModBean>,
        setLoadingPath: (String) -> Unit,
        forceScan: Boolean = false
    ): FlashModsResult = withContext(Dispatchers.IO) {
        flashModsObserverManager.stopWatching()
        try {
            ensureGameModPathUserCase(gameInfoManager.getGameInfo().modSavePath)
            val gameInfo = gameInfoManager.getGameInfo()

            // 扫描QQ
            if (userPreferencesState.scanQQDirectory) {
                setLoadingPath(ScanModPath.MOD_PATH_QQ)
                scanMods(ScanModPath.MOD_PATH_QQ, gameInfoManager.getGameInfo())
            }

            // 扫描下载目录
            if (userPreferencesState.scanDownload) {
                setLoadingPath(ScanModPath.MOD_PATH_DOWNLOAD)
                scanMods(ScanModPath.MOD_PATH_DOWNLOAD, gameInfoManager.getGameInfo())
            }

            // 扫描用户选择的目录
            setLoadingPath(appPathsManager.getRootPath() + userPreferencesState.selectedDirectory)
            scanMods(
                appPathsManager.getRootPath() + userPreferencesState.selectedDirectory,
                gameInfoManager.getGameInfo()
            )

            // 读取scanfiles
            var scanFiles = scanFileRepository.getAll().first()
            // 判断scanfiles中的文件是否被删除
            scanFiles.filter {
                !File(it.path).exists() || File(it.path).lastModified() != it.modifyTime || File(
                    it.path
                ).length() != it.size
            }.forEach {
                Log.d("ModViewModel", "文件已删除: $it")
                scanFileRepository.delete(it)
            }
            scanFiles =
                scanFiles.filter { File(it.path).exists() && File(it.path).lastModified() == it.modifyTime }

            // 创建mods
            val modsScan = createArchiveMods(
                gameInfo.modSavePath, gameInfo, scanFiles, forceScan
            ).toMutableList()

            // 扫描文件夹中的mod文件
            if (userPreferencesState.scanDirectoryMods) {
                modsScan.addAll(
                    scanDirectoryMods(
                        gameInfo.modSavePath, gameInfo
                    )
                )
            }

            // 在mods中找到和scanFiles中同路径的mod
            val mods1 = mods.filter { mod ->
                scanFiles.any { it.path == mod.path }
            }
            modsScan.addAll(mods1)

            Log.d("ModViewModel", "已有的mods: $mods")
            // 对比modsScan和mods，对比是否有新的mod
            val addMods = getNewMods(mods, modsScan)
            Log.d("ModViewModel", "添加: $addMods")
            // 对比modsScan和mods，删除数据库中不存在的mod
            val result = checkDelMods(mods, modsScan)
            // 去除数据库mods中重复的Mod
            modRepository.insertAll(addMods)
            val updateMods = checkUpdateMods(mods, modsScan)
            modRepository.updateAll(updateMods)

            return@withContext FlashModsResult(
                code = ResultCode.SUCCESS,
                newMods = addMods,
                delMods = result.first,
                updateMods = updateMods,
                delEnableMods = result.second,
                message = ""
            )
        } catch (e: Exception) {
            Log.e("ModViewModel", "刷新mods失败: $e")
            logRecord("刷新mods失败: $e")
            return@withContext FlashModsResult(
                code = ResultCode.FAIL,
                newMods = emptyList(),
                delMods = emptyList(),
                updateMods = emptyList(),
                delEnableMods = emptyList(),
                message = e.message ?: ""
            )
        } finally {
            flashModsObserverManager.startWatching()
        }

    }


    // 扫描文件夹mod
    suspend fun scanDirectoryMods(scanPath: String, gameInfo: GameInfoBean): List<ModBean> {
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
            modBeans.addAll(readModBeans(null, createModTempMap))
            return modBeans
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            logRecord("扫描文件夹Mod失败: $e")
            return emptyList()
        }
    }

    // 通过DocumentFile扫描mods
    suspend fun scanMods(scanPath: String, gameInfo: GameInfoBean): Boolean {
        var fileTools = fileToolsManager.getFileTools(permissionTools.checkPermission(scanPath))
        // shizuku扫描
        if (permissionTools.checkPermission(scanPath) == PathType.SHIZUKU) {
            return (fileToolsManager.getShizukuFileTools() as ShizukuFileTools).scanModsByShizuku(
                scanPath,
                gameInfo
            )
        }
        var tempScanPath = scanPath
        if (scanPath == ScanModPath.MOD_PATH_QQ) {
            fileTools = fileToolsManager.getFileTools(PathType.DOCUMENT)
            Log.d(TAG, "开始扫描QQ: ")
            tempScanPath = appPathsManager.getModsTempPath() + "Mods/"
            val pathUri = fileTools?.pathToUri(ScanModPath.MOD_PATH_QQ)
            val scanModsFile = DocumentFile.fromTreeUri(App.get(), pathUri!!)
            val documentFiles = scanModsFile?.listFiles()
            Log.d(TAG, "扫描到的文件:$documentFiles ")
            for (documentFile in documentFiles!!) {
                Log.d(TAG, "扫描到的文件: ${documentFile.name}")
                try {
                    if (isScanFile(documentFile)) {
                        fileTools.copyFileByDF(
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
        fileTools = fileToolsManager.getFileTools(PathType.FILE)
        File(tempScanPath).listFiles()?.forEach { file ->
            if (ArchiveUtil.isArchive(file.absolutePath)) {
                for (filename in ArchiveUtil.listInArchiveFiles(file.absolutePath)) {
                    if (gameInfo.gameFilePath.map { it + File(filename).name }.any {
                            fileTools =
                                fileToolsManager.getFileTools(permissionTools.checkPermission(it))
                            fileTools?.isFileExist(it) == true
                        } || specialOperationScanMods(gameInfo.packageName, filename)) {
                        Log.d(TAG, "开始移动文件: ${gameInfo.modSavePath + file.name}")
                        fileTools = fileToolsManager.getFileTools(PathType.FILE)
                        fileTools?.moveFile(file.absolutePath, gameInfo.modSavePath + file.name)
                        break
                    }
                }
            }
        }
        fileTools?.deleteFile(appPathsManager.getModsTempPath() + "Mods/")
        return true

    }

    // 扫描压缩文件mod
    suspend fun createArchiveMods(
        scanPath: String,
        gameInfo: GameInfoBean,
        scanFiles: List<ScanFileBean>,
        forceScan: Boolean = false
    ): MutableList<ModBean> {
        val fileTools = fileToolsManager.getFileTools()
        val scanMods = mutableListOf<ModBean>()
        val archiveFiles = mutableListOf<File>()
        // 收集所有符合条件的压缩包，包含子文件夹
        withContext(Dispatchers.IO) {
            Files.walk(Paths.get(scanPath)).use { paths ->
                paths.filter { path ->
                    val file = path.toFile()
                    !file.isDirectory && fileTools.isExcludeFileType(file.name) == false && ArchiveUtil.isArchive(
                        file.absolutePath
                    )
                }.forEach { path ->
                    archiveFiles.add(path.toFile())
                }
            }
        }

        if (forceScan) {
            // 强制扫描，删除所有scanfile
            scanFileRepository.deleteAll()
        }

        // 处理收集到的压缩包mod
        for (file in archiveFiles) {
            // 判断文件是否再scanfiles中
            val scanFile = scanFiles.find { it.path == file.absolutePath }

            if (!forceScan && scanFile != null) {
                // 如果文件已经扫描过，判断是否需要更新
                if (scanFile.modifyTime != file.lastModified() || scanFile.size != file.length()) {
                    scanFileRepository.update(
                        scanFile.copy(
                            modifyTime = file.lastModified(),
                            size = file.length()
                        )
                    )
                } else {
                    continue
                }
            } else {
                // 如果文件没有扫描过，插入scanfile表
                insertScanFile(
                    ScanFileBean(
                        path = file.absolutePath,
                        name = file.name,
                        modifyTime = file.lastModified(),
                        size = file.length()
                    )
                )
            }

            val modTempMap = createModTempMap(
                file.absolutePath,
                scanPath,
                ArchiveUtil.listInArchiveFiles(file.absolutePath),
                gameInfo
            )
            Log.d(TAG, "modTempMap: $modTempMap")

            val mods = readModBeans(
                file.absolutePath,
                modTempMap,
            )
            scanMods.addAll(mods)
        }

        Log.d(TAG, "modsList: $scanMods")
        return scanMods
    }

    private fun checkDelMods(
        mods: List<ModBean>,
        modsScan: MutableList<ModBean>,
    ): Pair<List<ModBean>, List<ModBean>> {
        val delMods = mutableListOf<ModBean>()
        mods.forEach {
            val mod = it.isDelete(modsScan)
            if (mod != null) {
                delMods.add(mod)
            }
        }
        Log.d("ModViewModel", "删除: $delMods")
        val delEnableMods = delMods.filter { it.isEnable }
        return Pair(delMods, delEnableMods)


    }

    // 对比是否有更新mod
    private fun checkUpdateMods(
        mods: List<ModBean>, modsScan: List<ModBean>
    ): MutableList<ModBean> {
        val updatedMods = mutableListOf<ModBean>()
        for (mod in mods) {
            val mod1 = mod.isUpdate(modsScan)
            if (mod1 != null) {
                updatedMods.add(mod1)
            }
        }
        Log.d("ModViewModel", "更新: $updatedMods")
        return updatedMods
    }

    private fun getNewMods(mods: List<ModBean>, modsScan: MutableList<ModBean>): List<ModBean> {
        val newMods = mutableListOf<ModBean>()
        for (modBean in modsScan) {
            modBean.isNew(mods)?.let { newMods.add(it) }
        }
        return newMods
    }

    private fun createModTempMap(
        filepath: String?,
        scanPath: String,
        files: List<String>,
        gameInfo: GameInfoBean
    ): MutableMap<String, ModBeanTemp> {
        val archiveFile: File? = filepath?.let { File(it) }
        val fileTools =
            fileToolsManager.getFileTools(permissionTools.checkPermission(gameInfo.gamePath))
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()
        for (file in files) {
            val modFileName = File(file).name
            val gameFileMap = mutableMapOf<String, String>()
            if (!gameInfo.isGameFileRepeat) {
                // 如果游戏文件路径不重复
                gameFileMap.apply {
                    gameInfo.gameFilePath.forEachIndexed { index, _ ->
                        put(
                            gameInfo.modType[index],
                            File(gameInfo.gameFilePath[index], modFileName).absolutePath
                        )
                    }
                }
            } else {
                // 如果游戏文件路径重复
                gameFileMap.apply {
                    gameInfo.gameFilePath.forEachIndexed { index, it ->
                        val pathName = File(it).name
                        if (pathName == (File(file).parentFile?.name ?: "")) {
                            put(
                                gameInfo.modType[index],
                                File(gameInfo.gameFilePath[index], modFileName).absolutePath
                            )
                        }
                    }
                }
            }
            gameFileMap.forEach {
                if ((fileTools?.isFileExist(it.value) == true && fileTools.isFile(it.value)) || specialOperationScanMods(
                        gameInfo.packageName,
                        modFileName
                    )
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

    private suspend fun readModBeans(
        filepath: String?,
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
                            appPathsManager.getModsTempPath()
                        )
                        val readmeFile = File(appPathsManager.getModsTempPath(), readmeFilename)
                        mod = readModReadmeFileUserCase(appPathsManager.getModsTempPath(), mod)
                        readmeFile.delete()
                    }

                    var icon = modBeanTemp.iconPath
                    var images: List<String> = modBeanTemp.images
                    kotlin.runCatching {
                        ArchiveUtil.extractSpecificFile(
                            archiveFile.absolutePath,
                            modBeanTemp.images,
                            appPathsManager.getModsImagePath() + archiveFile.nameWithoutExtension,

                            )
                        icon =
                            appPathsManager.getModsImagePath() + archiveFile.nameWithoutExtension + "/" + icon
                        images = modBeanTemp.images.map {
                            appPathsManager.getModsImagePath() + archiveFile.nameWithoutExtension + "/" + it
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
                    list.add(readModReadmeFileUserCase("", modBean))
                }
            }
        }
        return list

    }


    // 判断是否为待扫描的文件
    private fun isScanFile(documentFile: DocumentFile): Boolean {
        val fileTools = fileToolsManager.getFileTools(PathType.DOCUMENT)
        if (documentFile.isDirectory) return false
        if (documentFile.name?.let { fileTools?.isExcludeFileType(it) } == true) return false
        return true
    }

    // 特殊游戏扫描操作

    fun specialOperationScanMods(packageName: String, modFileName: String): Boolean {
        return specialGameToolsManager.getSpecialGameTools(packageName)?.specialOperationScanMods(
            packageName,
            modFileName
        ) == true

    }

    // 向scanfile表中插入数据
    suspend fun insertScanFile(scanFile: ScanFileBean) {
        scanFileRepository.insert(scanFile)
    }

}