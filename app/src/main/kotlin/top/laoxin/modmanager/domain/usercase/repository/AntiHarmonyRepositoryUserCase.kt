package top.laoxin.modmanager.domain.usercase.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.bean.AntiHarmonyBean
import top.laoxin.modmanager.data.repository.antiharmony.AntiHarmonyRepository
import top.laoxin.modmanager.tools.manager.GameInfoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAntiHarmonyUserCase @Inject constructor(
    private val antiHarmonyRepository: AntiHarmonyRepository,
    private val gameInfoManager: GameInfoManager
) {
    suspend operator fun invoke(): Flow<AntiHarmonyBean?> {
        return antiHarmonyRepository.getByGamePackageName(gameInfoManager.getGameInfo().packageName)
    }
}

@Singleton
class AddGameToAntiHarmonyUserCase @Inject constructor(
    private val antiHarmonyRepository: AntiHarmonyRepository,
    private val gameInfoManager: GameInfoManager
) {
    suspend operator fun invoke() {
        antiHarmonyRepository.insert(
            AntiHarmonyBean(
                gamePackageName = gameInfoManager.getGameInfo().packageName,
                isEnable = false
            )
        )
    }
}