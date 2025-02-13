package top.laoxin.modmanager.domain.usercase.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.data.repository.VersionRepository
import top.laoxin.modmanager.tools.LogTools
import top.lings.updater.util.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateLogToolUserCase @Inject constructor() {
    suspend operator fun invoke(logPath: String) = withContext(Dispatchers.IO) {
        LogTools.setLogPath(logPath)
    }
}