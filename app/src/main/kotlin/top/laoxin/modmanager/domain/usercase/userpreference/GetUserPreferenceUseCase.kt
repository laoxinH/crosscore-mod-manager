package top.laoxin.modmanager.domain.usercase.userpreference

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserPreferenceUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun <T> invoke(key: String, defaultValue: T): Flow<T> {
        return userPreferencesRepository.getPreferenceFlow(key, defaultValue)
    }
}