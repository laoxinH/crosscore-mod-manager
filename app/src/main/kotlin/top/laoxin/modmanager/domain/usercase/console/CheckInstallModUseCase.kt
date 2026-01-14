package top.laoxin.modmanager.domain.usercase.console

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.service.PermissionService

/** 检查是否可以安装 Mod 的用例 结合用户选择的游戏和权限状态，实时返回是否可以安装 Mod */
class CheckInstallModUseCase
@Inject
constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val permissionService: PermissionService
) {
    operator fun invoke(): Flow<Boolean> {
        return userPreferencesRepository.selectedGame.map { gameInfo ->
            val gamePath = gameInfo.gamePath
            if (gamePath.isEmpty()) {
                false
            } else {
                val accessType = permissionService.getFileAccessType(gamePath)
                accessType != FileAccessType.NONE
            }
        }
    }
}
