package top.laoxin.modmanager.domain.usercase.gameinfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.manager.GameInfoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadGameConfigUserCase @Inject constructor(
    @param:FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools,
    private val gameInfoManager: GameInfoManager
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        gameInfoManager.loadGameInfo()
    }

}