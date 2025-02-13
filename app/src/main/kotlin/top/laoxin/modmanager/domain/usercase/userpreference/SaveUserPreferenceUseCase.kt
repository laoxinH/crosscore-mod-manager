package top.laoxin.modmanager.domain.usercase.userpreference

import top.laoxin.modmanager.data.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveUserPreferenceUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun <T> invoke(key: String, value: T) {
        userPreferencesRepository.savePreference(key, value)
    }
}