package top.laoxin.modmanager.data.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ModForm
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.model.ScanEvent
import top.laoxin.modmanager.domain.model.ScanStep
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.TraditionalModScanService

/** 传统 MOD 扫描服务实现 负责扫描压缩包和文件夹，通过被动识别构建 ModBean */
class TraditionalModScanServiceImpl
@Inject
constructor(private val archiveService: ArchiveService, private val fileService: FileService) :
    TraditionalModScanService {

    companion object {
        const val TAG = "ModScanServiceImpl"

        // 根目录文件的特殊标记
        private const val ROOT_DIR_KEY = "."

        // 图片和文档文件扩展名（非游戏文件）
        private val NON_GAME_FILE_EXTENSIONS =
            setOf(
                "png",
                "jpg",
                "jpeg",
                "webp",
                "bmp",
                "gif", // 图片
                "txt",
                "md",
                "readme",
                "doc",
                "docx" // 文档
            )
    }

    override suspend fun scanArchive(
        archivePath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Result<List<ModBean>> {
        return try {
            // 1. 检查是否加密
            val isEncrypted =
                when (val encryptedResult = archiveService.isEncrypted(archivePath)) {
                    is Result.Success -> encryptedResult.data
                    is Result.Error -> return Result.Error(encryptedResult.error)
                }

            // 2. 获取文件列表
            val filesResult = archiveService.listFiles(archivePath)
            if (filesResult is Result.Error) {
                // 无法读取文件列表（完全加密），返回加密提示MOD
                //                if (filesResult.error is
                // AppError.ArchiveError.EncryptedNeedPassword) {
                //                    return Result.Success(listOf(createEncryptedMod(archivePath,
                // gameInfo, true)))
                //                }
                return Result.Error(filesResult.error)
            }
            val filePaths = (filesResult as Result.Success).data

            // 3. 识别 MOD（传入加密标志）
            val mods =
                identifyMods(
                    sourcePath = archivePath,
                    allFilePaths = filePaths,
                    gameInfo = gameInfo,
                    isZip = true,
                    isEncrypted = isEncrypted,
                    gameFilesByDir = gameFilesByDir,
                    // 传入预加载的映射
                )

            Result.Success(mods.filter { it.modFiles.isNotEmpty() })
        } catch (e: Exception) {
            Log.e(TAG, "Scan archive failed: $archivePath", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun scanDirectoryMod(
        folderPath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Result<List<ModBean>> {
        return try {
            val rootFile = File(folderPath)
            if (!rootFile.exists() || !rootFile.isDirectory) {
                return Result.Error(AppError.FileError.FileNotFound(folderPath))
            }

            // 遍历文件夹获取所有文件的相对路径
            val filePaths = mutableListOf<String>()
            rootFile.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.toRelativeString(rootFile).replace("//", "/")
                    filePaths.add(relativePath)
                }
            }

            // 识别 MOD（文件夹不加密）
            val mods =
                identifyMods(
                    sourcePath = folderPath,
                    allFilePaths = filePaths,
                    gameInfo = gameInfo,
                    isZip = false,
                    gameFilesByDir = gameFilesByDir,
                    // 传入预加载的映射
                )

            Result.Success(mods.filter { it.modFiles.isNotEmpty() })
        } catch (e: Exception) {
            Log.e(TAG, "Scan directory failed: $folderPath", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    override fun scanArchiveWithProgress(
        archivePath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Flow<ScanEvent> = flow {
        try {
            val archiveName = File(archivePath).name

            // 1. 检查加密状态
            emit(ScanEvent.Progress(step = ScanStep.CHECKING_ENCRYPTION))
            val isEncrypted =
                when (val encryptedResult = archiveService.isEncrypted(archivePath)) {
                    is Result.Success -> encryptedResult.data
                    is Result.Error -> {
                        emit(ScanEvent.Error(encryptedResult.error))
                        return@flow
                    }
                }

            // 2. 读取文件列表
            emit(ScanEvent.Progress(step = ScanStep.LISTING_FILES))
            val filesResult = archiveService.listFiles(archivePath)
            if (filesResult is Result.Error) {
                emit(ScanEvent.Error(filesResult.error))
                return@flow
            }
            val filePaths = (filesResult as Result.Success).data

            emit(
                ScanEvent.Progress(
                    step = ScanStep.ANALYZING_FILES,
                    total = filePaths.size,
                    subProgress = 0f
                )
            )

            // 3. 识别 MOD（传入加密标志和进度回调）
            val mods =
                identifyMods(
                    sourcePath = archivePath,
                    allFilePaths = filePaths,
                    gameInfo = gameInfo,
                    isZip = true,
                    isEncrypted = isEncrypted,
                    gameFilesByDir = gameFilesByDir,
                    onProgress = { current, total, fileName ->
                        // Log.d(TAG, "Scanning $archiveName: $current/$total $fileName")
                        val progress = current.toFloat() / total
                        emit(
                            ScanEvent.Progress(
                                step = ScanStep.ANALYZING_FILES,
                                currentFile = fileName,
                                current = current,
                                total = total,
                                subProgress = progress
                            )
                        )
                    },
                    onModFound = { mod ->
                        // Log.d(TAG, "Found mod $archiveName: $mod")
                        emit(ScanEvent.ModFound(mod))
                    }
                )

            // 4. 逐个发射发现的 MOD
            val validMods = mods.filter { it.modFiles.isNotEmpty() }
//            for ((index, mod) in validMods.withIndex()) {
//                emit(ScanEvent.ModFound(mod))
//            }

            // 5. 完成
            emit(ScanEvent.Complete(validMods))
        } catch (e: Exception) {
            Log.e(TAG, "Scan archive with progress failed: $archivePath", e)
            emit(ScanEvent.Error(AppError.Unknown(e)))
        }
    }

    override suspend fun isModSource(path: String, gameInfo: GameInfoBean): Result<Boolean> {
        return try {
            val result =
                if (File(path).isDirectory) {
                    scanDirectoryMod(path, gameInfo)
                } else {
                    scanArchive(path, gameInfo)
                }
            Result.Success(result is Result.Success && result.data.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override fun scanDirectoryModWithProgress(
        folderPath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Flow<ScanEvent> = flow {
        try {
            val folderName = File(folderPath).name

            emit(ScanEvent.Progress(step = ScanStep.CHECKING_FOLDER))
            val rootFile = File(folderPath)
            if (!rootFile.exists() || !rootFile.isDirectory) {
                emit(ScanEvent.Error(AppError.FileError.FileNotFound(folderPath)))
                return@flow
            }

            // 遍历文件夹获取所有文件的相对路径
            emit(ScanEvent.Progress(step = ScanStep.LISTING_FILES))
            val filePaths = mutableListOf<String>()
            rootFile.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.toRelativeString(rootFile).replace("\\", "/")
                    filePaths.add(relativePath)
                }
            }

            emit(
                ScanEvent.Progress(
                    step = ScanStep.ANALYZING_FILES,
                    total = filePaths.size,
                    subProgress = 0f
                )
            )

            // 识别 MOD（文件夹不加密，传入进度回调）
            val mods =
                identifyMods(
                    sourcePath = folderPath,
                    allFilePaths = filePaths,
                    gameInfo = gameInfo,
                    isZip = false,
                    isEncrypted = false,
                    gameFilesByDir = gameFilesByDir,
                    onProgress = { current, total, fileName ->
                        val progress = current.toFloat() / total
                        emit(
                            ScanEvent.Progress(
                                step = ScanStep.ANALYZING_FILES,
                                currentFile = fileName,
                                current = current,
                                total = total,
                                subProgress = progress
                            )
                        )
                    },
                    onModFound = { mod ->
                        emit(ScanEvent.ModFound(mod))
                    }
                )

            // 逐个发射发现的 MOD
            val validMods = mods.filter { it.modFiles.isNotEmpty() }
//            for ((index, mod) in validMods.withIndex()) {
//                emit(ScanEvent.ModFound(mod))
//            }

            emit(ScanEvent.Complete(validMods))
        } catch (e: Exception) {
            Log.e(TAG, "Scan directory with progress failed: $folderPath", e)
            emit(ScanEvent.Error(AppError.Unknown(e)))
        }
    }

    /**
     * 识别 MOD A模式 (isGameFileRepeat=true): 按游戏目录的完整路径前缀分组，验证文件存在于游戏目录 B模式 (isGameFileRepeat=false):
     * 按包含游戏匹配文件的目录分组，通过文件名匹配游戏目录
     * @param isEncrypted 是否加密，加密时不提取图片到缓存
     * @param onProgress 可选的进度回调，参数为 (当前索引, 总数, 当前文件名)
     */
    private suspend fun identifyMods(
        sourcePath: String,
        allFilePaths: List<String>,
        gameInfo: GameInfoBean,
        isZip: Boolean,
        isEncrypted: Boolean = false,
        gameFilesByDir: Map<String, Set<String>>? = null,
        onProgress: (suspend (Int, Int, String) -> Unit)? = null,
        onModFound: (suspend (ModBean) -> Unit)? = null
    ): List<ModBean> {
        val sourceName = File(sourcePath).nameWithoutExtension

        // 使用传入的映射，如果未提供则内部加载
        val gameFilesMap = gameFilesByDir ?: loadGameFilesMap(gameInfo)

        return if (gameInfo.isGameFileRepeat) {
            identifyModsAMode(
                sourcePath,
                sourceName,
                allFilePaths,
                gameInfo,
                isZip,
                isEncrypted,
                gameFilesMap,
                onProgress,
                onModFound
            )
        } else {
            identifyModsBMode(
                sourcePath,
                sourceName,
                allFilePaths,
                gameInfo,
                isZip,
                isEncrypted,
                gameFilesMap,
                onProgress,
                onModFound
            )
        }
    }

    /**
     * 加载游戏目录的文件名映射
     * @return Map<游戏目录名, Set<文件名>>
     */
    private suspend fun loadGameFilesMap(gameInfo: GameInfoBean): Map<String, Set<String>> {
        val result = mutableMapOf<String, Set<String>>()

        for (gameDir in gameInfo.gameFilePath) {
            val cleanDir = gameDir.trimEnd('/')
            // val fullPath = "${gameInfo.gamePath}/$cleanDir"

            val filesResult = fileService.getFileNames(gameDir)
            if (filesResult is Result.Success) {
                result[cleanDir] = filesResult.data.toSet()
            } else {
                // 目录不存在或无法访问，使用空集合
                result[cleanDir] = emptySet()
            }
        }

        return result
    }

    /** A模式：通过游戏目录名识别 验证：压缩包中的文件必须能映射到游戏目录中存在的文件 */
    private suspend fun identifyModsAMode(
        sourcePath: String,
        sourceName: String,
        allFilePaths: List<String>,
        gameInfo: GameInfoBean,
        isZip: Boolean,
        isEncrypted: Boolean,
        gameFilesByDir: Map<String, Set<String>>,
        onProgress: (suspend (Int, Int, String) -> Unit)? = null,
        onModFound: (suspend (ModBean) -> Unit)? = null
    ): List<ModBean> {
        // modKey (路径前缀+游戏目录) -> (modFiles列表, gameFilesPath列表, modType索引)
        data class ModGroup(
            val modFiles: MutableList<String> = mutableListOf(),
            val gameFilesPath: MutableList<String> = mutableListOf(),
            val typeIndex: Int
        )
        Log.d(TAG, "当前游戏文件: $gameFilesByDir")

        val modGroups = mutableMapOf<String, ModGroup>()
        val totalFiles = allFilePaths.size

        for ((fileIndex, filePath) in allFilePaths.withIndex()) {
            // 发射进度 (每处理 10% 发射一次，避免过于频繁)
//            if (onProgress != null && (fileIndex % maxOf(1, totalFiles / 100) == 0 || fileIndex == totalFiles - 1)) {
//                onProgress(fileIndex + 1, totalFiles, filePath.substringAfterLast("/"))
//            }

            // 跳过非游戏文件（图片、文档等）
            if (isNonGameFile(filePath)) continue

            // 查找该文件属于哪个游戏目录
            for ((index, gameDirSource) in gameInfo.gameFilePath.withIndex()) {
                // 如果gameDir以/结尾，则去除尾部斜杠
                val cleanGameDir = gameDirSource.trimEnd('/')

                val gameDirName = fileService.getFileName(cleanGameDir)
                Log.i(TAG, "当前gameDirSource: $gameDirSource")
                Log.i(TAG, "当前gameDirName: $gameDirName")
                Log.i(TAG, "当前cleanGameDir: $cleanGameDir")

                // 检查路径是否包含游戏目录

              //  Log.d(TAG, "$filePath 文件是否包含游戏目录: ${gameDirIndex != -1}")
                if (File(filePath).parentFile?.name == gameDirName) {
                    val gameDirIndex = filePath.indexOf(gameDirName, ignoreCase = true)
                    // 提取相对于游戏目录的文件路径
                    // 例如: "皮肤/人物1/Custom/abc.dat" -> "abc.dat"
                    val relativeToGameDir =
                        filePath.substring(gameDirIndex + gameDirName.length).trimStart('/')
                    val fileName = relativeToGameDir.substringAfterLast("/")
                    Log.d(TAG, "当前relativeToGameDir: $relativeToGameDir")
                    Log.d(TAG, "当前fileName: $fileName")


                    // 验证：检查游戏目录中是否存在此文件
                    val gameFiles = gameFilesByDir[cleanGameDir] ?: emptySet()
                    Log.d(TAG, "当前gameFiles大小: ${gameFiles.size}")
                    gameFilesByDir.forEach {
                        Log.d(TAG, "当前gameFiles: ${it.key} 包含文件数量：￥${it.value.size}")

                    }

                    val fileExistsInGame =
                        gameFiles.isNotEmpty() && // 如果无法获取游戏文件列表，不允许通过
                                gameFiles.any { it.equals(fileName, ignoreCase = true) }
                    Log.d(TAG, "是否存在于游戏目录: $fileExistsInGame")
                    if (fileExistsInGame) {
                        // modKey = 游戏目录之前的完整路径 + 游戏目录
                        // 示例 1: "皮肤/人物1/Custom/abc.dat" -> "皮肤/人物1/Custom"
                        val modKey = filePath.substring(0, gameDirIndex + gameDirName.length)

                        // 计算游戏目标路径
                        val gameFilePath = "${cleanGameDir}/$relativeToGameDir".replace("//", "/")

                        val group = modGroups.getOrPut(modKey) { ModGroup(typeIndex = index) }
                        group.modFiles.add(filePath)
                        group.gameFilesPath.add(gameFilePath)
                    }
                    break // 一个文件只属于一个游戏目录
                }
            }
        }
        Log.d(TAG, "最终mod分组 Mod groups: $modGroups")
        var modIndex = 0
        // 转换为 ModBean 列表
        val mods =
            modGroups.map { (modKey, group) ->
                // modFiles 只包含游戏文件，与 gameFilesPath 一一对应
                modIndex++
                // 发射进度 (每处理 5% 发射一次，避免过于频繁)
                if (onProgress != null) {
                    onProgress(modIndex, modGroups.size, modKey)
                }
                val mod = createModBeanWithGameFiles(
                    sourcePath = sourcePath,
                    sourceName = sourceName,
                    modRelativePath = modKey,
                    modFiles = group.modFiles, // 只传游戏文件
                    gameFilesPath = group.gameFilesPath,
                    modType = gameInfo.modType.getOrElse(group.typeIndex) { "Unknown" },
                    isZip = isZip,
                    isEncrypted = isEncrypted,
                    gameInfo = gameInfo,
                    allFilePaths = allFilePaths, // 传递所有文件用于提取图片/readme
                    onProgress = onProgress
                )
                onModFound?.invoke(mod)
                mod
            }

        // 设置整合包的 virtualPaths（使用 modRelativePath）
        return if (mods.size > 1) {
            mods.map { mod ->
                val relative = mod.modRelativePath ?: ""
                val virtualPath = if (relative.isNotEmpty()) "$sourcePath/$relative" else sourcePath
                mod.copy(virtualPaths = virtualPath)
            }
        } else {
            mods
        }
    }

    /** B模式：不需要游戏目录名 通过文件名与游戏目录中的实际文件进行匹配 */
    private suspend fun identifyModsBMode(
        sourcePath: String,
        sourceName: String,
        allFilePaths: List<String>,
        gameInfo: GameInfoBean,
        isZip: Boolean,
        isEncrypted: Boolean,
        gameFilesByDir: Map<String, Set<String>>,
        onProgress: (suspend (Int, Int, String) -> Unit)? = null,
        onModFound: (suspend (ModBean) -> Unit)?
    ): List<ModBean> {
        // modKey (父目录) -> (modFiles列表, gameFilesPath列表, matchedGameDir)
        data class ModGroup(
            val modFiles: MutableList<String> = mutableListOf(),
            val gameFilesPath: MutableList<String> = mutableListOf(),
            var matchedGameDir: String = "",
            var typeIndex: Int = 0
        )

        val modGroups = mutableMapOf<String, ModGroup>()
        val totalFiles = allFilePaths.size

        for ((fileIndex, filePath) in allFilePaths.withIndex()) {
            // 发射进度 (每处理 5% 发射一次，避免过于频繁)
//            if (onProgress != null && (fileIndex % maxOf(1, totalFiles / 100) == 0 || fileIndex == totalFiles - 1)) {
//                onProgress(fileIndex + 1, totalFiles, filePath.substringAfterLast("/"))
//            }

            // 跳过非游戏文件
            if (isNonGameFile(filePath)) continue

            val fileName = filePath.substringAfterLast("/")
            // 对于根目录文件，parentDir 为空字符串，使用特殊标记
            val parentDir = filePath.substringBeforeLast("/", "").ifEmpty { ROOT_DIR_KEY }

            // 在所有游戏目录中查找匹配的文件名
            for ((index, gameDir) in gameInfo.gameFilePath.withIndex()) {
                val cleanGameDir = gameDir.trimEnd('/')
                val gameFiles = gameFilesByDir[cleanGameDir] ?: continue

                if (gameFiles.any { it.equals(fileName, ignoreCase = true) }) {
                    val gameFilePath = "${gameDir}/$fileName".replace("//", "/")

                    val group =
                        modGroups.getOrPut(parentDir) {
                            ModGroup(matchedGameDir = cleanGameDir, typeIndex = index)
                        }
                    group.modFiles.add(filePath)
                    group.gameFilesPath.add(gameFilePath)
                    break // 找到匹配就停止
                }
            }
        }

        // 合并子目录
        //  val finalGroups = consolidateModGroups(modGroups)
        var modIndex = 0
        // 转换为 ModBean 列表
        val mods =
            modGroups.map { (modKey, group) ->
                modIndex++
                // 发射进度 (每处理 5% 发射一次，避免过于频繁)
                if (onProgress != null) {
                    onProgress(modIndex, modGroups.size, modKey)
                }
                // modFiles 只包含游戏文件，与 gameFilesPath 一一对应
                val mod = createModBeanWithGameFiles(
                    sourcePath = sourcePath,
                    sourceName = sourceName,
                    modRelativePath = modKey,
                    modFiles = group.modFiles, // 只传游戏文件
                    gameFilesPath = group.gameFilesPath.toList(),
                    modType = gameInfo.modType.getOrElse(group.typeIndex) { "Default" },
                    isZip = isZip,
                    isEncrypted = isEncrypted,
                    gameInfo = gameInfo,
                    allFilePaths = allFilePaths,
                    onProgress = onProgress
                    // 传递所有文件用于提取图片/readme
                )
                onModFound?.invoke(mod)
                mod
            }

        // 设置整合包的 virtualPaths（使用 modRelativePath）
        return if (mods.size > 1) {
            mods.map { mod ->
                val relative = mod.modRelativePath ?: ""
                val virtualPath = if (relative.isNotEmpty()) "$sourcePath/$relative" else sourcePath
                mod.copy(virtualPaths = virtualPath)
            }
        } else {
            mods
        }
    }

    /** 判断是否为非游戏文件（图片、文档等） */
    private fun isNonGameFile(path: String): Boolean {
        val ext = path.substringAfterLast(".", "").lowercase()
        return ext in NON_GAME_FILE_EXTENSIONS
    }

    /** 合并 MOD 分组：移除被子目录包含的父目录 */
    private fun <T> consolidateModGroups(groups: Map<String, T>): Map<String, T> {
        val sortedKeys = groups.keys.sortedByDescending { it.count { c -> c == '/' } }
        val result = mutableMapOf<String, T>()
        val processedDirs = mutableSetOf<String>()

        for (dir in sortedKeys) {
            val isParentOfProcessed = processedDirs.any { it.startsWith("$dir/") }
            if (!isParentOfProcessed) {
                groups[dir]?.let { result[dir] = it }
                processedDirs.add(dir)
            }
        }

        return result
    }

    /**
     * 创建 ModBean (使用已验证的游戏文件路径)
     * @param isEncrypted 是否加密，加密时图片保持相对路径不提取，description = "mod已加密"
     */
    private suspend fun createModBeanWithGameFiles(
        sourcePath: String,
        sourceName: String,
        modRelativePath: String,
        modFiles: List<String>,
        gameFilesPath: List<String>,
        modType: String,
        isZip: Boolean,
        isEncrypted: Boolean,
        gameInfo: GameInfoBean,
        allFilePaths: List<String>, // 所有文件路径，用于提取图片和readme
        onProgress: (suspend (Int, Int, String) -> Unit)?
    ): ModBean {
        val name = generateModName(sourceName, modRelativePath)

        // modFiles 处理：压缩包存相对路径，文件夹存完整路径
        val finalFiles = if (isZip) modFiles else modFiles.map { "$sourcePath/$it" }

        // path 处理：压缩包为压缩包路径，文件夹为 MOD 完整目录路径
        val modPath =
            if (isZip) {
                sourcePath
            } else {
                if (modRelativePath.isNotEmpty() && modRelativePath != ROOT_DIR_KEY) {
                    "$sourcePath/$modRelativePath".replace("//", "/")
                } else {
                    sourcePath
                }
            }

        // 提取该 MOD 目录下的图片和 readme
        val modDirFiles =
            allFilePaths.filter {
                if (modRelativePath == ROOT_DIR_KEY) {
                    File(it).parent.isNullOrEmpty()
                } else {
                    /*it.startsWith("$modRelativePath/") ||
                            (modRelativePath.isEmpty() && !it.contains("/"))*/
                    File(it).parent == modRelativePath
                }
            }
        // 提取根目录readme
        val rootReadmeFile = allFilePaths.find {
            //if (it.contains("readme")) Log.d(TAG,"当前mod: $name, 是否包含rootReadmeFile: ${it}")

            isReadmeFile(it) && File(it).parent.isNullOrEmpty()
        }
        // Log.d(TAG,"当前mod: $name, 是否包含rootReadmeFile: ${rootReadmeFile}")
        val imageFiles = modDirFiles.filter { isImageFile(it) }
        val readmeFile = modDirFiles.find { isReadmeFile(it) }

        // 处理图片路径
        val iconPath: String?
        val imagePaths: List<String>?

        if (imageFiles.isNotEmpty()) {
            if (isZip && isEncrypted) {
                // 加密压缩包：保持相对路径，不提取
                iconPath = imageFiles.first()
                imagePaths = imageFiles
            } else if (isZip) {
               // iconPath = imageFiles.first()
                //imagePaths = imageFiles
                // 不解压测试
                // 未加密压缩包：提取到缓存目录
                iconPath =
                    extractImageToCache(
                        sourcePath,
                        imageFiles.first(),
                        isIcon = true,
                        isExtract = false,
                        modName = name
                    )
                imagePaths =
                    imageFiles
                        .mapNotNull {
                            extractImageToCache(
                                sourcePath,
                                it,
                                isIcon = false,
                                isExtract = false,
                                modName = name
                            )
                        }
                        .ifEmpty { null }
            } else {
                // 文件夹：直接使用完整路径
                iconPath = "$sourcePath/${imageFiles.first()}"
                imagePaths = imageFiles.map { "$sourcePath/$it" }
            }
        } else {
            iconPath = null
            imagePaths = null
        }

        // 处理 readme 路径和 description
        val rootReadmePath: String?
        val readmePath: String?
        var description: String
        val rootDescription: String?

        if (isEncrypted) {
            // 加密压缩包：保持路径，description 标记为加密
            readmePath = readmeFile
            rootReadmePath = rootReadmeFile
            description = "mod已加密"
        } else if (isZip && (readmeFile != null || rootReadmeFile != null)) {
            readmePath = readmeFile
            rootReadmePath = rootReadmeFile
            // 未加密压缩包：提取 readme 文本内容到 description
            /*readmePath = readmeFile
            rootReadmePath = rootReadmeFile
            description = readmeFile?.let { extractReadmeContent(sourcePath, it) } ?: ""
            rootDescription = rootReadmeFile?.let { extractReadmeContent(sourcePath, it) } ?: ""
            if (description.isEmpty()) description = rootDescription
            */
            description = "正在读取中..."

        } else if (!isZip && (readmeFile != null || rootReadmeFile != null)) {
            // 文件夹：读取 readme 文件内容
            readmePath = "$sourcePath/$readmeFile"
            rootReadmePath = "$sourcePath/$rootReadmeFile"
            description = if (readmeFile != null) {
                try {
                   File(readmePath).readText().take(2000)
                    //Log.d(TAG,"当前mod: $name, 是否包含readmeFile: ${text}")

                } catch (e: Exception) {
                    ""
                }
            } else {
                try {
                    File(rootReadmePath).readText().take(2000)
                   // Log.d(TAG,"当前mod: $name, 是否包含filereadmeFile: ${text}")

                } catch (e: Exception) {
                    ""
                }
            }

        } else {
            readmePath = null
            rootReadmePath = null
            description = ""
        }
        val virtualPath =
            if (modRelativePath.isNotEmpty()) "$sourcePath/$modRelativePath" else sourcePath
        //Log.d(TAG,"当前mod: $name, 描述文本: ${description}")
        return ModBean(
            name = name.replace("($ROOT_DIR_KEY)",""),
            path = modPath,
            modFiles = finalFiles,
            gameFilesPath = gameFilesPath,
            modType = modType,
            modForm = ModForm.TRADITIONAL,
            isZipFile = isZip,
            isEncrypted = isEncrypted,
            date = File(sourcePath).lastModified(),
            gamePackageName = gameInfo.packageName,
            isEnable = false,
            icon = iconPath,
            images = imagePaths,
            readmePath = readmePath,
            fileReadmePath = rootReadmePath,
            description = description,
            virtualPaths = virtualPath,
            modRelativePath = modRelativePath.ifEmpty { null } // 存储相对路径
        )
    }

    /** 从压缩包提取图片到缓存目录（流式提取 + WebP 压缩） */
    private suspend fun extractImageToCache(
        archivePath: String,
        entryPath: String,
        isIcon: Boolean,
        // 是否提取
        isExtract: Boolean,
        modName: String
    ): String? {
        val cacheDir = if (isIcon) PathConstants.MODS_ICON_PATH else PathConstants.MODS_IMAGE_PATH
        val fileName = generateImageFileName(archivePath, entryPath)
        // 使用 .webp 扩展名
        val targetPath = "$cacheDir$fileName.webp"
        val targetFile = File(targetPath)
        if (!isExtract) return targetPath
        // 确保目录存在
        targetFile.parentFile?.mkdirs()

        // 如果已存在则直接返回
        if (targetFile.exists()) return targetPath

        // 1. 获取图片流
        val inputStreamResult = archiveService.getFileInputStream(archivePath, entryPath)
        if (inputStreamResult is Result.Error) {
            Log.w(TAG, "Failed to get input stream for $entryPath: ${inputStreamResult.error}")
            return null
        }

        val inputStream = (inputStreamResult as Result.Success).data

        return try {
            inputStream.use { stream ->
                // 2. 解码图片
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap == null) {
                    Log.w(TAG, "Failed to decode image: $entryPath")
                    return@use null
                }

                // 3. 压缩为 WebP 并写入文件
                FileOutputStream(targetFile).use { fos ->
                    val compressed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, fos)
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 80, fos)
                    }

                    bitmap.recycle()

                    if (compressed) targetPath else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract and compress image: $entryPath", e)
            null
        } finally {
            archiveService.clearTempDirectory()
        }
    }

/*    private fun generateImageFileName(modName: String, imagePath: String): String {
        val hash = (modName + imagePath).hashCode().toUInt().toString(16)
        val ext = imagePath.substringAfterLast(".", "png")
        return "${modName.replace("[^a-zA-Z0-9]".toRegex(), "_")}_$hash.$ext"
    }*/

    /**
     * 生成可还原的图片缓存文件名
     *
     * 格式：{base64EncodedPath}
     * 使用 URL-safe Base64 编码，可通过 decodeImageFileName 还原原始路径
     *
     * @param modName MOD 名称
     * @param imagePath 压缩包内的相对路径
     * @return 缓存文件名（不含扩展名）
     */
    private fun generateImageFileName(archivePath: String, imagePath: String): String {
        // 使用 URL-safe Base64 编码，替换特殊字符以确保文件名合法
        val safeModName = File(archivePath).nameWithoutExtension +"/" + imagePath
        return safeModName
    }

    /** 从压缩包提取 readme 文本内容 */
    private suspend fun extractReadmeContent(archivePath: String, entryPath: String): String? {
        val inputStreamResult = archiveService.getFileInputStream(archivePath, entryPath)
        if (inputStreamResult is Result.Error) {
            Log.w(
                TAG,
                "Failed to get readme input stream for $entryPath: ${inputStreamResult.error}"
            )
            return null
        }

        val inputStream = (inputStreamResult as Result.Success).data

        return try {
            inputStream.bufferedReader().use { reader ->
                // 限制读取长度，避免过大的文件
                reader.readText().take(2000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract readme content: $entryPath", e)
            null
        }
    }

    /** 生成 MOD 名称 规则：sourceName(路径1|路径2|...) */
    private fun generateModName(sourceName: String, modRelativePath: String): String {
        val parts = modRelativePath.split("/").filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> sourceName
            else -> "$sourceName(${parts.joinToString("|")})"
        }
    }



    // ==================== 工具方法 ====================

    private fun isImageFile(path: String): Boolean {
        val ext = path.substringAfterLast(".", "").lowercase()
        return ext in listOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
    }

    private fun isReadmeFile(path: String): Boolean {
        val name = path.substringAfterLast("/").lowercase()
        return name.startsWith("readme") || name == "说明.txt" || name.endsWith(".txt")
    }
}
