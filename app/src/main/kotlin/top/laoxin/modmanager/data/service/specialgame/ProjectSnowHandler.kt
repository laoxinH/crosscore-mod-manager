package top.laoxin.modmanager.data.service.specialgame

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.GameStartCheckResult
import top.laoxin.modmanager.domain.service.PermissionService

/**废弃
 *
 */

/** 尘白禁区特殊游戏处理器 处理尘白禁区游戏的 manifest.json 注入 */

@Singleton
class ProjectSnowHandler
@Inject
constructor(
        private val fileService: FileService,
        private val permissionService: PermissionService
) : BaseSpecialGameHandler {

    companion object {
        private const val TAG = "ProjectSnowHandler"
        private const val CHECK_FILENAME = "manifest.json"
    }

    private val gson: Gson = GsonBuilder().create()
    private var checkFilePath = ""
    private var checkFileModPath = ""

    data class MainIFest(
            var version: String,
            var projectVersion: String,
            var pathOffset: String,
            var bUserCache: Boolean,
            var paks: MutableList<Pak> = mutableListOf()
    )

    data class Pak(
            val name: String,
            val hash: String,
            val sizeInBytes: Long,
            val bPrimary: Boolean,
            val base: String = "",
            val diff: String = "",
            val diffSizeBytes: Long = 0
    )

    override suspend fun handleModEnable(mod: ModBean, packageName: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    checkFileModPath =
                            PathConstants.GAME_CHECK_FILE_PATH + packageName + "/" + CHECK_FILENAME
                    val unZipPath =
                            PathConstants.MODS_UNZIP_PATH +
                                    packageName +
                                    "/" +
                                    File(mod.path).nameWithoutExtension +
                                    "/"

                    var paks = mutableListOf<Pak>()
                    val checkFile = File(checkFileModPath)
                    if (checkFile.exists()) {
                        paks = gson.fromJson(checkFile.readText(), MainIFest::class.java).paks
                    } else {
                        checkFile.parentFile?.mkdirs()
                        checkFile.createNewFile()
                    }

                    for (modFile in mod.modFiles) {
                        val modFilePath = if (mod.isZipFile) unZipPath + modFile else modFile

                        var md5 = calculateMD5(File(modFilePath).inputStream())
                        if (md5 == null) {
                            getZipFileInputStream(mod.path, modFile, mod.password)?.use {
                                md5 = calculateMD5(it)
                            }
                        }

                        var fileSize =
                                try {
                                    File(modFilePath).length()
                                } catch (_: Exception) {
                                    null
                                }
                        if (fileSize == null) {
                            getZipFileInputStream(mod.path, modFile, mod.password)?.use {
                                fileSize = getInputStreamSize(it)
                            }
                        }

                        paks.add(Pak(File(modFilePath).name, md5!!, fileSize!!, false))

                        MainIFest(
                                        version = "1.0",
                                        projectVersion = "1.0",
                                        pathOffset = "",
                                        bUserCache = true,
                                        paks = paks
                                )
                                .let {
                                    File(checkFileModPath)
                                            .writeText(gson.toJson(it, MainIFest::class.java))
                                }
                    }

                    Result.Success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "handleModEnable: $e")
                    Result.Error(AppError.Unknown(e))
                }
            }

    override suspend fun handleModDisable(
            backup: List<BackupBean>,
            packageName: String,
            mod: ModBean
    ): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    checkFileModPath =
                            PathConstants.GAME_CHECK_FILE_PATH + packageName + "/" + CHECK_FILENAME

                    val mainIFest =
                            gson.fromJson(File(checkFileModPath).readText(), MainIFest::class.java)

                    val iterator = mainIFest.paks.iterator()
                    while (iterator.hasNext()) {
                        val pak = iterator.next()
                        mod.modFiles.forEach {
                            if (pak.name == File(it).name) {
                                iterator.remove()
                            }
                        }
                    }

                    File(checkFileModPath).writeText(gson.toJson(mainIFest, MainIFest::class.java))
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "handleModDisable: $e")
                    Result.Error(AppError.Unknown(e))
                }
            }

    override suspend fun handleGameStart(gameInfo: GameInfoBean): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "handleGameStart: 开始执行注入")
                    checkFileModPath =
                            PathConstants.GAME_CHECK_FILE_PATH +
                                    gameInfo.packageName +
                                    "/" +
                                    CHECK_FILENAME
                    checkFilePath =
                            "${PathConstants.ROOT_PATH}/Android/data/${gameInfo.packageName}/files/"

                    val accessType = getAccessType()
                    if (accessType == FileAccessType.NONE) {
                        return@withContext Result.Error(AppError.PermissionError.StoragePermissionDenied)
                    }

                    // 复制游戏的 manifest 到临时目录
                    when (accessType) {
                        FileAccessType.DOCUMENT_FILE -> {
                            fileService.copyFile(
                                    checkFilePath + CHECK_FILENAME,
                                    PathConstants.MODS_UNZIP_PATH + CHECK_FILENAME
                            )
                        }
                        else -> {
                            fileService.copyFile(
                                    checkFilePath + CHECK_FILENAME,
                                    PathConstants.MODS_UNZIP_PATH + CHECK_FILENAME
                            )
                        }
                    }

                    val modPaks =
                            gson.fromJson(File(checkFileModPath).readText(), MainIFest::class.java)
                                    .paks

                    val mainIFest =
                            gson.fromJson(
                                    File(PathConstants.MODS_UNZIP_PATH + CHECK_FILENAME).readText(),
                                    MainIFest::class.java
                            )

                    if (modPaks.isEmpty()) {
                        return@withContext Result.Success(Unit)
                    }

                    for (modPak in modPaks) {
                        if (modPak !in mainIFest.paks) {
                            mainIFest.paks.add(0, modPak)
                        }
                    }

                    val mainIFestJson = gson.toJson(mainIFest, MainIFest::class.java)
                    val startTime = System.currentTimeMillis()

                    // 持续写入 40 秒
                    while (true) {
                        fileService.writeFile(checkFilePath, CHECK_FILENAME, mainIFestJson)
                        val elapsedTime = System.currentTimeMillis() - startTime
                        if (elapsedTime > 40000) {
                            Log.d(TAG, "handleGameStart: 注入结束")
                            break
                        }
                    }

                    Result.Success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "handleGameStart: $e")
                    Result.Error(AppError.Unknown(e))
                }
            }

    override suspend fun checkBeforeGameStart(
            gameInfo: GameInfoBean
    ): Result<GameStartCheckResult> =
            withContext(Dispatchers.IO) {
                try {
                    val accessType = getAccessType()
                    if (accessType == FileAccessType.NONE) {
                        return@withContext Result.Success(GameStartCheckResult.NoPermission)
                    }

                    val gameFilePath = "${gameInfo.gamePath}/files/${getGameFileDir(gameInfo)}/"
                    val testResult = fileService.createDirectory("$gameFilePath/test")

                    when {
                        testResult is Result.Error ->
                                Result.Success(GameStartCheckResult.GameUpdated)
                        else -> {
                            fileService.deleteFile("$gameFilePath/test")
                            Result.Success(GameStartCheckResult.Success)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "checkBeforeGameStart: $e")
                    Result.Error(AppError.Unknown(e))
                }
            }

    override fun isModFileSupported(modFileName: String): Boolean {
        return modFileName.endsWith(".pak")
    }

    override suspend fun handleGameSelect(gameInfo: GameInfoBean): Result<GameInfoBean> =
            withContext(Dispatchers.IO) {
                try {
                    val accessType = getAccessType()
                    if (accessType == FileAccessType.NONE) {
                        return@withContext Result.Error(AppError.PermissionError.StoragePermissionDenied)
                    }

                    val gameFilePath = "${gameInfo.gamePath}/files/${getGameFileDir(gameInfo)}/"
                    val name = getGameFileDir(gameInfo)

                    val testResult = fileService.createDirectory("$gameFilePath/test")
                    if (testResult is Result.Error) {
                        fileService.renameDirectory(gameFilePath, name + "1")
                        File(gameFilePath).parentFile?.parentFile?.let {
                            fileService.createDirectory(it.absolutePath)
                        }
                        File(gameFilePath).parentFile?.let {
                            fileService.createDirectory(it.absolutePath)
                        }
                        fileService.createDirectory(gameFilePath)
                    } else {
                        Log.d(TAG, "开始删除测试文件")
                        fileService.deleteFile("$gameFilePath/test")
                    }

                    Result.Success(gameInfo)
                } catch (e: Exception) {
                    Log.e(TAG, "handleGameSelect: $e")
                    Result.Error(AppError.Unknown(e))
                }
            }

    override fun updateGameInfo(gameInfo: GameInfoBean): GameInfoBean {
        return gameInfo.copy(
                gameFilePath =
                        gameInfo.gameFilePath.map {
                            (it + File.separator + getGameFileDir(gameInfo) + File.separator)
                                    .replace("//", "/")
                        }
        )
    }

    override fun needGameService(): Boolean = true

    override fun needOpenVpn(): Boolean = true

    // ==================== 私有方法 ====================

    private fun getAccessType(): FileAccessType {
        return permissionService.getFileAccessType(checkFilePath)
    }

    private fun getGameFileDir(gameInfo: GameInfoBean): String {
        val version = gameInfo.version
        val regex = """^(\d+\.\d+\.\d+)""".toRegex()
        val matchResult = regex.find(version)
        val result = matchResult?.value ?: ""
        return result.split('.')
                .toMutableList()
                .apply { if (size >= 3) this[2] = "0" }
                .joinToString(".")
    }
}
