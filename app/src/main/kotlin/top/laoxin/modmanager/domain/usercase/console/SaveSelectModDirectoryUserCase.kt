package top.laoxin.modmanager.domain.usercase.console

import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveSelectModDirectoryUserCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    /**
     * 指示 Repository 去准备并设置一个新的 Mod 目录。
     * @return Boolean: 操作是否成功。
     */
    suspend operator fun invoke(selectedDirectoryPath: String, currentGame: GameInfoBean): Result<Unit> {


        // 2. The UseCase then calls a simpler method on the repository, passing in all required data.
        return userPreferencesRepository.prepareAndSetModDirectory(selectedDirectoryPath)
    }
}
