package top.laoxin.modmanager.domain.usercase.gameinfo

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.manager.GameInfoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadGameConfigUserCase @Inject constructor(
    @FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools,
    private val checkGameConfigUserCase: CheckGameConfigUserCase,
    private val gameInfoManager: GameInfoManager
) {
    suspend operator fun invoke(path : String, rootPath : String) = withContext(Dispatchers.IO) {
        gameInfoManager.loadGameInfo()
    }



}