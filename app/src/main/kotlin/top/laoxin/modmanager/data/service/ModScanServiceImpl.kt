package top.laoxin.modmanager.data.service

import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ModForm
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.model.ScanEvent
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.ModScanService
import top.laoxin.modmanager.domain.service.TraditionalModScanService

/** MOD 扫描服务调度实现 自动检测 MOD 类型并调度到对应的扫描服务 */
class ModScanServiceImpl
@Inject
constructor(
    private val traditionalService: TraditionalModScanService,
    // TODO: 待实现后注入
    // private val activeService: ActiveModScanService,
    // private val packagedService: PackagedModScanService,
    private val archiveService: ArchiveService,
    private val fileService: FileService
) : ModScanService {

    companion object {
        const val TAG = "ModScanServiceImpl"

        // ACTIVE 类型配置文件名
        private const val ACTIVE_CONFIG_FILE = "mod.json"

        // PACKAGED 类型配置文件名
        private const val PACKAGED_CONFIG_FILE = "package.json"
    }

    override suspend fun loadGameFilesMap(
        gameInfo: GameInfoBean
    ): Result<Map<String, Set<String>>> {
        return try {
            val result = mutableMapOf<String, Set<String>>()

            for (gameDir in gameInfo.gameFilePath) {
                val cleanDir = gameDir.trimEnd('/')

                val filesResult = fileService.getFileNames(gameDir)
                if (filesResult is Result.Success) {
                    result[cleanDir] = filesResult.data.toSet()
                } else {
                    // 目录不存在或无法访问，使用空集合
                    result[cleanDir] = emptySet()
                }
            }

            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun scan(
        path: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Result<List<ModBean>> {
        // 使用 FileService 检查是否为文件
        val isFileResult = fileService.isFile(path)
        val isFile =
            when (isFileResult) {
                is Result.Success -> isFileResult.data
                is Result.Error -> false // 默认当作文件夹处理
            }

        return if (!isFile) {
            scanDirectoryMod(path, gameInfo, gameFilesByDir)
        } else {
            scanArchive(path, gameInfo, gameFilesByDir)
        }
    }

    override suspend fun scanArchive(
        archivePath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Result<List<ModBean>> {
        // 1. 检测 MOD 类型
        val formResult = detectModForm(archivePath)
        if (formResult is Result.Error) {
            return Result.Error(formResult.error)
        }
        val modForm = (formResult as Result.Success).data

        // 2. 根据类型调度到对应服务
        return when (modForm) {
            ModForm.TRADITIONAL ->
                traditionalService.scanArchive(archivePath, gameInfo, gameFilesByDir)

            ModForm.ACTIVE -> {
                // TODO: 实现后替换
                Result.Error(AppError.Unknown(Exception("ActiveModScanService not implemented")))
            }

            ModForm.PACKAGED -> {
                // TODO: 实现后替换
                Result.Error(AppError.Unknown(Exception("PackagedModScanService not implemented")))
            }
        }
    }

    override suspend fun scanDirectoryMod(
        folderPath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Result<List<ModBean>> {
        // 1. 检测 MOD 类型
        val formResult = detectModForm(folderPath)
        if (formResult is Result.Error) {
            return Result.Error(formResult.error)
        }
        val modForm = (formResult as Result.Success).data

        // 2. 根据类型调度到对应服务
        return when (modForm) {
            ModForm.TRADITIONAL ->
                traditionalService.scanDirectoryMod(folderPath, gameInfo, gameFilesByDir)

            ModForm.ACTIVE -> {
                // TODO: 实现后替换
                Result.Error(AppError.Unknown(Exception("ActiveModScanService not implemented")))
            }

            ModForm.PACKAGED -> {
                // TODO: 实现后替换
                Result.Error(AppError.Unknown(Exception("PackagedModScanService not implemented")))
            }
        }
    }

    override fun scanArchiveWithProgress(
        archivePath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Flow<ScanEvent> = flow {

        // 目前只支持 TRADITIONAL 类型，直接委托
        traditionalService.scanArchiveWithProgress(archivePath, gameInfo, gameFilesByDir)
            .collect { emit(it) }


    }.flowOn(Dispatchers.IO)

    override fun scanDirectoryModWithProgress(
        folderPath: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Flow<ScanEvent> = flow {
        // 目前只支持 TRADITIONAL 类型，直接委托
        traditionalService.scanDirectoryModWithProgress(folderPath, gameInfo, gameFilesByDir)
            .collect { emit(it) }

    }.flowOn(Dispatchers.IO)

    override suspend fun detectModForm(path: String): Result<ModForm> {
        return try {
            // 使用 FileService 检查是否为文件
            val isFileResult = fileService.isFile(path)
            val isFile = (isFileResult as? Result.Success)?.data ?: false

            val filePaths =
                if (!isFile) {
                    // 文件夹：使用 FileService 获取文件列表
                    getDirectoryFilePaths(path)
                } else {
                    // 压缩包：使用 archiveService 获取文件列表
                    when (val result = archiveService.listFiles(path)) {
                        is Result.Success -> result.data
                        is Result.Error -> return Result.Error(result.error)
                    }
                }

            // 检测配置文件
            val hasPackagedConfig =
                filePaths.any { it.endsWith(PACKAGED_CONFIG_FILE, ignoreCase = true) }
            val hasActiveConfig =
                filePaths.any { it.endsWith(ACTIVE_CONFIG_FILE, ignoreCase = true) }

            val form =
                when {
                    hasPackagedConfig -> ModForm.PACKAGED
                    hasActiveConfig -> ModForm.ACTIVE
                    else -> ModForm.TRADITIONAL
                }

            Result.Success(form)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun isModSource(
        path: String,
        gameInfo: GameInfoBean,
        gameFilesByDir: Map<String, Set<String>>?
    ): Result<Boolean> {
        return try {
            // 使用 FileService 检查是否为文件
            val isFileResult = fileService.isFile(path)
            val isFile = (isFileResult as? Result.Success)?.data ?: false

            val filePaths =
                if (!isFile) {
                    // 文件夹：使用 FileService 获取文件列表
                    getDirectoryFilePaths(path)
                } else {
                    // 压缩包：使用 archiveService 获取文件列表
                    when (val result = archiveService.listFiles(path)) {
                        is Result.Success -> result.data
                        is Result.Error -> return Result.Success(false) // 无法读取则认为不是有效MOD
                    }
                }

            // 快速检查1：是否包含 JSON 配置文件（ACTIVE/PACKAGED 类型）
            val hasConfigFile =
                filePaths.any {
                    // Log.i("ModScanService", "isModSource: $it")
                    it.endsWith(ACTIVE_CONFIG_FILE, ignoreCase = true) ||
                            it.endsWith(PACKAGED_CONFIG_FILE, ignoreCase = true)
                }
            if (hasConfigFile) {
                return Result.Success(true)
            }

            // 快速检查2：是否包含与游戏目录中同名的文件（TRADITIONAL 类型）
            // 使用传入的或加载新的游戏文件映射
            val gameFilesMap =
                gameFilesByDir
                    ?: run {
                        val loadResult = loadGameFilesMap(gameInfo)
                        if (loadResult is Result.Success) loadResult.data else emptyMap()
                    }

            // 获取所有游戏文件名
            val gameFileNames = gameFilesMap.values.flatten().map { it.lowercase() }.toSet()

            // 检查压缩包中是否有与游戏文件同名的文件
            val hasGameFile =
                filePaths.any { filePath ->
                    val fileName = filePath.substringAfterLast("/").lowercase()
                    gameFileNames.contains(fileName)
                }

            Result.Success(hasGameFile)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /** 获取文件夹内所有文件的相对路径列表 */
    private suspend fun getDirectoryFilePaths(dirPath: String): List<String> {
        val filesResult = fileService.listFiles(dirPath)
        if (filesResult is Result.Error) return emptyList()

        val files = (filesResult as Result.Success).data
        val result = mutableListOf<String>()
        val rootPath = File(dirPath).absolutePath

        for (file in files) {
            if (file.isFile) {
                val relativePath =
                    file.absolutePath
                        .removePrefix(rootPath)
                        .trimStart('/', '\\')
                        .replace("\\", "/")
                result.add(relativePath)
            } else if (file.isDirectory) {
                // 递归获取子目录文件
                val subFiles = getDirectoryFilePaths(file.absolutePath)
                val subDirPrefix =
                    file.absolutePath
                        .removePrefix(rootPath)
                        .trimStart('/', '\\')
                        .replace("\\", "/")
                subFiles.forEach { subFile ->
                    result.add("$subDirPrefix/$subFile".replace("//", "/"))
                }
            }
        }
        return result
    }
}
