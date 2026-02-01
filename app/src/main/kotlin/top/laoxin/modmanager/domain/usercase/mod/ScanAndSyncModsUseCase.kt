package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ScanFileBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.model.ScanEvent
import top.laoxin.modmanager.domain.model.ScanStep
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.repository.ScanFileRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.ModScanService
import top.laoxin.modmanager.domain.service.ModSourcePrepareService
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.service.TransferEvent
import top.laoxin.modmanager.domain.service.TransferResult

/** 扫描状态密封类 使用 Flow 发射各个扫描阶段的状态 */
sealed class ScanState {
    /** 准备阶段 */
    data class Preparing(val message: String) : ScanState()

    /** 进度更新 */
    data class Progress(
        val step: ScanStep, // 扫描步骤枚举（用于 i18n）
        val sourceName: String = "", // 当前扫描的压缩包/文件夹名
        val currentFile: String = "", // 当前正在处理的文件名
        val current: Int = 0, // 当前进度值
        val total: Int = 0, // 总数
        val overallPercent: Float = 0f, // 总体进度百分比
        val subProgress: Float = -1f // 子进度百分比
    ) : ScanState()

    /** 发现一个 MOD（实时反馈） */
    data class ModFound(val mod: ModBean) : ScanState()

    /** 转移完成 */
    data class TransferComplete(val result: TransferResult) : ScanState()

    /** 扫描完成（进入同步阶段） */
    data class ScanComplete(val modsCount: Int) : ScanState()

    /** 完成 */
    data class Success(val result: ScanSyncResult) : ScanState()

    /** 错误 */
    data class Error(val error: AppError) : ScanState()
}

