package top.laoxin.modmanager.domain.usercase.console

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import top.laoxin.modmanager.domain.bean.AntiHarmonyBean
import top.laoxin.modmanager.domain.repository.AntiHarmonyRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCurrentGameAntiHarmonyStateUserCase @Inject constructor(
    private val antiHarmonyRepository: AntiHarmonyRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<AntiHarmonyBean?> {
       return userPreferencesRepository.selectedGame.flatMapLatest {
            antiHarmonyRepository.getAntiHarmony(it.packageName)
        }
        //return antiHarmonyRepository.getAntiHarmony(userPreferencesRepository.selectedGameValue.packageName)
    }
}

//@Singleton
//class AddGameToAntiHarmonyUserCase @Inject constructor(
//    private val antiHarmonyRepository: AntiHarmonyRepository,
//    private val gameInfoManager: GameInfoManager
//) {
//    suspend operator fun invoke() {
//        antiHarmonyRepository.insert(
//            AntiHarmonyBean(
//                gamePackageName = gameInfoManager.getGameInfo().packageName,
//                isEnable = false
//            )
//        )
//    }
//}