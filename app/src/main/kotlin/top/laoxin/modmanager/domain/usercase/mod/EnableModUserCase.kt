package top.laoxin.modmanager.domain.usercase.mod


import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.BackupBean
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.backup.BackupRepository
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.exception.CopyStreamFailedException
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.filetools.impl.ShizukuFileTools
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class EnableModResult(
    val code: Int,
    // 开启失败的mods
    val failMods: List<ModBean>,
    // 开启成功的mods
    val successMods: List<ModBean>,
    // 含有密码的mod
    val passwordMod: ModBean,
    val message: String,
)

@Singleton
class EnableModsUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val appPathsManager: AppPathsManager,
    private val modRepository: ModRepository,
    private val backupRepository: BackupRepository,
    private val permissionTools: PermissionTools,
    private val fileToolsManager: FileToolsManager,
    private val specialGameToolsManager: SpecialGameToolsManager,

    ) {

    companion object {
        const val TAG = "EnableModsUserCase"
    }

    private var setUnzipProgress: ((String) -> Unit)? = null

    suspend operator fun invoke(
        mods: List<ModBean>,
        delUnzipDictionary: Boolean,
        setMultitaskingProgress: (String) -> Unit,
        setUnzipProgress: (String) -> Unit,
        setTipsText: (String) -> Unit
    ): EnableModResult = withContext(Dispatchers.IO) {
        this@EnableModsUserCase.setUnzipProgress = setUnzipProgress
        // 密码检查
        for (mod in mods) {
            if (mod.isEncrypted && mod.password == null) {
                return@withContext EnableModResult(
                    code = ResultCode.MOD_NEED_PASSWORD,
                    failMods = emptyList(),
                    successMods = emptyList(),
                    passwordMod = mod,
                    message = ""
                )
            }
        }

        setMultitaskingProgress("0/${mods.size}")
        val gameInfo = gameInfoManager.getGameInfo()
        val failMods: MutableList<ModBean> = mutableListOf()
        val successMods: MutableList<ModBean> = mutableListOf()

        mods.forEachIndexed { index, modBean ->
            kotlin.runCatching {
                val gameModPath = modBean.gameModPath!!
                Log.d(TAG, "游戏文件路径: $gameModPath ")

                // 备份游戏文件
                if (gameInfo.enableBackup) {
                    setTipsText(App.get().getString(R.string.tips_backups_game_files))
                    val backups = backupGameFiles(
                        gameModPath, modBean, gameInfo.packageName + "/"
                    )
                    Log.d(TAG, "invoke: $backups")
                    backupRepository.insertAll(backups)
                }
                // 解压mod压缩包
                setTipsText(
                    "${
                        App.get().getString(R.string.tips_unzip_mod)
                    } ${modBean.name}"
                )
                var unZipPath: String = ""
                if (modBean.isZipFile) {

                    val decompression = ArchiveUtil.decompression(
                        modBean.path!!,
                        appPathsManager.getModsUnzipPath() + gameInfo.packageName + "/" + File(
                            modBean.path
                        ).nameWithoutExtension + "/",
                        modBean.password,
                    )
                    if (decompression) {
                        unZipPath =
                            appPathsManager.getModsUnzipPath() + gameInfo.packageName + "/" + File(
                                modBean.path
                            ).nameWithoutExtension + "/"
                    } else {
                        throw Exception(
                            App.get().getString(R.string.toast_decompression_failed)
                        )
                    }
                }
                // 执行特殊操作
                setTipsText(App.get().getString(R.string.tips_special_operation))
                specialOperationEnable(modBean, gameInfo.packageName)
                // 复制mod文件
                setTipsText(App.get().getString(R.string.tips_copy_mod_to_game))
                if (!copyModFiles(modBean, gameModPath, unZipPath)) {
                    setTipsText(App.get().getString(R.string.tips_copy_mod_failed))
                    copyModsByStream(
                        modBean.path!!, gameModPath, modBean.modFiles!!, modBean.password
                    )
                }
                modRepository.updateMod(modBean.copy(isEnable = true))
                setMultitaskingProgress("${index + 1}/${mods.size}")
                successMods.add(modBean)


            }.onFailure {
                failMods.add(modBean)
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(it.message)
                }
                deleteTempFile()
                LogTools.logRecord("开启mod失败:${modBean.name}--$it")
            }

            if (delUnzipDictionary) {
                val fileTools = fileToolsManager.getFileTools(PathType.FILE)
                fileTools?.deleteFile(appPathsManager.getModsTempPath())
            }
        }

        return@withContext EnableModResult(
            code = ResultCode.SUCCESS,
            failMods = failMods,
            successMods = successMods,
            passwordMod = mods[0],
            message = ""
        )


    }

    private suspend fun backupGameFiles(
        gameModPath: String,
        modBean: ModBean,
        gameBackupPath: String,
    ): List<BackupBean> {
        val list: MutableList<BackupBean> = mutableListOf()
        // 通过ZipTools解压文件到modTempPath
        val checkPermission = permissionTools.checkPermission(gameModPath)
        val fileTools = fileToolsManager.getFileTools(checkPermission)
        Log.d(TAG, "游戏mod路径: ${modBean.modFiles}")
        val backups = backupRepository.getByModNameAndGamePackageName(
            modBean.name!!,
            modBean.gamePackageName!!
        ).first()
        modBean.modFiles?.forEachIndexed { index: Int, it: String ->
            val file = File(it)
            val backupPath =
                appPathsManager.getBackupPath() + gameBackupPath + File(modBean.gameModPath!!).name + "/" + file.name
            val gamePath = gameModPath + file.name
            if (!File(backupPath).exists()) {
                withContext(Dispatchers.IO) {
                    if (if (checkPermission == PathType.DOCUMENT) {
                            fileTools?.copyFileByDF(gamePath, backupPath) == true
                        } else {
                            fileTools?.copyFile(gamePath, backupPath) == true
                        } && fileTools?.isFileExist(backupPath) == true
                    ) {
                        this@EnableModsUserCase.setUnzipProgress?.let { it1 -> it1("${index + 1}/${modBean.modFiles.size}") }
                    }
                }
            }
            if (backups.isEmpty()) {
                list.add(
                    BackupBean(
                        id = 0,
                        filename = file.name,
                        gamePath = modBean.gameModPath,
                        gameFilePath = gamePath,
                        backupPath = backupPath,
                        gamePackageName = modBean.gamePackageName,
                        modName = modBean.name
                    )
                )
            } else {
                val backup = backups.find { it.filename == file.name }
                if (backup == null) {
                    list.add(
                        BackupBean(
                            id = 0,
                            filename = file.name,
                            gamePath = modBean.gameModPath,
                            gameFilePath = gamePath,
                            backupPath = backupPath,
                            gamePackageName = modBean.gamePackageName,
                            modName = modBean.name
                        )
                    )
                }
            }
        }
        return list
    }

    suspend fun specialOperationEnable(modBean: ModBean, packageName: String) {
        var specialOperationFlag = true
        withContext(Dispatchers.IO) {
            specialOperationFlag = specialGameToolsManager.getSpecialGameTools(packageName)
                ?.specialOperationEnable(modBean, packageName) != false

        }
        if (!specialOperationFlag) {
            throw Exception(App.get().getString(R.string.toast_special_operation_failed))
        }
    }


    fun copyModFiles(
        modBean: ModBean,
        gameModPath: String,
        unZipPath: String,
    ): Boolean {
        val checkPermission = permissionTools.checkPermission(gameModPath)
        val fileTools = fileToolsManager.getFileTools(checkPermission)
        val flags = mutableListOf<Boolean>()
        modBean.modFiles?.forEachIndexed { index: Int, modFilePath: String ->
            val modFile = File(unZipPath + modFilePath)
            val gameFile = File(gameModPath + modFile.name)
            var flag = false
            flag = if (checkPermission == PathType.DOCUMENT) {
                fileTools?.copyFileByFD(modFile.absolutePath, gameFile.absolutePath) == true

            } else {
                fileTools?.copyFile(modFile.absolutePath, gameFile.absolutePath) == true

            }
            flags.add(flag)
            this@EnableModsUserCase.setUnzipProgress?.let { it("${index + 1}/${modBean.modFiles.size}") }
        }
        return flags.all { it }

    }

    // 通过流写入mod文件
    fun copyModsByStream(
        path: String,
        gameModPath: String,
        modFiles: List<String>,
        password: String?,

        ): Boolean {
        val checkPermission = permissionTools.checkPermission(gameModPath)
        Log.d(TAG, "copyModsByStream: $checkPermission")
        when (checkPermission) {
            PathType.SHIZUKU -> {
                return copyModStreamByShizuku(path, gameModPath, modFiles, password)
            }

            PathType.DOCUMENT -> {
                return copyModStreamByDocumentFile(path, gameModPath, modFiles, password)
            }

            PathType.FILE -> {
                return copyModStreamByFile(path, gameModPath, modFiles, password)
            }

            else -> {
                return false
            }
        }

    }

    fun copyModStreamByShizuku(
        modTempPath: String, gameModPath: String, files: List<String>, password: String?
    ): Boolean {
        val fileTools = fileToolsManager.getShizukuFileTools() as ShizukuFileTools
        val flags: MutableList<Boolean> = ArrayList()
        try {
            files.forEach {
                flags.add(
                    fileTools.unzipFile(modTempPath, gameModPath, it, password)
                )
            }

        } catch (_: RemoteException) {
            flags.add(false)
        }
        if (!flags.all { it }) {
            // 抛出流复制失败异常
            throw CopyStreamFailedException(App.get().getString(R.string.toast_copy_failed))
        }
        return true
    }

    private fun copyModStreamByDocumentFile(
        path: String, gameModPath: String, modFiles: List<String>, password: String?
    ): Boolean {
        val flags: MutableList<Boolean> = ArrayList()
        try {
            modFiles.forEach {
                flags.add(unZipModsByFileHeardByDocument(path, gameModPath, it, password))
            }

        } catch (_: RemoteException) {
            flags.add(false)
        }
        if (!flags.all { it }) {
            // 抛出流复制失败异常
            throw CopyStreamFailedException(App.get().getString(R.string.toast_copy_failed))
        }
        return true
    }

    private fun copyModStreamByFile(
        path: String, gameModPath: String, modFiles: List<String>, password: String?
    ): Boolean {
        val flags: MutableList<Boolean> = ArrayList()
        try {
            modFiles.forEach {
                flags.add(
                    unZipModsByFileHeardByFile(path, gameModPath, it, password)
                )
            }

        } catch (_: RemoteException) {
            flags.add(false)
        }
        if (!flags.all { it }) {
            // 抛出流复制失败异常
            throw CopyStreamFailedException(App.get().getString(R.string.toast_copy_failed))
        }
        return true

    }

    private fun deleteTempFile(): Boolean {
        return if (File(appPathsManager.getModsTempPath()).exists()) {
            val fileTools = fileToolsManager.getFileTools(PathType.FILE)
            fileTools?.deleteFile(appPathsManager.getModsTempPath()) == true
        } else {
            true
        }
    }

    private fun unZipModsByFileHeardByDocument(
        modTempPath: String, gameModPath: String, files: String?, password: String?
    ): Boolean {

        val fileTools = fileToolsManager.getFileTools(PathType.DOCUMENT)
        val fileHeaders = ArchiveUtil.listInArchiveFiles(modTempPath)
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (fileHeaderObj == files) {
                try {
                    fileTools?.createFileByStream(
                        gameModPath,
                        File(files).name,
                        ArchiveUtil.getArchiveItemInputStream(
                            modTempPath,
                            fileHeaderObj,
                            password
                        ),
                    )
                    flag.add(true)
                } catch (e: Exception) {
                    Log.e(TAG, "通过流解压失败: $e")
                    e.printStackTrace()
                    flag.add(false)
                }
            }
        }
        return flag.all { it }
    }

    private fun unZipModsByFileHeardByFile(
        modTempPath: String, gameModPath: String, files: String?, password: String?
    ): Boolean {
        val fileTools = fileToolsManager.getFileTools(PathType.FILE)
        val fileHeaders = ArchiveUtil.listInArchiveFiles(modTempPath)
        val flag: MutableList<Boolean> = mutableListOf()
        for (fileHeaderObj in fileHeaders) {
            if (fileHeaderObj == files) {
                try {
                    fileTools?.createFileByStream(
                        gameModPath,
                        File(files).name,
                        ArchiveUtil.getArchiveItemInputStream(
                            modTempPath,
                            fileHeaderObj,
                            password
                        ),
                    )
                    flag.add(true)
                } catch (e: Exception) {
                    Log.e("ZipTools", "通过流解压失败: $e")
                    e.printStackTrace()
                    flag.add(false)
                }
            }
        }
        return flag.all { it }
    }
}