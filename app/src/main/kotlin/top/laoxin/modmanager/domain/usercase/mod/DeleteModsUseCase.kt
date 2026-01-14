package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.PermissionService

/** 批量删除 MOD 用例 负责权限检查、整合包判断、已启用跳过、物理文件删除、数据库清理 */
@Singleton
class DeleteModsUseCase
@Inject
constructor(
    private val modRepository: ModRepository,
    private val permissionService: PermissionService,
    private val fileService: FileService
) {
    companion object {
        private const val TAG = "DeleteModsUseCase"
    }

    // 取消任务标识
    private var isCanceled = false

    /**
     * 执行批量删除 MOD
     * @param mods 要删除的 MOD 列表
     * @param deleteIntegratedPackage 是否删除整合包（一个物理文件包含多个MOD）
     * @return Flow<DeleteState> 删除状态流
     */
    fun execute(modToDelete: List<ModBean>, deleteIntegratedPackage: Boolean): Flow<DeleteState> = flow {
        isCanceled = false
        // 从数据库查询最新的mod
        val mods = modToDelete.mapNotNull { modRepository.getModById(it.id) }
        if (mods.size != modToDelete.size) {
            Log.w(TAG, "部分 MOD 不存在，预期 ${modToDelete.size} 个，实际找到 ${mods.size} 个")
        }
        if (mods.isEmpty()) {
            emit(
                DeleteState.Success(
                    DeleteResult(
                        deletedMods = emptyList(),
                        deletedEnabledMods = emptyList(),
                        skippedIntegratedMods = emptyList(),
                        failedMods = emptyList()
                    )
                )
            )
            return@flow
        }

        // Step 1: 鉴权
        emit(
            DeleteState.Progress(
                step = DeleteStep.AUTHENTICATING,
                modName = "",
                progress = 0f,
                current = 0,
                total = mods.size
            )
        )

        if (!permissionService.hasStoragePermission()) {
            emit(DeleteState.Error(AppError.PermissionError.StoragePermissionDenied))
            return@flow
        }

        if (isCanceled) {
            emit(DeleteState.Cancel)
            return@flow
        }

        // Step 2: 收集所有MOD的物理路径并去重
        emit(
            DeleteState.Progress(
                step = DeleteStep.COLLECTING_PATHS,
                modName = "",
                progress = 0.1f,
                current = 0,
                total = mods.size
            )
        )

        val distinctPaths = mods.map { it.path }.distinct()
        Log.d(TAG, "收集到 ${distinctPaths.size} 个不同路径")

        if (isCanceled) {
            emit(DeleteState.Cancel)
            return@flow
        }

        // Step 3: 查询每个路径下的所有MOD，进行筛选
        emit(
            DeleteState.Progress(
                step = DeleteStep.FILTERING,
                modName = "",
                progress = 0.2f,
                current = 0,
                total = mods.size
            )
        )

        val skippedIntegratedMods = mutableListOf<ModBean>()
        val modsToDelete = mutableListOf<ModBean>() // 未开启的MOD，删除后清理数据库
        val enabledModsToDelete = mutableListOf<ModBean>() // 已开启的MOD，删除物理文件但保留数据库
        val pathsToDelete = mutableSetOf<String>() // 需要删除的物理文件路径

        // 按路径分组选中的MOD
        val selectedModsByPath = mods.groupBy { it.path }

        for (path in distinctPaths) {
            if (isCanceled) {
                emit(DeleteState.Cancel)
                return@flow
            }

            // 查询该路径下所有的MOD
            val allModsAtPath = modRepository.getModsByPath(path).first()
            val selectedModsAtPath = selectedModsByPath[path] ?: emptyList()

            Log.d(
                TAG,
                "路径 $path 下共有 ${allModsAtPath.size} 个MOD, 选中 ${selectedModsAtPath.size} 个"
            )

            // 判断是否为整合包（路径下有多个MOD）
            val isIntegratedPackage = allModsAtPath.size > 1

            for (mod in selectedModsAtPath) {
                when {
                    // 整合包处理：如果不删除整合包则跳过
                    isIntegratedPackage && !deleteIntegratedPackage -> {
                        skippedIntegratedMods.add(mod)
                        Log.d(TAG, "跳过整合包MOD: ${mod.name}")
                    }
                    // 已启用的MOD：删除物理文件但不清理数据库
                    mod.isEnable -> {
                        enabledModsToDelete.add(mod)
                        pathsToDelete.add(path)
                        Log.d(TAG, "已启用的MOD将被删除(保留数据库): ${mod.name}")
                    }
                    // 未启用的MOD：删除物理文件并清理数据库
                    else -> {
                        modsToDelete.add(mod)
                        pathsToDelete.add(path)
                    }
                }
            }

            // 如果是整合包且选择删除整合包，需要添加该路径下所有MOD
            if (isIntegratedPackage && deleteIntegratedPackage) {
                for (mod in allModsAtPath) {
                    if (!selectedModsAtPath.contains(mod)) {
                        if (mod.isEnable) {
                            if (!enabledModsToDelete.contains(mod)) {
                                enabledModsToDelete.add(mod)
                            }
                        } else if (!modsToDelete.contains(mod)) {
                            modsToDelete.add(mod)
                        }
                    }
                }
            }
        }

        Log.d(
            TAG,
            "待删除MOD: ${modsToDelete.size}, 已启用待删除: ${enabledModsToDelete.size}, 跳过整合包: ${skippedIntegratedMods.size}"
        )

        if (modsToDelete.isEmpty() && enabledModsToDelete.isEmpty()) {
            emit(
                DeleteState.Success(
                    DeleteResult(
                        deletedMods = emptyList(),
                        deletedEnabledMods = emptyList(),
                        skippedIntegratedMods = skippedIntegratedMods,
                        failedMods = emptyList()
                    )
                )
            )
            return@flow
        }

        // Step 4: 执行删除
        val deletedMods = mutableListOf<ModBean>()
        val deletedEnabledMods = mutableListOf<ModBean>()
        val failedMods = mutableListOf<Pair<ModBean, AppError>>()

        // 合并所有需要删除物理文件的MOD
        val allModsToDeletePhysically = modsToDelete + enabledModsToDelete

        // 按路径分组，每个路径只删除一次物理文件
        val modsGroupedByPath = allModsToDeletePhysically.groupBy { it.path }
        var deletedCount = 0
        val totalToDelete = allModsToDeletePhysically.size

        for ((path, modsAtPath) in modsGroupedByPath) {
            if (isCanceled) {
                emit(DeleteState.Cancel)
                return@flow
            }

            emit(
                DeleteState.Progress(
                    step = DeleteStep.DELETING,
                    modName = modsAtPath.firstOrNull()?.name ?: "",
                    progress = 0.3f + 0.6f * (deletedCount.toFloat() / totalToDelete),
                    current = deletedCount,
                    total = totalToDelete
                )
            )

            when (val deleteResult = fileService.deleteFile(path)) {
                is Result.Success -> {
                    // 物理文件删除成功
                    // 分离已启用和未启用的MOD
                    val enabledMods = modsAtPath.filter { it.isEnable }
                    val disabledMods = modsAtPath.filter { !it.isEnable }

                    // 未启用的MOD：删除数据库记录
                    if (disabledMods.isNotEmpty()) {
                        try {
                            modRepository.deleteAll(disabledMods)
                            deletedMods.addAll(disabledMods)
                            Log.d(TAG, "成功删除路径: $path, 未启用MOD数量: ${disabledMods.size}")
                        } catch (e: Exception) {
                            Log.e(TAG, "删除数据库记录失败: $path", e)
                            disabledMods.forEach { mod ->
                                failedMods.add(mod to AppError.DatabaseError.DeleteFailed)
                            }
                        }
                    }

                    // 已启用的MOD：保留数据库记录，只记录到已删除列表
                    if (enabledMods.isNotEmpty()) {
                        deletedEnabledMods.addAll(enabledMods)
                        Log.d(TAG, "成功删除路径: $path, 已启用MOD数量: ${enabledMods.size} (保留数据库)")
                    }
                }

                is Result.Error -> {
                    Log.e(TAG, "删除物理文件失败: $path, 错误: ${deleteResult.error}")
                    modsAtPath.forEach { mod -> failedMods.add(mod to deleteResult.error) }
                }
            }

            deletedCount += modsAtPath.size
        }

        // Step 5: 完成
        emit(
            DeleteState.Progress(
                step = DeleteStep.COMPLETED,
                modName = "",
                progress = 1f,
                current = deletedCount,
                total = totalToDelete
            )
        )

        emit(
            DeleteState.Success(
                DeleteResult(
                    deletedMods = deletedMods,
                    deletedEnabledMods = deletedEnabledMods,
                    skippedIntegratedMods = skippedIntegratedMods,
                    failedMods = failedMods
                )
            )
        )
    }

    /**
     * 删除前检查 检测待删除MOD中是否包含整合包，返回整合包信息用于用户确认
     * @param mods 要删除的 MOD 列表
     * @return Result<DeleteCheckResult, AppError> 检查结果
     */
    suspend fun checkBeforeDelete(mods: List<ModBean>): Result<DeleteCheckResult> {
        if (mods.isEmpty()) {
            return Result.Success(
                DeleteCheckResult(
                    selectedMods = emptyList(),
                    singleMods = emptyList(),
                    singleEnabledMods = emptyList(),
                    integratedPackages = emptyList(),
                    hasEnabledMods = false
                )
            )
        }

        val distinctPaths = mods.map { it.path }.distinct()
        val integratedPackages = mutableListOf<IntegratedPackageInfo>() // 整合包信息
        // 单个mod列表
        val singleMods = mutableListOf<ModBean>()
        // 当个已开启的MOD
        val singleEnabledMods = mutableListOf<ModBean>()

        var hasEnabledMods = false

        // 按路径分组选中的MOD
        val selectedModsByPath = mods.groupBy { it.path }

        for (path in distinctPaths) {
            // 查询该路径下所有的MOD
            val allModsAtPath = modRepository.getModsByPath(path).first()
            val selectedModsAtPath = selectedModsByPath[path] ?: emptyList()

            // 判断是否为整合包（路径下有多个MOD）
            if (allModsAtPath.size > 1) {
                // 找出未被选中的其他MOD
                val otherMods =
                    allModsAtPath.filter { mod -> selectedModsAtPath.none { it.id == mod.id } }

                // 检查是否有已启用的MOD
                val enabledMods = allModsAtPath.filter { it.isEnable }
                if (enabledMods.isNotEmpty()) {
                    hasEnabledMods = true
                }

                integratedPackages.add(
                    IntegratedPackageInfo(
                        name = fileService.getFileName(path),
                        path = path,
                        selectedMods = selectedModsAtPath,
                        otherMods = otherMods,
                        enabledMods = enabledMods,
                        totalCount = allModsAtPath.size
                    )
                )
            } else {
                // 单个MOD，检查是否已启用
                if (selectedModsAtPath.any { it.isEnable }) {
                    hasEnabledMods = true
                    singleEnabledMods.add(selectedModsAtPath.first())
                } else {
                    singleMods.add(selectedModsAtPath.first())
                }
            }
        }

        Log.d(
            TAG,
            "删除前检查: 发现 ${integratedPackages.size} 个整合包, 有已启用MOD: $hasEnabledMods"
        )

        return Result.Success(
            DeleteCheckResult(
                selectedMods = mods,
                singleMods = singleMods,
                singleEnabledMods = singleEnabledMods,
                integratedPackages = integratedPackages,
                hasEnabledMods = hasEnabledMods
            )
        )
    }

    /** 取消删除任务 */
    fun cancel() {
        isCanceled = true
    }
}

