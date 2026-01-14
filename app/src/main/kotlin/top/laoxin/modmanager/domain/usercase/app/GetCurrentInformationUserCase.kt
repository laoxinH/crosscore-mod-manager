package top.laoxin.modmanager.domain.usercase.app

import kotlinx.coroutines.flow.first
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.data.repository.AppDataRepositoryImpl
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCurrentInformationUserCase @Inject constructor(
    private val appDataRepositoryImpl: AppDataRepositoryImpl,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * Fetches new information if available.
     *
     */
    /**
     * Fetches new information if available.
     * This UseCase delegates all data handling logic to the repository.
     */
    suspend operator fun invoke(autoCheck : Boolean = true): InfoBean? {
        val lastSeenVersion = userPreferencesRepository.cachedInformationVision.first()
        val remoteInfo =  appDataRepositoryImpl.getNewInformation().getOrNull()

        return if (remoteInfo != null ) {
            if (remoteInfo.version > lastSeenVersion) {
                userPreferencesRepository.saveCachedInformationVision(remoteInfo.version)
                userPreferencesRepository.saveCachedInformation(remoteInfo.msg)
                remoteInfo
            } else {
                if (!autoCheck) {
                    remoteInfo
                } else {
                    null
                }

            }
        } else {
            if (lastSeenVersion != 0.0) {
                InfoBean(
                    lastSeenVersion,
                    userPreferencesRepository.cachedInformation.first()
                )
            } else {
                null
            }

        }
    }
}
