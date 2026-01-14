package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ReplacedFileBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.BackupRepository
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.repository.ReplacedFileRepository
import top.laoxin.modmanager.domain.service.BackupEvent
import top.laoxin.modmanager.domain.service.BackupService
import top.laoxin.modmanager.domain.service.EnableFileEvent
import top.laoxin.modmanager.domain.service.EnableStep
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.ModEnableService
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.service.SpecialGameService
import top.laoxin.modmanager.domain.service.ValidationResult
import kotlin.text.substringAfterLast

//import java.io.File

/** 批量开启/关闭 MOD 用例 负责权限检查、验证、备份、调用 Service、特殊处理、DB更新、结果聚合 */
class EnableModsUseCase
@Inject
constructor(
    private val modEnableService: ModEnableService,
    private val backupService: BackupService,
    private val specialGameService: SpecialGameService,
    private val modRepository: ModRepository,
    private val backupRepository: BackupRepository,
    private val permissionService: PermissionService,
    private val replacedFileRepository: ReplacedFileRepository,
    private val fileService: FileService
) {
    companion object {
        private const val TAG = "EnableModsUseCase"
    }


    // 取消任务标识
    private var isCanceled = false

    /**
     * 执行批量开启 MOD
     * @param mods 要开启的 MOD 列表
     * @param gameInfo 游戏信息
     * @return Flow<EnableState> 开启状态流
     */
    fun execute(modsToEnable: List<ModBean>, gameInfo: GameInfoBean): Flow<EnableState> = flow {
        val mods = modsToEnable.mapNotNull { modRepository.getModById( it.id) }.filter { !it.isEnable }
        if (mods.isEmpty()) return@flow
        // 初始化结果统计
        val needPasswordMods = mutableListOf<ModBean>()
        val fileMissingMods = mutableListOf<ModBean>()
        val backupFailedMods = mutableListOf<ModBean>()
        val enableFailedMods = mutableListOf<ModBean>()
        val enabledSucceededMods = mutableListOf<ModBean>()
        // var enabledCount = 0
        val total = mods.size

        // 0. 游戏选择检查
        emit(
            EnableState.Progress(
                step = EnableStep.VALIDATING,
                modName = "",
                current = 0,
                total = total,
                message = "检查游戏配置..."
            )
        )
        if (gameInfo == GameInfoConstant.NO_GAME || gameInfo.packageName.isEmpty()) {
            emit(EnableState.Error(AppError.GameError.GameNotSelected))
            return@flow
        }

        // 0.1 游戏安装检查
        fileService.listFiles(gameInfo.gamePath).onSuccess {
            if (it.isEmpty()) {
                emit(EnableState.Error(AppError.GameError.GameNotInstalled(gameInfo.gameName)))
                return@flow
            }
        }.onError {
            emit(EnableState.Error(it))
            return@flow
        }

        // 1. 权限检查
        emit(
            EnableState.Progress(
                step = EnableStep.VALIDATING,
                modName = "",
                current = 0,
                total = total,
                message = "检查权限..."
            )
        )

        val permissionResult = permissionService.checkPathPermissions(gameInfo.gamePath)
        if (permissionResult is Result.Error) {
            emit(EnableState.Error(permissionResult.error))
            return@flow
        }

        // 2. 冲突检测
        emit(
            EnableState.Progress(
                step = EnableStep.VALIDATING,
                modName = "",
                current = 0,
                total = total,
                message = "检测冲突..."
            )
        )

        // 2.1 检测待开启 MOD 之间的内部冲突
        val (mutualConflictMods, noInternalConflictMods) = detectMutualConflicts(mods)
        Log.d(
            TAG,
            "内部冲突 MOD: ${mutualConflictMods.size}, 无冲突: ${noInternalConflictMods.size}"
        )

        // 2.2 获取已开启的 MOD 列表
        val enabledMods = modRepository.getEnableMods(gameInfo.packageName).first()

        // 2.3 检测与已开启 MOD 的冲突
        val (enabledConflictMods, modsToProcess) = detectEnabledConflicts(
            noInternalConflictMods,
            enabledMods
        )
        Log.d(TAG, "与已开启冲突: ${enabledConflictMods.size}, 待处理: ${modsToProcess.size}")

        val processTotal = modsToProcess.size

        // 3. 遍历无冲突的 MOD 列表
        loop@ for ((index, mod) in modsToProcess.withIndex()) {


            val currentIndex = index + 1

            // 2.1 验证 MOD
            emit(
                EnableState.Progress(
                    step = EnableStep.VALIDATING,
                    modName = mod.name,
                    current = currentIndex,
                    total = total,
                    message = "验证 MOD..."
                )
            )

            val validationResult = modEnableService.validateMod(mod)
            when (validationResult) {
                ValidationResult.NEED_PASSWORD -> {
                    // emit(EnableState.Error(AppError.ModError.EncryptedNeedPassword))
                    needPasswordMods.add(mod)
                    continue
                }

                ValidationResult.FILE_MISSING -> {
                    // emit(EnableState.Error(AppError.ModError.FileMissing(mod.name)))
                    fileMissingMods.add(mod)
                    continue
                }

                ValidationResult.VALID -> {
                    /* 继续处理 */
                }
            }

            // 2.2 备份游戏文件（流式处理）
            emit(
                EnableState.Progress(
                    step = EnableStep.BACKING_UP,
                    modName = mod.name,
                    current = currentIndex,
                    total = total,
                    message = "备份游戏文件..."
                )
            )

            var backupSuccess = false
            var backupError: AppError? = null
            var backups: List<BackupBean> = emptyList()

            backupService.backupGameFiles(mod, gameInfo).collect { event ->
                when (event) {
                    is BackupEvent.FileProgress -> {
                        emit(
                            EnableState.Progress(
                                step = EnableStep.BACKING_UP,
                                modName = mod.name,
                                current = currentIndex,
                                total = total,
                                message = "备份文件: ${event.fileName}",
                                subProgress = event.current.toFloat() / event.total
                            )
                        )
                    }

                    is BackupEvent.Success -> {
                        // 插入备份数据库
                        //event.backups.forEach { backup -> backupRepository.insert(backup) }
                        backups = event.backups
                        backupSuccess = true
                    }

                    is BackupEvent.Failed -> {
                        // emit(EnableState.Error(AppError.ModError.BackupFailed(mod.name)))
                        backupSuccess = false
                        backupError = event.error
                    }
                }
            }

            // 备份失败则跳过该 MOD
            if (!backupSuccess) {
                backupFailedMods.add(mod)
                Log.e(TAG, "Backup failed for ${mod.name}: $backupError")
                continue
            }

            // 2.3 开启 MOD（收集 Service 的文件级进度）
            emit(
                EnableState.Progress(
                    step = EnableStep.ENABLING,
                    modName = mod.name,
                    current = currentIndex,
                    total = total,
                    message = "开启 MOD..."
                )
            )

            var enableSuccess = false
            var enableError: AppError? = null
            var fileMd5Map: Map<String, String> = emptyMap()

            modEnableService.enableSingleMod(mod, gameInfo).collect { event ->
                when (event) {
                    is EnableFileEvent.FileProgress -> {
                        emit(
                            EnableState.Progress(
                                step = EnableStep.ENABLING,
                                modName = mod.name,
                                current = currentIndex,
                                total = total,
                                message = event.fileName,
                                subProgress = event.current.toFloat() / event.total
                            )
                        )
                    }

                    is EnableFileEvent.Complete -> {
                        enableSuccess = event.success
                        enableError = event.error
                        fileMd5Map = event.fileMd5Map
                    }
                }
            }

            if (!enableSuccess) {
                enableFailedMods.add(mod)
                Log.e(TAG, "Enable failed for ${mod.name}: $enableError")
                continue
            }

            if (specialGameService.isSpecialGame(gameInfo.packageName)) {
                // 2.4 特殊游戏处理
                emit(
                    EnableState.Progress(
                        step = EnableStep.SPECIAL_PROCESS,
                        modName = mod.name,
                        current = currentIndex,
                        total = total,
                        message = "特殊处理..."
                    )
                )

                // TODO: 调用 specialGameService 进行特殊处理
                specialGameService.onModEnable(mod, gameInfo.packageName).onError {
                    enableFailedMods.add(mod)
                    continue@loop
                }
            }

            // 2.5 更新数据库状态
            emit(
                EnableState.Progress(
                    step = EnableStep.UPDATING_DB,
                    modName = mod.name,
                    current = currentIndex,
                    total = total,
                    message = "更新状态..."
                )
            )

            // 保存替换文件记录
            if (fileMd5Map.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                val replacedFiles =
                    fileMd5Map.map { (gameFilePath, md5) ->
                        ReplacedFileBean(
                            modId = mod.id,
                            filename = gameFilePath.substringAfterLast("/"),
                            gameFilePath = gameFilePath,
                            md5 = md5,
                            gamePackageName = gameInfo.packageName,
                            replaceTime = currentTime
                        )
                    }
                replacedFileRepository.saveReplacedFiles(replacedFiles)
            }
            if (backups.isNotEmpty()) {
                backupRepository.insertAll(backups)
            }

            modRepository.updateMod(mod.copy(isEnable = true))
            enabledSucceededMods.add(mod.copy(isEnable = true))
            //判断是否取消
            if (isCanceled) {
                emit(
                    EnableState.Cancel(
                        EnableResult(
                            enabledCount = enabledSucceededMods.size,
                            enabledSucceededMods = enabledSucceededMods,
                            skippedCount = needPasswordMods.size + fileMissingMods.size +
                                    mutualConflictMods.size + enabledConflictMods.size,
                            needPasswordMods = needPasswordMods,
                            fileMissingMods = fileMissingMods,
                            backupFailedMods = backupFailedMods,
                            enableFailedMods = enableFailedMods,
                            mutualConflictMods = mutualConflictMods,
                            enabledConflictMods = enabledConflictMods
                        )
                    )
                )
                isCanceled = false
                return@flow
            }
        }

        // 4. 发射最终结果
        emit(
            EnableState.Success(
                EnableResult(
                    enabledCount = enabledSucceededMods.size,
                    enabledSucceededMods = enabledSucceededMods,
                    skippedCount = needPasswordMods.size + fileMissingMods.size +
                            mutualConflictMods.size + enabledConflictMods.size,
                    needPasswordMods = needPasswordMods,
                    fileMissingMods = fileMissingMods,
                    backupFailedMods = backupFailedMods,
                    enableFailedMods = enableFailedMods,
                    mutualConflictMods = mutualConflictMods,
                    enabledConflictMods = enabledConflictMods
                )
            )
        )
    }

    /**
     * 执行批量关闭 MOD
     * @param mods 要关闭的 MOD 列表
     * @param gameInfo 游戏信息
     * @return Flow<EnableState> 关闭状态流
     */
    fun disable(modsToDisable: List<ModBean>, gameInfo: GameInfoBean): Flow<EnableState> = flow {
        val mods = modsToDisable.mapNotNull { modRepository.getModById( it.id) }.filter { it.isEnable }
        if (mods.isEmpty()) return@flow
        val restoreFailedMods = mutableListOf<ModBean>()
        val disabledSucceededMods = mutableListOf<ModBean>()
        //var disabledCount = 0
        val total = mods.size

        // 1. 权限检查
        val permissionResult = permissionService.checkPathPermissions(gameInfo.gamePath)
        if (permissionResult is Result.Error) {
            emit(EnableState.Error(permissionResult.error))
            return@flow
        }

        // 2. 遍历 MOD 列表
        loop@ for ((index, mod) in mods.withIndex()) {
            val currentIndex = index + 1

            // 2.1 还原备份（流式处理）
            emit(
                EnableState.Progress(
                    step = EnableStep.DISABLING,
                    modName = mod.name,
                    current = currentIndex,
                    total = total,
                    message = "还原游戏文件..."
                )
            )

            var restoreSuccess = false
            var restoreError: AppError? = null
            val backups = backupRepository.getByModIdSync(mod.id)
            val replacedFiles = replacedFileRepository.getByModId(mod.id)
            backupService.restoreBackups(backups, replacedFiles, mod, gameInfo).collect { event ->
                when (event) {
                    is BackupEvent.FileProgress -> {
                        emit(
                            EnableState.Progress(
                                step = EnableStep.DISABLING,
                                modName = mod.name,
                                current = currentIndex,
                                total = total,
                                message = event.fileName,
                                subProgress = event.current.toFloat() / event.total
                            )
                        )
                    }

                    is BackupEvent.Success -> {

                        restoreSuccess = true
                    }

                    is BackupEvent.Failed -> {
                        restoreSuccess = false
                        restoreError = event.error
                    }
                }
            }

            if (!restoreSuccess) {
                restoreFailedMods.add(mod)
                Log.e(TAG, "Restore failed for ${mod.name}: $restoreError")
                continue
            }

            if (specialGameService.isSpecialGame(gameInfo.packageName)) {
                // 2.4 特殊游戏处理
                emit(
                    EnableState.Progress(
                        step = EnableStep.SPECIAL_PROCESS,
                        modName = mod.name,
                        current = currentIndex,
                        total = total,
                        message = "特殊处理..."
                    )
                )

                // TODO: 调用 specialGameService 进行特殊处理
                specialGameService.onModDisable(backups, gameInfo.packageName, mod).onError {
                    restoreFailedMods.add(mod)
                    continue@loop
                }


            }

            // 2.2 更新数据库状态
            emit(
                EnableState.Progress(
                    step = EnableStep.UPDATING_DB,
                    modName = mod.name,
                    current = currentIndex,
                    total = total,
                    message = "更新状态..."
                )
            )

            // 删除替换文件记录
            replacedFileRepository.deleteByModId(mod.id)

            // 检查物理文件是否存在
            val isExist = when (val existResult =  fileService.isFileExist(mod.path)) {
                is Result.Success -> existResult.data
                is Result.Error -> {
                    Log.e(TAG, "Check file exist failed: ${existResult.error}")
                    false
                }
            }
            if (isExist) {
                // 物理文件存在，只更新状态为关闭
                modRepository.updateMod(mod.copy(isEnable = false))
                disabledSucceededMods.add(mod.copy(isEnable = false))
            } else {
                // 物理文件不存在，直接从数据库删除
                Log.d(TAG, "Mod physical file not found, deleting from database: ${mod.path}")
                modRepository.deleteAll(listOf(mod))
                disabledSucceededMods.add(mod.copy(isEnable = false))
            }

            // 删除备份数据
            backupRepository.deleteByModId(mod.id)

            // 检查是否取消任务
            if (isCanceled) {
                emit(
                    EnableState.Cancel(
                        EnableResult(
                            enabledCount = disabledSucceededMods.size,
                            disabledSucceededMods = disabledSucceededMods,
                            skippedCount = 0,
                            restoreFailedMods = restoreFailedMods
                        )
                    )
                )
                isCanceled = false
                return@flow
            }
        }

        // 3. 发射最终结果
        emit(
            EnableState.Success(
                EnableResult(
                    enabledCount = disabledSucceededMods.size,
                    disabledSucceededMods = disabledSucceededMods,
                    skippedCount = 0,
                    restoreFailedMods = restoreFailedMods
                )
            )
        )
    }

    /**
     * 执行取消任务
     */

    fun cancel() {
        isCanceled = true
    }
    /**
     * 检测待开启 MOD 之间的内部冲突
     * @return Pair<冲突的MOD列表, 无冲突的MOD列表>
     */
    private fun detectMutualConflicts(mods: List<ModBean>): Pair<List<ModBean>, List<ModBean>> {
        if (mods.size <= 1) return Pair(emptyList(), mods)

        // 建立 gameFilePath -> MOD 列表的映射
        val pathToMods = mutableMapOf<String, MutableList<ModBean>>()
        for (mod in mods) {
            for (path in mod.gameFilesPath) {
                pathToMods.getOrPut(path) { mutableListOf() }.add(mod)
            }
        }

        // 找出所有冲突的 MOD（有任何路径被多个 MOD 使用）
        val conflictMods = mutableSetOf<ModBean>()
        for ((_, modsForPath) in pathToMods) {
            if (modsForPath.size > 1) {
                conflictMods.addAll(modsForPath)
            }
        }

        val noConflictMods = mods.filter { it !in conflictMods }
        return Pair(conflictMods.toList(), noConflictMods)
    }

    /**
     * 检测与已开启 MOD 的冲突
     * @return Pair<与冲突的已开启MOD列表, 无冲突的待开启MOD列表>
     */
    private fun detectEnabledConflicts(
        mods: List<ModBean>,
        enabledMods: List<ModBean>
    ): Pair<List<ModBean>, List<ModBean>> {
        if (enabledMods.isEmpty()) return Pair(emptyList(), mods)

        // 构建已开启MOD的路径到MOD的映射
        val pathToEnabledMod = mutableMapOf<String, MutableList<ModBean>>()
        for (enabledMod in enabledMods) {
            for (path in enabledMod.gameFilesPath) {
                pathToEnabledMod.getOrPut(path) { mutableListOf() }.add(enabledMod)
            }
        }

        val conflictEnabledMods = mutableSetOf<ModBean>()
        val noConflictMods = mutableListOf<ModBean>()

        for (mod in mods) {
            // 检查是否有任何路径与已开启 MOD 冲突
            var hasConflict = false
            for (path in mod.gameFilesPath) {
                val conflicting = pathToEnabledMod[path]
                if (conflicting != null) {
                    hasConflict = true
                    conflictEnabledMods.addAll(conflicting)
                }
            }
            if (!hasConflict) {
                noConflictMods.add(mod)
            }
        }

        return Pair(conflictEnabledMods.toList(), noConflictMods)
    }
}

