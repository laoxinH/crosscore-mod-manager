package top.laoxin.modmanager.domain.usercase.app

import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.data.service.LogService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateLogServiceUserCase @Inject constructor(
    private  val logService: LogService,

) {
     suspend operator fun invoke(logPath: String)  {
        logService.setLogPath(PathConstants.getFullModPath(logPath))
    }
}