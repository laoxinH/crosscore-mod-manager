package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetGameModsCountUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository

) {
    operator fun invoke(): Flow<Int> {
       return userPreferencesRepository.selectedGame.flatMapLatest {
             modRepository.getModsCountByGamePackageName(it.packageName)
        }

        //return modRepository.getModsCountByGamePackageName(userPreferencesRepository.selectedGameValue.packageName)
    }
}

@Singleton
class GetGameEnableModsCountUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository


) {
    operator fun invoke(): Flow<Int> {

        return userPreferencesRepository.selectedGame.flatMapLatest {
            modRepository.getEnableModsCountByGamePackageName(it.packageName)
        }
    }
}

@Singleton
class GetGameEnableModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository


) {
    operator fun invoke(): Flow<List<ModBean>> {
        return modRepository.getEnableMods(userPreferencesRepository.selectedGameValue.packageName)
    }
}

@Singleton
class GetGameAllModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository

) {
    operator fun invoke(): Flow<List<ModBean>> {
        return userPreferencesRepository.selectedGame.flatMapLatest  {
            modRepository.getModsByGamePackageName(it.packageName)
        }
        //return modRepository.getModsByGamePackageName(userPreferencesRepository.selectedGameValue.packageName)
    }
}

@Singleton
class GetGameDisEnableUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository


) {
    operator fun invoke(): Flow<List<ModBean>> {
        return modRepository.getDisableMods(userPreferencesRepository.selectedGameValue.packageName)
    }
}

@Singleton
class DeleteModsUserCase @Inject constructor(
    private val modRepository: ModRepository

) {
    suspend operator fun invoke(delModsList: List<ModBean>) {
        return modRepository.deleteAll(delModsList)
    }
}

@Singleton
class SearchModsUserCase @Inject constructor(
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository


) {
    operator fun invoke(searchText: String): Flow<List<ModBean>> {
        return modRepository.search(searchText, userPreferencesRepository.selectedGameValue.packageName)
    }
}
