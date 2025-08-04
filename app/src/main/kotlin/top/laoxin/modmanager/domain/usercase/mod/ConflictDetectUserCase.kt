package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.tools.manager.GameInfoManager

import javax.inject.Inject
import javax.inject.Singleton

data class ConflictDetectResult(
    val code: Int,
    // 冲突的mod
    val conflictMods: List<ModBean>,

    val message: String,
)

@Singleton
class ConflictDetectUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val modRepository: ModRepository,
) {

    companion object {
        const val TAG = "ConflictDetectUserCase"
    }


    suspend operator fun invoke(
        mods: List<ModBean>,
    ): ConflictDetectResult = withContext(Dispatchers.IO) {
        val currentGameMods =
            modRepository.getModsByGamePackageName(gameInfoManager.getGameInfo().packageName)
                .first()
        // 获取已开启的mod
        val enabledMods = currentGameMods.filter { it.isEnable }
        // 获取mods中所有modfile
        val allModFiles = mods.flatMap { it.modFiles.orEmpty() }
        // 根据allModFiles获取冲突的mod
        val conflictMods = enabledMods.filter { mod ->
            allModFiles.any { modFile ->
                mod.modFiles?.contains(modFile) == true
            }
        }
        // 如果冲突的mod不为空
        if (conflictMods.isNotEmpty()) {
            return@withContext ConflictDetectResult(
                code = ResultCode.HAVE_CONFLICT_MODS,
                conflictMods = conflictMods,
                message = ""
            )
        }
        return@withContext ConflictDetectResult(
            code = ResultCode.SUCCESS,
            conflictMods = emptyList(),
            message = ""
        )

    }


}