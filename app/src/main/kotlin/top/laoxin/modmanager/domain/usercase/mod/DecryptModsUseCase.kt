package top.laoxin.modmanager.domain.usercase.mod

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.service.DecryptEvent
import top.laoxin.modmanager.domain.service.DecryptResult
import top.laoxin.modmanager.domain.service.DecryptStep
import top.laoxin.modmanager.domain.service.ModDecryptService

/** 解密 MOD UseCase 验证密码后更新 MOD 信息（图片、描述等） */
class DecryptModsUseCase
@Inject
constructor(
        private val decryptService: ModDecryptService,
        private val modRepository: ModRepository
) {
    companion object {
        private const val TAG = "DecryptModsUseCase"
    }

    /**
     * 执行解密
     * @param archivePath 压缩包路径（用于获取所有相关 MOD）
     * @param password 用户输入的密码
     * @return Flow<DecryptState> 解密状态流
     */
    fun execute(archivePath: String, password: String): Flow<DecryptState> = flow {
        // 1. 获取该压缩包的所有 MOD
        emit(
                DecryptState.Progress(
                        step = DecryptStep.VALIDATING_PASSWORD,
                        modName = "",
                        current = 0,
                        total = 0
                )
        )

        val mods = modRepository.getModsByPath(archivePath).first()
        if (mods.isEmpty()) {
            emit(DecryptState.Error(AppError.ModError.DecryptFailed("未找到相关 MOD")))
            return@flow
        }

        Log.d(TAG, "Found ${mods.size} mods for path: $archivePath")

        // 2. 收集解密事件并转换为 UI 状态
        decryptService.decryptMods(archivePath, password, mods).collect { event ->
            when (event) {
                is DecryptEvent.Validating -> {
                    emit(
                            DecryptState.Progress(
                                    step = DecryptStep.VALIDATING_PASSWORD,
                                    modName = "",
                                    current = 0,
                                    total = mods.size
                            )
                    )
                }
                is DecryptEvent.Progress -> {
                    emit(
                            DecryptState.Progress(
                                    step = event.step,
                                    modName = event.modName,
                                    current = event.current,
                                    total = event.total
                            )
                    )
                }
                is DecryptEvent.ModUpdated -> {
                    // 更新数据库
                    emit(
                            DecryptState.Progress(
                                    step = DecryptStep.UPDATING_DATABASE,
                                    modName = event.mod.name,
                                    current = 0,
                                    total = 0
                            )
                    )
                    modRepository.updateMod(event.mod)
                    Log.d(TAG, "Updated mod: ${event.mod.name}")
                }
                is DecryptEvent.Complete -> {
                    emit(DecryptState.Success(event.result))
                }
                is DecryptEvent.Error -> {
                    emit(DecryptState.Error(event.error))
                }
            }
        }
    }
}

/** 解密状态密封类（供 UI 使用） */
sealed class DecryptState {
    /** 进度更新 */
    data class Progress(
            val step: DecryptStep,
            val modName: String,
            val current: Int,
            val total: Int
    ) : DecryptState()

    /** 成功 */
    data class Success(val result: DecryptResult) : DecryptState()

    /** 错误 */
    data class Error(val error: AppError) : DecryptState()
}
