package top.laoxin.modmanager.domain.usercase.console

import kotlinx.coroutines.flow.first
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.AntiHarmonyRepository
import top.laoxin.modmanager.domain.repository.GameInfoRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A UseCase that orchestrates the process of switching the anti-harmony feature.
 * It delegates all the complex logic to the repositories.
 */
@Singleton
class SwitchAntiHarmonyUserCase @Inject constructor(
    private val antiHarmonyRepository: AntiHarmonyRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enable: Boolean): Result<Unit> {
        // 1. Get the current game info from the source of truth.
        val gameInfo = userPreferencesRepository.selectedGameValue

        // 2. Delegate the entire operation to the responsible repository.
        return antiHarmonyRepository.switchAntiHarmony(gameInfo, enable)
    }
}
