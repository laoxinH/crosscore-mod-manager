package top.laoxin.modmanager.domain.usercase.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.tools.manager.GameInfoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetGameModsCountUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
     operator fun invoke(): Flow<Int> {
        return modRepository.getModsCountByGamePackageName(gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class GetGameEnableModsCountUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    operator fun invoke(): Flow<Int> {
        return modRepository.getEnableModsCountByGamePackageName(gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class GetGameEnableModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    operator fun invoke(): Flow<List<ModBean>> {
        return modRepository.getEnableMods(gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class GetGameAllModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    operator fun invoke(): Flow<List<ModBean>> {
        return modRepository.getModsByGamePackageName(gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class GetGameDisEnableUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    operator fun invoke(): Flow<List<ModBean>> {
        return modRepository.getDisableMods(gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class DeleteModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    suspend operator fun invoke(delModsList : List<ModBean>){
        return modRepository.deleteAll(delModsList)
    }
}

@Singleton
class SearchModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    suspend operator fun invoke(searchText: String): Flow<List<ModBean>> {
        return modRepository.search(searchText, gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class UpdateModUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val gameInfoManager: GameInfoManager

) {
    suspend operator fun invoke(mod : ModBean) {
        return modRepository.updateMod(mod)
    }
}