/** 删除步骤枚举 */
enum class DeleteStep {
    AUTHENTICATING, // 鉴权
    COLLECTING_PATHS, // 收集路径
    FILTERING, // 筛选
    DELETING, // 删除中
    COMPLETED // 完成
}

/** 删除状态密封类 */
sealed class DeleteState {
    /** 进度更新 */
    data class Progress(
        val step: DeleteStep,
        val modName: String,
        val progress: Float,
        val current: Int,
        val total: Int
    ) : DeleteState()

    /** 完成 */
    data class Success(val result: DeleteResult) : DeleteState()

    /** 错误 */
    data class Error(val error: AppError) : DeleteState()

    /** 取消 */
    object Cancel : DeleteState()
}

/** 删除结果 */
data class DeleteResult(
    val deletedMods: List<ModBean>,
    val deletedEnabledMods: List<ModBean>, // 已删除的已启用MOD（物理文件已删除，数据库保留）
    val skippedIntegratedMods: List<ModBean>,
    val failedMods: List<Pair<ModBean, AppError>>
)

/** 删除前检查结果 */
data class DeleteCheckResult(
    val selectedMods: List<ModBean>,
    // 单个mod
    val singleMods: List<ModBean>,
    // 单个已启用的MOD
    val singleEnabledMods: List<ModBean>,
    val integratedPackages: List<IntegratedPackageInfo>,
    val hasEnabledMods: Boolean
) {
    /** 是否存在整合包 */
    val hasIntegratedPackages: Boolean
        get() = integratedPackages.isNotEmpty()

    /** 整合包中未被选中的其他MOD总数 */
    val otherModsCount: Int
        get() = integratedPackages.sumOf { it.otherMods.size }

    /** 所有检查结果已启用MOD */
    val allEnabledMods: List<ModBean>
        get() = integratedPackages.flatMap { it.enabledMods }.distinctBy { it.id } + singleEnabledMods
}

/** 整合包信息 */
data class IntegratedPackageInfo(
    val name: String,
    val path: String,
    val selectedMods: List<ModBean>,
    val otherMods: List<ModBean>,
    val enabledMods: List<ModBean>,
    val totalCount: Int
) {
    /** 整合包名称（使用路径名） */
    val packageName: String
        get() = path.substringAfterLast("/").substringBeforeLast(".")
}