/** 扫描并同步 MOD 用例 完整流程：鉴权 → 转移外部MOD → 扫描 → 同步到数据库 */
class ScanAndSyncModsUseCase
@Inject
constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val permissionService: PermissionService,
    private val prepareService: ModSourcePrepareService,
    private val scanService: ModScanService,
    private val modRepository: ModRepository,
    private val fileService: FileService,
    private val scanFileRepository: ScanFileRepository
) {
    companion object {
        private const val TAG = "ScanAndSyncModsUseCase"

        // 支持的压缩包扩展名
        private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z")
    }

    /**
     * 执行完整的扫描同步流程
     * @param gameInfo 游戏配置信息
     * @param scanExternalDirs 是否扫描外部目录（QQ、Download）
     * @return Flow<ScanState> 扫描状态流
     */
    fun execute(scanExternalDirs: Boolean = true, forceScan: Boolean): Flow<ScanState> = flow {
        try {
            val gameInfo = userPreferencesRepository.selectedGameValue
            Log.d(TAG, "Starting scan and sync for ${gameInfo.packageName}")

            // 0. 游戏选择检查
            emit(ScanState.Preparing("检查游戏配置.11.."))
            if (gameInfo == GameInfoConstant.NO_GAME || gameInfo.packageName.isEmpty()) {
                emit(ScanState.Error(AppError.GameError.GameNotSelected))
                return@flow
            }

            // 1. 鉴权检查和安装检测
            emit(ScanState.Preparing("检查权限..."))
            val authResult = permissionService.checkPathPermissions(gameInfo.gamePath)
            if (authResult is Result.Error) {
                emit(ScanState.Error(authResult.error))
                return@flow
            }
            // Log.d(TAG, "Permission check passed")

            fileService
                .listFiles(gameInfo.gamePath)
                .onSuccess {
                    // Log.d(TAG, "找到游戏路径：$it")
                    if (it.isEmpty())
                        return@flow emit(
                            ScanState.Error(
                                AppError.GameError.GameNotInstalled(
                                    gameInfo.gameName
                                )
                            )
                        )
                }
                .onError {
                    emit(ScanState.Error(it))
                    return@flow
                }

            Log.d(TAG, "开始检查权限o")



            // 获取配置路径
            val modPath =
                PathConstants.getFullModPath(
                    userPreferencesRepository.selectedDirectory.first()
                ) + "/${gameInfo.packageName}"

            val externalPaths: MutableList<String> = mutableListOf()
            if (userPreferencesRepository.scanQQDirectory.first()) {
                externalPaths.add(PathConstants.SCAN_PATH_QQ)
            }
            if (userPreferencesRepository.scanDownload.first()) {
                externalPaths.add(PathConstants.SCAN_PATH_DOWNLOAD)
            }
            externalPaths.add(
                PathConstants.getFullModPath(
                    userPreferencesRepository.selectedDirectory.first()
                )
            )

            // 2. 转移外部 MOD（如果启用）
            var transferResult: TransferResult? = null
            if (scanExternalDirs && externalPaths.isNotEmpty()) {
                emit(ScanState.Progress(step = ScanStep.TRANSFERRING, overallPercent = 0.1f))
                var baseOverallPercent = 0.1f
                prepareService.transferModSources(externalPaths, modPath, gameInfo).collect { event
                    ->
                    when (event) {
                        is TransferEvent.Scanning -> {
                            baseOverallPercent += (event.current.toFloat() / event.total) * 0.1f
                            emit(
                                ScanState.Progress(
                                    step = ScanStep.TRANSFERRING,
                                    currentFile = event.directory,
                                    current = event.current,
                                    total = event.total,
                                    subProgress = (event.current.toFloat() / event.total),
                                    overallPercent = baseOverallPercent
                                )
                            )
                        }

                        is TransferEvent.ScanningFile -> {
                            emit(
                                ScanState.Progress(
                                    step = ScanStep.TRANSFERRING,
                                    currentFile = event.fileName,
                                    current = event.current,
                                    total = event.total,
                                    overallPercent = baseOverallPercent,
                                    subProgress = (event.current.toFloat() / event.total)
                                )
                            )
                        }

                        is TransferEvent.FileProgress -> {
                            emit(
                                ScanState.Progress(
                                    step = ScanStep.TRANSFERRING,
                                    currentFile = event.fileName,
                                    current = event.current,
                                    total = event.total,
                                    overallPercent = baseOverallPercent,
                                    subProgress = (event.current.toFloat() / event.total)
                                )
                            )
                        }

                        is TransferEvent.Complete -> {
                            transferResult = event.result
                            emit(ScanState.TransferComplete(event.result))
                            Log.d(
                                TAG,
                                "Transferred ${event.result.transferredCount} MODs from external dirs"
                            )
                        }

                        is TransferEvent.Error -> {
                            Log.e(TAG, "Transfer error: ${event.err}")
                        }
                    }
                }
            }

            // 3. 扫描 MOD 目录
            emit(ScanState.Progress(step = ScanStep.SCANNING_DIRECTORY, overallPercent = 0.1f))
            val scannedMods = mutableListOf<ModBean>()
            val scanDirectory = userPreferencesRepository.scanDirectoryMods.first()
            // 使用 collector 收集扫描结果并实时发射
            val skippedPaths =
                scanModDirectoryWithEmit(modPath, gameInfo, scanDirectory, forceScan) { state ->
                    // Log.d(TAG, "Scan state 收集扫描结果: $state")
                    emit(state)
                    if (state is ScanState.ModFound) {
                        scannedMods.add(state.mod)
                    }
                }

            emit(ScanState.ScanComplete(scannedMods.size))
            Log.d(TAG, "Scanned ${scannedMods.size} MODs")

            // 4. 同步到数据库
            emit(ScanState.Progress(step = ScanStep.SYNCING_DATABASE, overallPercent = 0.9f))
            val syncResult = syncToDatabase(scannedMods, gameInfo, skippedPaths)
            Log.d(
                TAG,
                "Sync complete: added=${syncResult.addedCount}, updated=${syncResult.updatedCount}, deleted=${syncResult.deletedCount}"
            )

            // 5. 清理不存在的扫描文件记录
            val cleanedCount = cleanupNonExistentScanRecords()
            if (cleanedCount > 0) {
                Log.d(TAG, "Cleaned up $cleanedCount non-existent scan file records")
            }

            // 6. 完成
            emit(
                ScanState.Success(
                    ScanSyncResult(
                        scannedCount = scannedMods.size,
                        addedCount = syncResult.addedCount,
                        updatedCount = syncResult.updatedCount,
                        deletedCount = syncResult.deletedCount,
                        skippedCount = skippedPaths.size,
                        transferredCount = transferResult?.transferredCount ?: 0,
                        deletedEnabledMods = syncResult.deletedEnabledMods,
                        errors = (transferResult?.errors
                            ?: emptyList()) + syncResult.errors
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Scan and sync failed", e)
            emit(ScanState.Error(AppError.Unknown(e)))
        }
    }

    /** 扫描 MOD 目录并实时发射状态，返回跳过的路径集合 */
    private suspend fun scanModDirectoryWithEmit(
        modPath: String,
        gameInfo: GameInfoBean,
        scanDirectory: Boolean,
        forceScan: Boolean,
        emitter: suspend (ScanState) -> Unit
    ): Set<String> {
        // 1. 预加载游戏文件映射（只加载一次）
        val gameFilesMapResult = scanService.loadGameFilesMap(gameInfo)
        val gameFilesMap =
            if (gameFilesMapResult is Result.Success) {
                gameFilesMapResult.data
            } else {
                emptyMap()
            }

        // Log.d(TAG, "scanModDirectoryWithEmit: ${gameFilesMap}")
        // 2. 检查目录是否存在
        val dirExistsResult = fileService.isFileExist(modPath)
        if (dirExistsResult is Result.Error ||
            (dirExistsResult is Result.Success && !dirExistsResult.data)
        ) {
            Log.w(TAG, "Mod directory does not exist: $modPath")
            return emptySet()
        }

        // 3. 递归获取所有压缩包文件
        val archiveFilesResult = fileService.listFilesRecursively(modPath, ARCHIVE_EXTENSIONS)
        val archiveFiles =
            if (archiveFilesResult is Result.Success) {
                archiveFilesResult.data
            } else {
                emptyList()
            }

        // 4. 获取第一层目录（作为文件夹 MOD）
        val firstLevelDirsResult = fileService.listFirstLevelDirectories(modPath)
        val firstLevelDirs =
            if (firstLevelDirsResult is Result.Success) {
                firstLevelDirsResult.data
            } else {
                emptyList()
            }

        val totalItems = archiveFiles.size + if (scanDirectory) firstLevelDirs.size else 0
        var processedItems = 0
        val skippedPaths = mutableSetOf<String>() // 跳过的压缩包路径

        // 5. 扫描所有压缩包（使用 Flow 方式实时反馈进度）
        for (archive in archiveFiles) {
            val archiveName = archive.name
            processedItems++
            val baseProgress = 0.1f + (processedItems.toFloat() / totalItems) * 0.7f

            // 检查是否需要跳过
            if (!forceScan && shouldSkipScan(archive.absolutePath, gameInfo.packageName)) {
                Log.d(TAG, "Skipping already scanned archive: $archiveName")
                skippedPaths.add(archive.absolutePath)
                continue
            }

            emitter(
                ScanState.Progress(
                    step = ScanStep.ANALYZING_FILES,
                    sourceName = archiveName,
                    current = processedItems,
                    total = totalItems,
                    overallPercent = baseProgress
                )
            )

            try {
                scanService.scanArchiveWithProgress(archive.absolutePath, gameInfo, gameFilesMap)
                    .collect { event ->
                        when (event) {
                            is ScanEvent.Progress -> {
                                // Log.d(TAG, "scanModDirectoryWithEmit: $event")
                                // 计算当前项在总体进度中的范围
                                val itemProgressRange = 0.7f / totalItems
                                val calculatedProgress =
                                    if (event.subProgress >= 0) {
                                        baseProgress +
                                                (event.subProgress * itemProgressRange)
                                    } else {
                                        baseProgress
                                    }
                                emitter(
                                    ScanState.Progress(
                                        step = event.step,
                                        sourceName = archiveName,
                                        currentFile = event.currentFile,
                                        current = event.current,
                                        total = event.total,
                                        overallPercent = calculatedProgress,
                                        subProgress = event.subProgress
                                    )
                                )
                            }

                            is ScanEvent.ModFound -> {
                                emitter(ScanState.ModFound(event.mod))
                            }

                            is ScanEvent.Complete -> {
                                /* 单个文件完成 */
                            }

                            is ScanEvent.Error -> {
                                Log.e(TAG, "Scan archive error: ${event.error}")
                            }
                        }
                    }
                // 扫描成功后记录文件
                recordScannedFile(archive.absolutePath, gameInfo.packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan archive $archiveName", e)
            }
        }

        // 6. 如果开启扫描文件夹，扫描第一层目录（使用 Flow 方式实时反馈进度）
        if (scanDirectory) {
            for (dir in firstLevelDirs) {
                val dirName = dir.name
                processedItems++
                val baseProgress = 0.1f + (processedItems.toFloat() / totalItems) * 0.6f

                emitter(
                    ScanState.Progress(
                        step = ScanStep.ANALYZING_FILES,
                        sourceName = dirName,
                        current = processedItems,
                        total = totalItems,
                        overallPercent = baseProgress
                    )
                )

                try {
                    scanService.scanDirectoryModWithProgress(
                        dir.absolutePath,
                        gameInfo,
                        gameFilesMap
                    )
                        .collect { event ->
                            when (event) {
                                is ScanEvent.Progress -> {
                                    val itemProgressRange = 0.7f / totalItems
                                    val calculatedProgress =
                                        if (event.subProgress >= 0) {
                                            baseProgress +
                                                    (event.subProgress * itemProgressRange)
                                        } else {
                                            baseProgress
                                        }
                                    emitter(
                                        ScanState.Progress(
                                            step = event.step,
                                            sourceName = dirName,
                                            currentFile = event.currentFile,
                                            current = event.current,
                                            total = event.total,
                                            overallPercent = calculatedProgress,
                                            subProgress = event.subProgress
                                        )
                                    )
                                }

                                is ScanEvent.ModFound -> {
                                    emitter(ScanState.ModFound(event.mod))
                                }

                                is ScanEvent.Complete -> {
                                    /* 单个目录完成 */
                                }

                                is ScanEvent.Error -> {
                                    Log.e(TAG, "Scan directory error: ${event.error}")
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to scan directory $dirName", e)
                }
            }
        }

        return skippedPaths // 返回跳过的路径集合
    }

    /** 同步到数据库 比较扫描结果和现有数据，执行增删改 */
    private suspend fun syncToDatabase(
        scannedMods: List<ModBean>,
        gameInfo: GameInfoBean,
        skippedPaths: Set<String>
    ): DatabaseSyncResult {
        val errors = mutableListOf<String>()

        // 1. 获取数据库中现有的 MOD
        val existingMods = modRepository.getModsByGamePackageName(gameInfo.packageName).first()

        // 2. 构建索引：以 (path, name) 作为唯一标识
        val existingMap = existingMods.associateBy { "${it.path}|${it.name}|${it.virtualPaths}" }
        val scannedMap = scannedMods.associateBy { "${it.path}|${it.name}|${it.virtualPaths}" }

        // 3. 计算差异
        val toAdd = mutableListOf<ModBean>()
        val toUpdate = mutableListOf<ModBean>()
        val toDelete = mutableListOf<ModBean>()
        val deletedEnabledMods = mutableListOf<ModBean>()

        // 新增和更新
        for ((key, scannedMod) in scannedMap) {
            val existingMod = existingMap[key]
            if (existingMod == null) {
                // 新 MOD
                toAdd.add(scannedMod)
            } else {
                // 检查是否需要更新（比较关键字段）
                if (needsUpdate(existingMod, scannedMod)) {
                    // 保留用户设置的字段
                    toUpdate.add(
                        scannedMod.copy(
                            id = existingMod.id,
                            isEnable = existingMod.isEnable,
                            password = existingMod.password
                        )
                    )
                }
            }
        }

        // 删除：数据库中有但扫描结果中没有的
        // 注意：跳过的路径下的 MOD 不应该被删除
        for ((key, existingMod) in existingMap) {
            if (!scannedMap.containsKey(key)) {
                // 检查是否属于跳过的路径
                val isInSkippedPath = skippedPaths.any { existingMod.path.startsWith(it) }
                if (!isInSkippedPath) {
                    if (existingMod.isEnable) {
                        // 已启用的MOD物理文件已删除，收集起来（不从数据库删除）
                        deletedEnabledMods.add(existingMod)
                    } else {
                        // 未启用的MOD直接删除
                        toDelete.add(existingMod)
                    }
                }
            }
        }

        // 4. 执行数据库操作
        try {
            if (toAdd.isNotEmpty()) {
                modRepository.insertAll(toAdd)
            }
            if (toUpdate.isNotEmpty()) {
                modRepository.updateAll(toUpdate)
            }
            if (toDelete.isNotEmpty()) {
                modRepository.deleteAll(toDelete)
            }
        } catch (e: Exception) {
            errors.add("数据库操作失败: ${e.message}")
        }

        return DatabaseSyncResult(
            addedCount = toAdd.size,
            updatedCount = toUpdate.size,
            deletedCount = toDelete.size,
            errors = errors,
            deletedEnabledMods = deletedEnabledMods
        )
    }

    /** 检查 MOD 是否需要更新 */
    private fun needsUpdate(existing: ModBean, scanned: ModBean): Boolean {
        return existing.date != scanned.date ||
                existing.modFiles != scanned.modFiles ||
                existing.gameFilesPath != scanned.gameFilesPath ||
                existing.name != scanned.name ||
                existing.author != scanned.author ||
                existing.version != scanned.version ||
                existing.modConfig != scanned.modConfig ||
                if (existing.isEncrypted)
                    (existing.icon != scanned.icon || existing.description != scanned.description)
                            && existing.password.isEmpty()
                else existing.icon != scanned.icon 

    }

    /** 检查是否应该跳过扫描（非强制扫描时使用） */
    private suspend fun shouldSkipScan(absolutePath: String, gamePackageName: String): Boolean {
        val existingRecord =
            scanFileRepository.getByPathSync(absolutePath) ?: return false // 没有记录，需要扫描

        val lastModifyResult = fileService.getLastModified(absolutePath)
        if (lastModifyResult is Result.Error) return false
        val lastModify = (lastModifyResult as Result.Success).data
        // 快速检查：modifyTime 和 size
        val fileLengthResult : Result<Long> = fileService.getFileLength(absolutePath)
        if (fileLengthResult is Result.Error) return false
        val fileLength = (fileLengthResult as Result.Success).data
        if (existingRecord.modifyTime == lastModify && existingRecord.size == fileLength
        ) {
            return true // 文件未修改，跳过
        }

        // 文件可能被修改，计算 MD5 进一步验证
        val md5Result = fileService.calculateFileMd5(absolutePath)
        val currentMd5 = if (md5Result is Result.Success) md5Result.data else ""
        return currentMd5.isNotEmpty() && currentMd5 == existingRecord.md5
    }

    /** 记录已扫描的文件 */
    private suspend fun recordScannedFile(absolutePath: String, gamePackageName: String) {
        val md5Result = fileService.calculateFileMd5(absolutePath)
        val md5 = if (md5Result is Result.Success) md5Result.data else ""
        val lastModifiedResult = fileService.getLastModified(absolutePath)
        val lastModified = if (lastModifiedResult is Result.Success) lastModifiedResult.data else 0L
        val fileLengthResult = fileService.getFileLength(absolutePath)
        val fileLength = if (fileLengthResult is Result.Success) fileLengthResult.data else 0L
        val isFileResult = fileService.isFile(absolutePath)
        val isFile = if (isFileResult is Result.Success) isFileResult.data else false
        val scanFileBean =
            ScanFileBean(
                path = absolutePath,
                name = fileService.getFileName(absolutePath),
                modifyTime =lastModified,
                size =fileLength,
                isDirectory = !isFile,
                md5 = md5,
                gamePackageName = gamePackageName
            )
        // 先删除旧记录（如果存在），再插入新记录
        scanFileRepository.deleteByPath(absolutePath)
        scanFileRepository.insert(scanFileBean)
    }

    /**
     * 清理不存在的扫描文件记录
     * 检查跳过扫描的文件路径，如果物理文件不存在则从数据库中移除记录
     * @param skippedPaths 跳过扫描的文件路径集合
     * @return 清理的记录数量
     */
    private suspend fun cleanupNonExistentScanRecords(): Int {
       val skippedFiles = scanFileRepository.getAll().first()
        var cleanedCount = 0
        for (skippedFile in skippedFiles) {
            //Log.d(TAG, "Checking file exist: ${skippedFile.path}")
            val result = fileService.isFileExist( skippedFile.path)
            if (result is Result.Error) {
                Log.e(TAG, "Failed to check file exist: ${result.error}")
                continue
            }
            if (!(result as Result.Success).data) {
                // 物理文件不存在，删除扫描记录
                scanFileRepository.deleteByPath(skippedFile.path)
                cleanedCount++
                Log.d(TAG, "Removed non-existent scan record: ${skippedFile.path}")
            }
        }
        return cleanedCount
    }
}

/** 扫描同步结果 */
data class ScanSyncResult(
    val scannedCount: Int,
    val addedCount: Int,
    val updatedCount: Int,
    val deletedCount: Int,
    val skippedCount: Int = 0,
    val transferredCount: Int = 0,
    val errors: List<String> = emptyList(),
    /** 物理文件已删除但仍处于启用状态的MOD列表 */
    val deletedEnabledMods: List<ModBean> = emptyList()
)

/** 数据库同步结果（内部使用） */
private data class DatabaseSyncResult(
    val addedCount: Int,
    val updatedCount: Int,
    val deletedCount: Int,
    val errors: List<String> = emptyList(),
    /** 物理文件已删除但仍处于启用状态的MOD列表 */
    val deletedEnabledMods: List<ModBean> = emptyList()
)
