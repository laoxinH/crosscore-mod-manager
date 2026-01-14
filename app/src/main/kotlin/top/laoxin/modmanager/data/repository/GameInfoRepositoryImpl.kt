package top.laoxin.modmanager.data.repository

import android.util.Log
import com.google.gson.Gson
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.DownloadGameConfigBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.data.service.AppInfoService
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.GameInfoRepository
import top.laoxin.modmanager.domain.repository.ImportConfigResult
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.SpecialGameService

@Singleton
class GameInfoRepositoryImpl
@Inject
constructor(
        private val fileService: FileService,
        private val appInfoService: AppInfoService,
        private val specialGameService: SpecialGameService,
        private val externalScope: CoroutineScope // From di/AppModule.kt
) : GameInfoRepository {

    private val _gameInfoList = MutableSharedFlow<List<GameInfoBean>>(replay = 1)

    init {
        externalScope.launch() { reloadGameInfo() }
    }

    override fun getGameInfoList(): SharedFlow<List<GameInfoBean>> = _gameInfoList

    override suspend fun reloadGameInfo() =
            withContext(Dispatchers.IO) {
                try {
                    val defaultGames =
                            listOf(
                                    GameInfoConstant.NO_GAME,
                                    GameInfoConstant.CROSSCORE,
                                    GameInfoConstant.CROSSCOREB
                            )

                    val configFiles =
                            fileService
                                    .listFiles(
                                            PathConstants.APP_PATH + PathConstants.GAME_CONFIG_PATH
                                    )
                                    .getOrNull()
                                    ?: return@withContext
                    val customGames =
                            configFiles.mapNotNull { file ->
                                if (file.name.endsWith(".json")) {
                                    try {
                                        val gameInfo =
                                                Gson().fromJson(
                                                                file.readText(),
                                                                GameInfoBean::class.java
                                                        )
                                        checkGameConfig(gameInfo, PathConstants.ROOT_PATH)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "GameInfoRepo",
                                                "Failed to parse game config: ${file.name}",
                                                e
                                        )
                                        null
                                    }
                                } else null
                            }

                    val combinedList = (defaultGames + customGames).distinctBy { it.packageName }
                    _gameInfoList.emit(combinedList)
                } catch (e: Exception) {
                    Log.e("GameInfoRepo", "Failed to load game configs", e)
                    if (_gameInfoList.replayCache.isEmpty()) {
                        _gameInfoList.emit(
                                listOf(
                                        GameInfoConstant.NO_GAME,
                                        GameInfoConstant.CROSSCORE,
                                        GameInfoConstant.CROSSCOREB
                                )
                        )
                    }
                }
            }

    override suspend fun enrichGameInfo(baseGameInfo: GameInfoBean, modPath: String): GameInfoBean {

        if (baseGameInfo.packageName.isEmpty()) return baseGameInfo
        val gameInfo = baseGameInfo.copy(
            gameFilePath = baseGameInfo.gameFilePath.filter { !it.contains("null") },
            modType = baseGameInfo.modType.filter { !it.isNullOrEmpty() }
        )
        return if (appInfoService.isAppInstalled(gameInfo.packageName).isSuccess) {
            val modifyGameInfo =
                    gameInfo.copy(
                            version =
                                    appInfoService
                                            .getVersionName(gameInfo.packageName)
                                            .getOrDefault(gameInfo.version),
                            modSavePath = modPath + gameInfo.packageName + File.separator
                    )
            createModsDirectory(modifyGameInfo, modPath)
            // Log.i("UpdateGameInfoUserCase", "修改的gameInfo: ${modifyGameInfo.version}")
            specialGameService.updateGameInfo(modifyGameInfo)
        } else {
            // If the selected app is not installed, return the "No Game" object.
            GameInfoConstant.NO_GAME
        }
    }

    override suspend fun getRemoteGameConfigs(): Result<List<DownloadGameConfigBean>> {
        return withContext(Dispatchers.IO) {
            try {
                val configs = ModManagerApi.retrofitService.getGameConfigs()
                Log.d("GameInfoRepo", "获取远程配置列表成功: ${configs.size} 个配置")
                Result.Success(configs)
            } catch (e: Exception) {
                Log.e("GameInfoRepo", "获取远程配置列表失败", e)
                Result.Error(AppError.NetworkError.ConnectionFailed(e.message ?: "网络请求失败"))
            }
        }
    }

    override suspend fun downloadRemoteGameConfig(
            config: DownloadGameConfigBean
    ): Result<GameInfoBean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GameInfoRepo", "开始下载配置: ${config.gameName}")

                // 1. 从API下载配置
                val gameInfo = ModManagerApi.retrofitService.downloadGameConfig(config.packageName)
                Log.d("GameInfoRepo", "下载配置成功: ${gameInfo.gameName}")

                // 2. 校验配置
                val validatedConfig =
                        try {
                            checkGameConfig(gameInfo, PathConstants.ROOT_PATH)
                        } catch (e: Exception) {
                            Log.e("GameInfoRepo", "配置校验失败: ${e.message}")
                            return@withContext Result.Error(
                                    AppError.GameConfigError.InvalidConfig(e.message ?: "配置校验失败")
                            )
                        }

                // 3. 确保目标目录存在
                val configDir = File(PathConstants.APP_PATH + PathConstants.GAME_CONFIG_PATH)
                if (!configDir.exists()) {
                    configDir.mkdirs()
                }

                // 4. 保存配置文件到目标目录
                val targetFile = File(configDir, "${config.packageName}.json")
                val jsonContent = Gson().toJson(gameInfo)
                targetFile.writeText(jsonContent)
                Log.d("GameInfoRepo", "配置文件已保存: ${targetFile.absolutePath}")

                // 5. 刷新游戏列表
                reloadGameInfo()
                Log.d("GameInfoRepo", "游戏列表已刷新")

                Result.Success(validatedConfig)
            } catch (e: Exception) {
                Log.e("GameInfoRepo", "下载配置失败", e)
                Result.Error(AppError.NetworkError.ConnectionFailed(e.message ?: "下载失败"))
            }
        }
    }

    private suspend fun createModsDirectory(gameInfo: GameInfoBean, path: String) {
        Log.d("UpdateGameInfoUserCase", "创建文件夹: $path + ${gameInfo.packageName}")
        withContext(Dispatchers.IO) {
            try {
                File(path + gameInfo.packageName).mkdirs()
                File(path + PathConstants.GAME_CONFIG_PATH).mkdirs()
            } catch (e: Exception) {
                Log.e("UpdateGameInfoUserCase", "创建文件夹失败: $e")
            }
        }
    }

    private fun checkGameConfig(gameInfo: GameInfoBean, rootPath: String): GameInfoBean {
        if (gameInfo.gameName.isEmpty()) throw Exception("gameName cannot be empty")
        if (gameInfo.packageName.isEmpty()) throw Exception("packageName cannot be empty")

        var result = gameInfo
        if (gameInfo.gamePath.isEmpty()) {
            throw Exception("gamePath cannot be empty")
        } else {
            result =
                    result.copy(gamePath = rootPath + "/Android/data/" + gameInfo.packageName + "/")
        }

        if (gameInfo.antiHarmonyFile.isNotEmpty()) {
            result =
                    result.copy(
                            antiHarmonyFile =
                                    (rootPath + "/" + gameInfo.antiHarmonyFile).replace("//", "/")
                    )
        }

        if (gameInfo.modType.isEmpty()) throw Exception("modType cannot be empty")
        if (gameInfo.gameFilePath.isEmpty()) throw Exception("gameFilePath cannot be empty")

        val paths = gameInfo.gameFilePath.map { "$rootPath/$it/".replace("//", "/") }
        result = result.copy(gameFilePath = paths)

        if (gameInfo.serviceName.isEmpty()) throw Exception("serviceName cannot be empty")
        if (gameInfo.gameFilePath.size != gameInfo.modType.size)
                throw Exception("gameFilePath and modType must have the same size")

        return result
    }

    override suspend fun importCustomGameConfigs(
            customConfigPath: String
    ): Result<ImportConfigResult> {
        return withContext(Dispatchers.IO) {
            try {
                val customDir = File(customConfigPath)
                if (!customDir.exists() || !customDir.isDirectory) {
                    Log.d("GameInfoRepo", "自定义配置目录不存在: $customConfigPath")
                    return@withContext Result.Error(
                            AppError.GameConfigError.InvalidConfig("目录不存在")
                    )
                }

                // 获取所有JSON文件
                val jsonFiles = customDir.listFiles { file ->
                    file.isFile && file.extension.lowercase() == "json"
                } ?: emptyArray()

                if (jsonFiles.isEmpty()) {
                    Log.d("GameInfoRepo", "未找到配置文件")
                    return@withContext Result.Success(
                            ImportConfigResult(successCount = 0, failedCount = 0)
                    )
                }

                Log.d("GameInfoRepo", "找到 ${jsonFiles.size} 个配置文件")

                // 确保目标目录存在
                val targetDir = File(PathConstants.APP_PATH + PathConstants.GAME_CONFIG_PATH)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                var successCount = 0
                var failedCount = 0
                val successConfigs = mutableListOf<String>()
                val gson = Gson()

                for (jsonFile in jsonFiles) {
                    try {
                        // 读取并解析JSON
                        val jsonContent = jsonFile.readText()
                        val gameInfo = gson.fromJson(jsonContent, GameInfoBean::class.java)

                        // 校验配置
                        checkGameConfig(gameInfo, PathConstants.ROOT_PATH)

                        // 复制到目标目录(覆盖同名文件)
                        val targetFile = File(targetDir, jsonFile.name)
                        jsonFile.copyTo(targetFile, overwrite = true)

                        Log.d("GameInfoRepo", "导入配置成功: ${gameInfo.gameName}")
                        successCount++
                        successConfigs.add(gameInfo.gameName)
                    } catch (e: Exception) {
                        Log.e("GameInfoRepo", "导入配置失败: ${jsonFile.name}", e)
                        failedCount++
                    }
                }

                // 重新加载配置
                if (successCount > 0) {
                    reloadGameInfo()
                }

                Result.Success(
                        ImportConfigResult(
                                successCount = successCount,
                                failedCount = failedCount,
                                successConfigs = successConfigs
                        )
                )
            } catch (e: Exception) {
                Log.e("GameInfoRepo", "导入自定义配置失败", e)
                Result.Error(AppError.GameConfigError.SaveFailed(e.message ?: "导入失败"))
            }
        }
    }
}
