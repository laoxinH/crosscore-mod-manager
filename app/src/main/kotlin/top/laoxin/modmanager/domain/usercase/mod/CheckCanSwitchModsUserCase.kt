package top.laoxin.modmanager.domain.usercase.mod

import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.domain.usercase.app.CheckPermissionUserCase
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckCanSwitchModsUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val checkPermissionUserCase: CheckPermissionUserCase,
    private val appPathsManager: AppPathsManager

) {
    operator fun invoke(): Pair<Int, String> {
        return if (checkPermissionUserCase(gameInfoManager.getGameInfo().gamePath)) {
            if (checkPermissionUserCase(appPathsManager.getMyAppPath())) {
                Pair(ResultCode.SUCCESS, "")
            } else {
                Pair(ResultCode.NO_PERMISSION, appPathsManager.getMyAppPath())
            }
        } else {
            Pair(ResultCode.NO_PERMISSION, gameInfoManager.getGameInfo().gamePath)
        }
    }

}