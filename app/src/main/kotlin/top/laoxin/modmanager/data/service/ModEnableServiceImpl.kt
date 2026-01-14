package top.laoxin.modmanager.data.service

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ModForm
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.EnableFileEvent
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.ModEnableService
import top.laoxin.modmanager.domain.service.TraditionalModEnableService
import top.laoxin.modmanager.domain.service.ValidationResult

/** MOD 开启服务实现（调度器模式） 根据 MOD 形式分发到不同的处理服务 */
@Singleton
class ModEnableServiceImpl
@Inject
constructor(
        private val fileService: FileService,
        private val traditionalModEnableService: TraditionalModEnableService
) : ModEnableService {

    companion object {
        private const val TAG = "ModEnableService"
    }

    override fun enableSingleMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent> {
        return when (mod.modForm) {
            ModForm.TRADITIONAL -> {
                if (mod.isZipFile) {
                    traditionalModEnableService.enableZipMod(mod, gameInfo)
                } else {
                    traditionalModEnableService.enableFolderMod(mod, gameInfo)
                }
            }
            ModForm.ACTIVE -> {
                // TODO: 主动形式处理
                kotlinx.coroutines.flow.flow { emit(EnableFileEvent.Complete(false, AppError.ModError.InvalidStructure(mod.name))) }
            }
            ModForm.PACKAGED -> {
                // TODO: 打包形式处理
                kotlinx.coroutines.flow.flow { emit(EnableFileEvent.Complete(false, AppError.ModError.InvalidStructure(mod.name))) }
            }
        }
    }

    override fun disableSingleMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent> {
        return when (mod.modForm) {
            ModForm.TRADITIONAL -> {
                if (mod.isZipFile) {
                    traditionalModEnableService.disableZipMod(mod, gameInfo)
                } else {
                    traditionalModEnableService.disableFolderMod(mod, gameInfo)
                }
            }
            ModForm.ACTIVE -> {
                kotlinx.coroutines.flow.flow { emit(EnableFileEvent.Complete(false, AppError.ModError.InvalidStructure(mod.name))) }
            }
            ModForm.PACKAGED -> {
                kotlinx.coroutines.flow.flow { emit(EnableFileEvent.Complete(false, AppError.ModError.InvalidStructure(mod.name))) }
            }
        }
    }

    override suspend fun validateMod(mod: ModBean): ValidationResult {
        // 1. 检查文件是否存在
        if (mod.isZipFile) {
            // 压缩包：检查 path 是否存在
            val existResult = fileService.isFileExist(mod.path)
            if (existResult is Result.Error || (existResult is Result.Success && !existResult.data)
            ) {
                return ValidationResult.FILE_MISSING
            }

            // 2. 检查加密和密码
            if (mod.isEncrypted && mod.password.isEmpty()) {
                return ValidationResult.NEED_PASSWORD
            }
        } else {
            // 文件夹：检查 modFiles 是否存在
            val modFiles = mod.modFiles
            if (modFiles.isEmpty()) {
                return ValidationResult.FILE_MISSING
            }
            for (filePath in modFiles) {
                val existResult = fileService.isFileExist(filePath)
                if (existResult is Result.Error ||
                                (existResult is Result.Success && !existResult.data)
                ) {
                    return ValidationResult.FILE_MISSING
                }
            }
        }

        return ValidationResult.VALID
    }
}
