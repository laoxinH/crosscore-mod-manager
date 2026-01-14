package top.laoxin.modmanager.data.service.specialgame

import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.GameStartCheckResult
import top.laoxin.modmanager.domain.service.SpecialGameService
import kotlin.collections.iterator

/** 特殊游戏服务实现 管理和分发特殊游戏的操作请求 */
@Singleton
class SpecialGameServiceImpl
@Inject
constructor(
        private val arknightsHandler: ArknightsHandler,
) : SpecialGameService {

    private val specialGameHandlers: Map<String, BaseSpecialGameHandler> =
            mapOf(
                    "arknights" to arknightsHandler,
                    "com.mrfz" to arknightsHandler,
                    "Arknights" to arknightsHandler,
            )

    private fun getHandler(packageName: String): BaseSpecialGameHandler? {
        for ((key, value) in specialGameHandlers) {
            if (packageName.contains(key,true)) {
                return value
            }
        }
        return null
    }

    override suspend fun onModEnable(mod: ModBean, packageName: String): Result<Unit> {
        val handler = getHandler(packageName) ?: return Result.Success(Unit)
        return handler.handleModEnable(mod, packageName)
    }

    override suspend fun onModDisable(
            backup: List<BackupBean>,
            packageName: String,
            mod: ModBean
    ): Result<Unit> {
        val handler = getHandler(packageName) ?: return Result.Success(Unit)
        return handler.handleModDisable(backup, packageName, mod)
    }

    override suspend fun onGameStart(gameInfo: GameInfoBean): Result<Unit> {
        val handler = getHandler(gameInfo.packageName) ?: return Result.Success(Unit)
        return handler.handleGameStart(gameInfo)
    }

    override suspend fun checkBeforeGameStart(
            gameInfo: GameInfoBean
    ): Result<GameStartCheckResult> {
        val handler =
                getHandler(gameInfo.packageName)
                        ?: return Result.Success(GameStartCheckResult.Success)
        return handler.checkBeforeGameStart(gameInfo)
    }

    override fun isModFileSupported(packageName: String, modFileName: String): Boolean {
        val handler = getHandler(packageName) ?: return false
        return handler.isModFileSupported(modFileName)
    }

    override suspend fun onGameSelect(gameInfo: GameInfoBean): Result<GameInfoBean> {
        val handler = getHandler(gameInfo.packageName) ?: return Result.Success(gameInfo)
        return handler.handleGameSelect(gameInfo)
    }

    override fun updateGameInfo(gameInfo: GameInfoBean): GameInfoBean {
        val handler = getHandler(gameInfo.packageName) ?: return gameInfo
        return handler.updateGameInfo(gameInfo)
    }

    override fun needGameService(packageName: String): Boolean {
        val handler = getHandler(packageName) ?: return false
        return handler.needGameService()
    }

    override fun needOpenVpn(packageName: String): Boolean {
        val handler = getHandler(packageName) ?: return false
        return handler.needOpenVpn()
    }

    override fun isSpecialGame(packageName: String): Boolean {
        return getHandler(packageName) != null
    }
}
