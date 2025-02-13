package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import androidx.compose.runtime.ProvidedValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.VersionRepository
import top.laoxin.modmanager.domain.usercase.app.CheckPermissionUserCase
import top.laoxin.modmanager.exception.NoSelectedGameException
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.lings.updater.util.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckCanSwitchModsUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val checkPermissionUserCase: CheckPermissionUserCase,
    private val appPathsManager: AppPathsManager

) {
     operator fun invoke(): Pair<Int,String> {
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