/** 开启状态密封类（UseCase 级别，供 UI 使用） */
sealed class EnableState {
    /** 进度更新 */
    data class Progress(
        val step: EnableStep,
        val modName: String,
        val current: Int,
        val total: Int,
        val message: String = "",
        val subProgress: Float = -1f
    ) : EnableState()

    /** 完成 */
    data class Success(val result: EnableResult) : EnableState()

    /** 错误 */
    data class Error(val error: AppError) : EnableState()

    /** 取消*/
    data class Cancel(val result: EnableResult) : EnableState()
}

/** 开启结果 */
data class EnableResult(
    val enabledCount: Int,
    val skippedCount: Int = 0,
    val enabledSucceededMods: List<ModBean> = emptyList(),
    val disabledSucceededMods: List<ModBean> = emptyList(),
    val needPasswordMods: List<ModBean> = emptyList(),
    val fileMissingMods: List<ModBean> = emptyList(),
    val backupFailedMods: List<ModBean> = emptyList(),
    val enableFailedMods: List<ModBean> = emptyList(),
    val restoreFailedMods: List<ModBean> = emptyList(),
    /** 互相冲突的 MOD（待开启列表内部冲突） */
    val mutualConflictMods: List<ModBean> = emptyList(),
    /** 与已开启 MOD 冲突的 MOD */
    val enabledConflictMods: List<ModBean> = emptyList()
)
