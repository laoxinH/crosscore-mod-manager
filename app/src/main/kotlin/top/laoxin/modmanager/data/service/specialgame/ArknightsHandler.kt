package top.laoxin.modmanager.data.service.specialgame

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
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


/** 明日方舟特殊游戏处理器 处理明日方舟游戏的校验文件修改 */
@Singleton
class ArknightsHandler
@Inject
constructor(
    private val fileService: FileService,
    private val permissionService: PermissionService
) : BaseSpecialGameHandler {

    companion object {
        private const val TAG = "ArknightsHandler"
        private const val CHECK_FILE_1 = "persistent_res_list.json"
        private const val CHECK_FILE_2 = "hot_update_list.json"
    }

    private val gson: Gson =
        GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .create()

    private var checkFilePath = ""
    private lateinit var hotUpdate: HotUpdate
    private lateinit var persistentRes: PersistentRes

    data class HotUpdate(
        var versionId: String = "",
        var abInfos: MutableList<AbInfo> = mutableListOf(),
        var manifestName: String = "",
        var manifestVersion: String = "",
        var packInfos: MutableList<AbInfo> = mutableListOf()
    )

    data class PersistentRes(
        var manifestName: String = "",
        var manifestVersion: String = "",
        var abInfos: MutableList<AbInfo> = mutableListOf()
    )

    data class AbInfo(
        var name: String? = null,
        var hash: String? = null,
        var md5: String? = null,
        var totalSize: Long? = null,
        var abSize: Long? = null,
        var type: String? = null,
        var pid: String? = null,
        var cid: Long? = null,
        var cat: Long? = null,
        var meta: Long? = null
    )

    override suspend fun handleModEnable(mod: ModBean, packageName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                checkFilePath =
                    "${PathConstants.ROOT_PATH}/Android/data/$packageName/files/Bundles/"


                val accessType = getAccessType()
                if (accessType == FileAccessType.NONE) {
                    return@withContext Result.Error(AppError.PermissionError.UriPermissionNotGranted)
                }

                // 复制校验文件到应用目录
                copyCheckFilesToAppPath()

                // 加载校验文件
                loadCheckFiles()

                // 修改校验信息
                val flag = mutableListOf<Boolean>()
                mod.gameFilesPath.forEachIndexed { index, gameFile ->
                    val md5Result = fileService.calculateFileMd5(gameFile)
                    if (md5Result is Result.Error)  return@withContext Result.Error(md5Result.error)
                    val md5 = (md5Result as Result.Success).data
                    val fileSizeResult = fileService.getFileLength(gameFile)
                    if (fileSizeResult is Result.Error)  return@withContext Result.Error(fileSizeResult.error)
                    val fileSize = (fileSizeResult as Result.Success).data
                    Log.d(TAG, "checkFileName: $gameFile, md5: $md5, fileSize: $fileSize")
                    flag.add(modifyCheckFile(gameFile, md5, fileSize))
                    onProgressUpdate("${index + 1}/${mod.modFiles.size}")
                }

                // 写入校验文件
                writeCheckFiles()

                // 复制回游戏目录
                if (flag.all { it }) {
                    copyCheckFilesToGamePath()
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.ModError.EnableFailed("部分校验文件修改失败"))
                }
            } catch (e: Exception) {
                //Log.e(TAG, "handleModEnable: $e")
                e.printStackTrace()
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
                checkFilePath =
                    "${PathConstants.ROOT_PATH}/Android/data/$packageName/files/Bundles/"

                val accessType = getAccessType()
                if (accessType == FileAccessType.NONE) {
                    return@withContext Result.Error(AppError.PermissionError.StoragePermissionDenied)
                }

                copyCheckFilesToAppPath()
                loadCheckFiles()

                val flag = mutableListOf<Boolean>()
                mod.gameFilesPath.forEachIndexed { index, gameFile ->
                    val md5Result = fileService.calculateFileMd5(gameFile)
                    if (md5Result is Result.Error)  return@withContext Result.Error(md5Result.error)
                    val md5 = (md5Result as Result.Success).data
                    val fileSizeResult = fileService.getFileLength(gameFile)
                    if (fileSizeResult is Result.Error)  return@withContext Result.Error(fileSizeResult.error)
                    val fileSize = (fileSizeResult as Result.Success).data
                    flag.add(modifyCheckFile(gameFile, md5, fileSize))
                    onProgressUpdate("${index + 1}/${backup.size}")
                }

                writeCheckFiles()

                if (flag.all { it }) {
                    copyCheckFilesToGamePath()
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.ModError.DisableFailed("部分校验文件修改失败"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleModDisable: $e")
                Result.Error(AppError.Unknown(e))
            }
        }

    override suspend fun handleGameStart(gameInfo: GameInfoBean): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun checkBeforeGameStart(
        gameInfo: GameInfoBean
    ): Result<GameStartCheckResult> {
        return Result.Success(GameStartCheckResult.Success)
    }

    override fun isModFileSupported(modFileName: String): Boolean = false

    override suspend fun handleGameSelect(gameInfo: GameInfoBean): Result<GameInfoBean> {
        return Result.Success(gameInfo)
    }

    override fun updateGameInfo(gameInfo: GameInfoBean): GameInfoBean = gameInfo

    override fun needGameService(): Boolean = false

    override fun needOpenVpn(): Boolean = false

    // ==================== 私有方法 ====================

    private fun getAccessType(): FileAccessType {
        return permissionService.getFileAccessType(checkFilePath)
    }

    private suspend fun copyCheckFilesToAppPath() {


        fileService.copyFile(
            checkFilePath + CHECK_FILE_1,
            PathConstants.MODS_TEMP_PATH + CHECK_FILE_1
        )
        fileService.copyFile(
            checkFilePath + CHECK_FILE_2,
            PathConstants.MODS_TEMP_PATH + CHECK_FILE_2
        )


    }

    private suspend fun copyCheckFilesToGamePath() {

        fileService.copyFile(
            PathConstants.MODS_TEMP_PATH + CHECK_FILE_1,
            checkFilePath + CHECK_FILE_1
        )

        fileService.copyFile(
            PathConstants.MODS_TEMP_PATH + CHECK_FILE_2,
            checkFilePath + CHECK_FILE_2
        )


    }

    private fun loadCheckFiles() {
        persistentRes =
            gson.fromJson(
                File(PathConstants.MODS_TEMP_PATH+ CHECK_FILE_1).readText(),
                PersistentRes::class.java
            )
        hotUpdate =
            gson.fromJson(
                File(PathConstants.MODS_TEMP_PATH + CHECK_FILE_2).readText(),
                HotUpdate::class.java
            )
    }

    private fun writeCheckFiles() {
        val checkFile1 = File(PathConstants.MODS_UNZIP_PATH + CHECK_FILE_1)
        val checkFile2 = File(PathConstants.MODS_UNZIP_PATH + CHECK_FILE_2)

        gson.toJson(persistentRes, PersistentRes::class.java).let {
            if (checkFile1.exists()) {
                checkFile1.delete()
                checkFile1.createNewFile()
                checkFile1.writeText(it)
            }
        }
        gson.toJson(hotUpdate, HotUpdate::class.java).let {
            if (checkFile2.exists()) {
                checkFile2.delete()
                checkFile2.createNewFile()
                checkFile2.writeText(it)
            }
        }
    }

    private fun modifyCheckFile(fileName: String, md5: String, fileSize: Long): Boolean {
        return try {
            persistentRes.abInfos.replaceAll {
                if (it.name?.contains(fileName) == true) {
                    it.copy(md5 = md5, abSize = fileSize)
                } else {
                    it
                }
            }
            hotUpdate.abInfos.replaceAll {
                if (it.name?.contains(fileName) == true) {
                    it.copy(md5 = md5, abSize = fileSize)
                } else {
                    it
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
