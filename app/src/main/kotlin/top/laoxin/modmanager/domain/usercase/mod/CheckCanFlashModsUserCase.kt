package top.laoxin.modmanager.domain.usercase.mod


import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.ScanModPath

import top.laoxin.modmanager.domain.usercase.app.CheckPermissionUserCase

import top.laoxin.modmanager.tools.manager.GameInfoManager

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckCanFlashModsUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val checkPermissionUserCase: CheckPermissionUserCase,
) {
    operator fun invoke(): Pair<Int, String> {
        return if (checkSelectGame()) {
            if (!checkPermissionUserCase(gameInfoManager.getGameInfo().gamePath)) {
                Pair(ResultCode.NO_PERMISSION, gameInfoManager.getGameInfo().gamePath)
            } else if (!checkPermissionUserCase(ScanModPath.MOD_PATH_QQ)) {
                Pair(ResultCode.NO_PERMISSION, ScanModPath.MOD_PATH_QQ)
            } else {
                Pair(ResultCode.SUCCESS, "")
            }
        } else {
            Pair(ResultCode.NO_SELECTED_GAME, "")
        }
    }

    private fun checkSelectGame(): Boolean {
        return gameInfoManager.getGameInfo().packageName.isNotEmpty()
    }
}