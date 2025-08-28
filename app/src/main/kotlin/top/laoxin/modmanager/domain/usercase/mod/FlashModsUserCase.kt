package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
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
import top.laoxin.modmanager.tools.filetools.BaseFileTools
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
import kotlin.coroutines.cancellation.CancellationException

// 刷新Mods结果数据类
data class FlashModsResult(
    val code: Int,
    val newMods: List<ModBean>,     // 新增mods
    val delMods: List<ModBean>,     // 删除mods
    val updateMods: List<ModBean>,  // 更新mods
    val delEnableMods: List<ModBean>, // 删除已启用的mods
    val message: String,            // 提示信息
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
    private val ensureGameModPathUserCase: EnsureGameModPathUserCase,
    private val flashMOdDetailUserCase: FlashModDetailUserCase,
    private val flashModImageUserCase: FlashModImageUserCase
) {

    companion object {
        const val TAG = "FlashModsUserCase"
    }

    // 主函数：刷新Mods
    suspend operator fun invoke(
        userPreferencesState: UserPreferencesState,
        mods: List<ModBean>,
        setLoadingPath: (String) -> Unit,
        forceScan: Boolean = false
    ): FlashModsResult = withContext(Dispatchers.IO) {
        // 停止文件监视
        flashModsObserverManager.stopWatching()

        try {
            // 确保游戏Mod路径存在
            ensureGameModPathUserCase(gameInfoManager.getGameInfo().modSavePath)
            val gameInfo = gameInfoManager.getGameInfo()

            // 扫描QQ目录
            if (userPreferencesState.scanQQDirectory) {
                ensureActive()
                setLoadingPath(ScanModPath.MOD_PATH_QQ)
                scanMods(ScanModPath.MOD_PATH_QQ, gameInfo)
            }

            // 扫描下载目录
            if (userPreferencesState.scanDownload) {
                ensureActive()
                setLoadingPath(ScanModPath.MOD_PATH_DOWNLOAD)
                scanMods(ScanModPath.MOD_PATH_DOWNLOAD, gameInfo)
            }

            // 扫描用户选择的目录
            ensureActive()
            val userDir = appPathsManager.getRootPath() + userPreferencesState.selectedDirectory
            setLoadingPath(userDir)
            scanMods(userDir, gameInfo)

            // 强制扫描，清空扫描记录
            if (forceScan) {
                scanFileRepository.deleteAll()
            }

            // 获取并过滤已扫描文件列表
            var scanFiles = scanFileRepository.getAll().first()
            // 过滤掉已删除或修改的文件
            scanFiles = scanFiles.filter {
                ensureActive()
                val file = File(it.path)
                file.exists() && file.lastModified() == it.modifyTime && file.length() == it.size
            }

            // 从不存在或已修改的文件中删除扫描记录
            scanFiles.filter {
                ensureActive()
                val file = File(it.path)
                !file.exists() || file.lastModified() != it.modifyTime || file.length() != it.size
            }.forEach {
                Log.d("ModViewModel", "文件已删除或修改: $it")
                scanFileRepository.delete(it)
            }

            // 创建压缩包Mods
            val modsScan = createArchiveMods(
                gameInfo.modSavePath, gameInfo, scanFiles, forceScan
            ).toMutableList()

            // 扫描文件夹中的mod文件
            if (userPreferencesState.scanDirectoryMods) {
                ensureActive()
                modsScan.addAll(scanDirectoryMods(gameInfo.modSavePath, gameInfo))
            }

            // 添加已有的但仍然存在的Mods
            ensureActive()
            modsScan.addAll(mods.filter { mod -> scanFiles.any { it.path == mod.path } })

            // 强制扫描时刷新所有Mod信息和图片
            if (forceScan) {
                modsScan.forEach {
                    flashMOdDetailUserCase(it)
                    flashModImageUserCase(it)
                }
            }

            // 对比新增、删除和更新的Mod
            val addMods = getNewMods(mods, modsScan)
            Log.d("ModViewModel", "添加: $addMods")

            // 检查删除的Mod
            val result = checkDelMods(mods, modsScan)

            // 检查更新的Mod
            val updateMods = checkUpdateMods(mods, modsScan)
            ensureActive()

            // 更新数据库
            modRepository.insertAll(addMods)
            modRepository.updateAll(updateMods)

            // 去重名字和路径相同的Mod
            delRepeatModsByPathAndName()

            // 去重路径相同的Mod
            if (forceScan) {
                delDuplicateModsByPathAndVirtualPathsAndModType()
            }

            return@withContext FlashModsResult(
                code = ResultCode.SUCCESS,
                newMods = addMods,
                delMods = result.first,
                updateMods = updateMods,
                delEnableMods = result.second,
                message = ""
            )
        } catch (e: Exception) {
            if (e is CancellationException) {
                logRecord("刷新mods任务被取消: $e")
                Log.d(TAG, "invoke: $e")
            }
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
            // 无论成功失败，都重新开始监视
            withContext(NonCancellable) {
                flashModsObserverManager.startWatching()
            }
        }
    }

    // 数据库Mod去重
    private suspend fun delRepeatModsByPathAndName() {
        val allMods = modRepository.getAllIModsStream().first()
        // 按路径和名称分组
        val groupedMods = allMods.groupBy { Pair(it.path, it.name) }
        val filteredMods = mutableListOf<ModBean>()

        // 每组保留一个（优先保留已启用且最新的）
        for ((_, sameMods) in groupedMods) {
            val enabledMods = sameMods.filter { it.isEnable }
            val chosen = if (enabledMods.isNotEmpty()) {
                enabledMods.maxByOrNull { it.date }
            } else {
                sameMods.maxByOrNull { it.date }
            }
            if (chosen != null) {
                filteredMods.add(chosen)
            }
        }

        // 删除重复项
        val duplicates = allMods - filteredMods
        modRepository.deleteAll(duplicates)
    }

    private suspend fun delDuplicateModsByPathAndVirtualPathsAndModType() {
        val allMods = modRepository.getAllIModsStream().first()

        // 按 path 进行分组
        val groupedMods =
            allMods.groupBy {
                // virtualPaths为空时不参与去重
                if (it.virtualPaths.isNullOrEmpty()) {
                    Triple(it.path, null, it.modType)
                } else {
                    Triple(it.path, it.virtualPaths, it.modType)
                }
            }
        val filteredMods = mutableListOf<ModBean>()

        for ((_, samePathMods) in groupedMods) {
            val enabledMods = samePathMods.filter { it.isEnable }

            // 选出要保留的 Mod
            val chosen = if (enabledMods.isNotEmpty()) {
                enabledMods.maxByOrNull { it.date }
            } else {
                samePathMods.maxByOrNull { it.date }
            }

            if (chosen != null) {
                filteredMods.add(chosen)
            }
        }

        // 计算需要删除的重复项
        val duplicates = allMods - filteredMods
        modRepository.deleteAll(duplicates)
    }

    // 扫描指定路径的Mod文件
    suspend fun scanMods(scanPath: String, gameInfo: GameInfoBean): Boolean {
        var fileTools: BaseFileTools?

        // Shizuku模式扫描
        if (permissionTools.checkPermission(scanPath) == PathType.SHIZUKU) {
            Log.d(TAG, "开始扫描Shizuku: $scanPath")
            val shizukuTools = fileToolsManager.getFileTools(PathType.SHIZUKU) as ShizukuFileTools
            return shizukuTools.scanModsByShizuku(scanPath, gameInfo)
        }

        // QQ目录特殊处理
        var tempScanPath = scanPath
        if (scanPath == ScanModPath.MOD_PATH_QQ) {
            fileTools = fileToolsManager.getFileTools(PathType.DOCUMENT)
            Log.d(TAG, "开始扫描QQ")
            tempScanPath = appPathsManager.getModsTempPath() + "Mods/"
            val pathUri = fileTools?.pathToUri(ScanModPath.MOD_PATH_QQ)
            val scanModsFile = DocumentFile.fromTreeUri(App.get(), pathUri!!)
            val documentFiles = scanModsFile?.listFiles()

            // 拷贝QQ目录中的文件到临时目录
            for (documentFile in documentFiles!!) {
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

        // 处理找到的mod文件
        fileTools = fileToolsManager.getFileTools(PathType.FILE)
        File(tempScanPath).listFiles()?.forEach { file ->
            if (ArchiveUtil.isArchive(file.absolutePath)) {
                // 检查压缩包中是否包含游戏相关文件
                for (filename in ArchiveUtil.listInArchiveFiles(file.absolutePath)) {
                    gameInfo.gameFilePath.map { it + File(filename).name }.forEach {
                        Log.d(TAG, "scanMods压缩中的文件映射: $it")
                    }
                    if (gameInfo.gameFilePath.map { it + File(filename).name }.any {
                            fileTools =
                                fileToolsManager.getFileTools(permissionTools.checkPermission(it))
                            fileTools?.isFileExist(it) == true
                        } || specialOperationScanMods(gameInfo.packageName, filename)) {
                        // 移动文件到mod保存目录
                        // Log.d(TAG, "开始移动文件: ${gameInfo.modSavePath + file.name}")
                        fileTools =
                            fileToolsManager.getFileTools(permissionTools.checkPermission(file.absolutePath))
                        //Log.d(TAG, "移动权限: ${permissionTools.checkPermission(file.absolutePath)}")
                        fileTools?.moveFile(file.absolutePath, gameInfo.modSavePath + file.name)
                        break
                    }
                }
            }
        }

        // 清理临时目录
        fileTools?.deleteFile(appPathsManager.getModsTempPath() + "Mods/")
        return true
    }

    // 扫描文件夹mod
    suspend fun scanDirectoryMods(scanPath: String, gameInfo: GameInfoBean): List<ModBean> {
        try {
            val modBeans = mutableListOf<ModBean>()
            val files = mutableListOf<String>()

            // 获取所有文件路径
            withContext(Dispatchers.IO) {
                Files.walk(Paths.get(scanPath))
            }.sorted(Comparator.reverseOrder()).forEach {
                files.add(it.toString())
            }

            // 创建临时mod映射
            val createModTempMap = createModTempMap(null, scanPath, files, gameInfo)
            modBeans.addAll(readModBeans(null, createModTempMap))
            return modBeans
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            logRecord("扫描文件夹Mod失败: $e")
            return emptyList()
        }
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

        // 收集所有符合条件的压缩包
        withContext(Dispatchers.IO) {
            Files.walk(Paths.get(scanPath)).use { paths ->
                paths.filter { path ->
                    val file = path.toFile()
                    !file.isDirectory && !fileTools.isExcludeFileType(file.name) && ArchiveUtil.isArchive(
                        file.absolutePath
                    )
                }.forEach { path ->
                    archiveFiles.add(path.toFile())
                }
            }
        }

        // 处理收集到的压缩包mod
        for (file in archiveFiles) {
            // 检查文件是否已扫描
            val scanFile = scanFiles.find { it.path == file.absolutePath }

            // 处理扫描文件记录
            if (!forceScan && scanFile != null) {
                // 文件已扫描过，检查是否需要更新
                if (scanFile.modifyTime != file.lastModified() || scanFile.size != file.length()) {
                    scanFileRepository.update(
                        scanFile.copy(
                            modifyTime = file.lastModified(),
                            size = file.length()
                        )
                    )
                } else {
                    continue  // 无变化，跳过
                }
            } else {
                // 新文件，添加扫描记录
                insertScanFile(
                    ScanFileBean(
                        path = file.absolutePath,
                        name = file.name,
                        modifyTime = file.lastModified(),
                        size = file.length()
                    )
                )
            }

            // 创建Mod临时映射
            val modTempMap = createModTempMap(
                file.absolutePath,
                scanPath,
                ArchiveUtil.listInArchiveFiles(file.absolutePath),
                gameInfo
            )
            Log.d(TAG, "modTempMap: $modTempMap")

            // 读取Mod信息
            val mods = readModBeans(file.absolutePath, modTempMap)
            scanMods.addAll(mods)
        }

        Log.d(TAG, "modsList: $scanMods")
        return scanMods
    }

    // 检查已删除的Mod
    private fun checkDelMods(
        mods: List<ModBean>,
        modsScan: MutableList<ModBean>,
    ): Pair<List<ModBean>, List<ModBean>> {
        val delMods = mutableListOf<ModBean>()

        // 比较找出已删除的Mod
        mods.forEach {
            val mod = it.isDelete(modsScan)
            if (mod != null) {
                delMods.add(mod)
            }
        }

        Log.d("ModViewModel", "删除: $delMods")
        // 找出已删除且启用的Mod
        val delEnableMods = delMods.filter { it.isEnable }
        return Pair(delMods, delEnableMods)
    }

    // 检查需要更新的Mod
    private fun checkUpdateMods(
        mods: List<ModBean>, modsScan: List<ModBean>
    ): MutableList<ModBean> {
        val updatedMods = mutableListOf<ModBean>()

        // 比较找出需要更新的Mod
        for (mod in mods) {
            val mod1 = mod.isUpdate(modsScan)
            if (mod1 != null) {
                updatedMods.add(mod1)
            }
        }

        Log.d("ModViewModel", "更新: $updatedMods")
        return updatedMods
    }

    // 获取新增的Mods
    private fun getNewMods(mods: List<ModBean>, modsScan: MutableList<ModBean>): List<ModBean> {
        val newMods = mutableListOf<ModBean>()

        // 找出新增的Mod
        for (modBean in modsScan) {
            modBean.isNew(mods)?.let { newMods.add(it) }
        }

        return newMods
    }

    // 创建ModTemp映射
    private fun createModTempMap(
        filepath: String?,
        scanPath: String,
        files: List<String>,
        gameInfo: GameInfoBean
    ): MutableMap<String, ModBeanTemp> {
        //Log.d(TAG, "createModTempMap--files: $files")
        val archiveFile: File? = filepath?.let { File(it) }
        val fileTools = fileToolsManager.getFileTools(
            permissionTools.checkPermission(gameInfo.gamePath)
        )
        val modBeanTempMap = mutableMapOf<String, ModBeanTemp>()

        // 遍历文件创建Mod映射
        for (file in files) {
            val modFileName = File(file).name
            val gameFileMap = mutableMapOf<String, String>()

            // 根据游戏文件路径特性创建映射
            if (!gameInfo.isGameFileRepeat) {
                // 游戏文件路径不重复
                gameFileMap.apply {
                    gameInfo.gameFilePath.forEachIndexed { index, _ ->
                        put(
                            gameInfo.modType[index],
                            File(gameInfo.gameFilePath[index], modFileName).absolutePath
                        )
                    }
                }
            } else {
                // 游戏文件路径重复
                gameFileMap.apply {
                    gameInfo.gameFilePath.forEachIndexed { index, it ->
                        val pathName = File(it).name
                        if (pathName == (File(file).parentFile?.name ?: "")) {
                            put(
                                gameInfo.modType[index],
                                File(gameInfo.gameFilePath[index], modFileName).absolutePath
                            )
                        } else {
                            put(
                                gameInfo.modType[index],
                                File(
                                    gameInfo.gameFilePath[index],
                                    (File(file).parentFile?.name ?: "") + "/" + File(file).name
                                ).absolutePath
                            )
                        }
                    }
                }
            }

            // 检查文件是否存在于游戏中
            gameFileMap.forEach {
                // Log.d(TAG, "createModTempMap游戏文件映射: ${it.key} -> ${it.value}")
                if ((fileTools?.isFileExist(it.value) == true && fileTools.isFile(it.value)) ||
                    specialOperationScanMods(gameInfo.packageName, modFileName)
                ) {

                    val modEntries = File(file.replace(scanPath, ""))
                    val key = modEntries.parent ?: archiveFile?.name ?: modEntries.name
                    val modBeanTemp = modBeanTempMap[key]
                    var gameModPath = gameInfo.gameFilePath[gameInfo.modType.indexOf(it.key)]
                    if (!gameModPath.contains(modEntries.parent ?: "")) {
                        if (fileTools?.isFileExist(
                                File(
                                    gameModPath,
                                    modEntries.parentFile?.name ?: ""
                                ).absolutePath
                            ) == true
                        ) {
                            gameModPath = File(
                                gameModPath,
                                modEntries.parentFile?.name ?: ""
                            ).absolutePath + File.separator
                        }
                    }

                    // 创建或更新ModBeanTemp
                    if (modBeanTemp == null) {
                        // 计算Mod名称
                        val modName =
                            (archiveFile?.nameWithoutExtension + if (modEntries.parentFile == null) {
                                if (archiveFile == null) modEntries.name else ""
                            } else "(" + modEntries.parentFile!!.path.replace(
                                "/",
                                "|"
                            ) + ")").replace(
                                "null",
                                App.get().getString(R.string.mod_temp_bean_name_dictionary_desc)
                            )

                        // 创建新的ModBeanTemp
                        val beanTemp = ModBeanTemp(
                            name = modName,
                            iconPath = null,
                            readmePath = null,
                            modFiles = mutableListOf(file),
                            images = mutableListOf(),
                            fileReadmePath = null,
                            isEncrypted = if (archiveFile == null) false else
                                ArchiveUtil.isArchiveEncrypted(archiveFile.absolutePath),
                            gamePackageName = gameInfo.packageName,
                            modType = it.key,
                            gameModPath = gameModPath,
                            isZip = if (archiveFile == null) false else
                                ArchiveUtil.isArchive(archiveFile.absolutePath),
                            modPath = if (archiveFile == null) File(file).parent ?: file
                            else archiveFile.absolutePath,
                            virtualPaths = if (archiveFile == null) "" else
                                archiveFile.absolutePath + File(file).parentFile?.absolutePath
                        )
                        modBeanTempMap[key] = beanTemp
                    } else {
                        // 已存在则添加文件
                        modBeanTemp.modFiles.add(file)
                    }
                }
            }
        }

        Log.d(TAG, "modBeanTempMap: $modBeanTempMap")

        // 查找并添加readme和图片文件
        for (file in files) {
            Log.d(TAG, file)
            val modEntries = File(file.replace(scanPath, ""))
            val key = modEntries.parent ?: archiveFile?.name ?: modEntries.name

            // 检查readme文件
            if (file.substringAfterLast("/").equals("readme.txt", ignoreCase = true) ||
                file.substringAfterLast("/").equals("readme.md", ignoreCase = true)
            ) {
                val modBeanTemp = modBeanTempMap[key]
                if (modBeanTemp != null) {
                    Log.d(TAG, "readmePath: $file")
                    modBeanTemp.readmePath = file
                }
            }
            // 检查图片文件
            else if (file.contains(".jpg", ignoreCase = true) ||
                file.contains(".png", ignoreCase = true) ||
                file.contains(".gif", ignoreCase = true) ||
                file.contains(".jpeg", ignoreCase = true)
            ) {
                val modBeanTemp = modBeanTempMap[key]
                modBeanTemp?.images?.add(file)
                modBeanTemp?.iconPath = file
            }
            // 根目录readme文件
            else if (file.equals("readme.txt", ignoreCase = true) ||
                file.equals("readme.md", ignoreCase = true)
            ) {
                modBeanTempMap.forEach {
                    val modBeanTemp = it.value
                    modBeanTemp.fileReadmePath = file
                }
            }
        }

        return modBeanTempMap
    }

    // 读取Mod详细信息
    private fun readModBeans(
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
            // 创建基础ModBean
            val modBean = ModBean(
                id = 0,
                name = modBeanTemp.name,
                version = "1.0",
                description = null,
                author = null,
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

            // 处理加密的Mod
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
                // 处理普通Mod
                if (archiveFile != null) {
                    var mod = modBean

                    // 处理readme文件
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

                    // 处理图片文件
                    var icon = modBeanTemp.iconPath
                    var images: List<String> = modBeanTemp.images
                    runCatching {
                        ArchiveUtil.extractSpecificFile(
                            archiveFile.absolutePath,
                            modBeanTemp.images,
                            appPathsManager.getModsImagePath() + archiveFile.nameWithoutExtension
                        )
                        icon = appPathsManager.getModsImagePath() +
                                archiveFile.nameWithoutExtension + "/" + icon
                        images = modBeanTemp.images.map {
                            appPathsManager.getModsImagePath() +
                                    archiveFile.nameWithoutExtension + "/" + it
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
                    // 处理非压缩包Mod
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
        return specialGameToolsManager.getSpecialGameTools(packageName)
            ?.specialOperationScanMods(packageName, modFileName) == true
    }

    // 向scanfile表中插入数据
    suspend fun insertScanFile(scanFile: ScanFileBean) {
        scanFileRepository.insert(scanFile)
    }
}
