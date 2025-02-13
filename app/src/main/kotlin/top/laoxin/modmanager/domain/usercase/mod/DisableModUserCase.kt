package top.laoxin.modmanager.domain.usercase.mod

import android.app.Application
import android.os.RemoteException
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App

import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.constant.ScanModPath
import top.laoxin.modmanager.data.bean.BackupBean
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.bean.ModBeanTemp
import top.laoxin.modmanager.data.bean.ScanFileBean
import top.laoxin.modmanager.data.repository.VersionRepository
import top.laoxin.modmanager.data.repository.backup.BackupRepository
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.data.repository.scanfile.ScanFileRepository
import top.laoxin.modmanager.domain.usercase.app.CheckPermissionUserCase
import top.laoxin.modmanager.exception.CopyStreamFailedException
import top.laoxin.modmanager.exception.NoSelectedGameException
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools


import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.filetools.impl.ShizukuFileTools
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import top.laoxin.modmanager.ui.state.UserPreferencesState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import top.lings.updater.util.GithubApi
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class DisableModResult(
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
class DisableModsUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val appPathsManager: AppPathsManager,
    private val scanFileRepository: ScanFileRepository,
    private val modRepository: ModRepository,
    private val backupRepository: BackupRepository,
    private val checkPermissionUserCase: CheckPermissionUserCase,
    private val permissionTools: PermissionTools,
    private val flashModsObserverManager: FlashModsObserverManager,
    private val fileToolsManager: FileToolsManager,
    private val specialGameToolsManager: SpecialGameToolsManager,


    ) {

    companion object {
        const val TAG = "FlashModsUserCase"
    }

    private var setUnzipProgress: ((String) -> Unit)? = null

    suspend operator fun invoke(
        mods: List<ModBean>,
        isDel :Boolean,
        removeDelEnableModsList : (ModBean) -> Unit,
        setMultitaskingProgress : (String) -> Unit,
        setUnzipProgress : (String) -> Unit,
        setTipsText : (String) -> Unit
    ): DisableModResult = withContext(Dispatchers.IO) {
        this@DisableModsUserCase.setUnzipProgress = setUnzipProgress
        val failMods : MutableList<ModBean> = mutableListOf()
        val successMods : MutableList<ModBean> = mutableListOf()
        val gameInfo = gameInfoManager.getGameInfo()
        setMultitaskingProgress("0/${mods.size}")
        mods.forEachIndexed { index, modBean ->
            val backupBeans = backupRepository.getByModNameAndGamePackageName(
                modBean.name!!, gameInfo.packageName
            ).first()
            Log.d("ModViewModel", "disableMod: 开始执行关闭$backupBeans")
            kotlin.runCatching {
                setTipsText(App.get().getString(R.string.tips_special_operation))
                // 特殊游戏操作
                specialOperationDisable(
                    backupBeans, gameInfo.packageName, modBean
                )
                // 还原游戏文件
                setTipsText(App.get().getString(R.string.tips_restore_game_files))
                restoreGameFiles(backupBeans)
                modRepository.updateMod(modBean.copy(isEnable = false))
                setMultitaskingProgress("${index + 1}/${mods.size}")
                successMods.add(modBean)
                if (isDel) {
                    removeDelEnableModsList(modBean)
                }
            }.onFailure {
                failMods.add(modBean)
                LogTools.logRecord("关闭mod失败:${modBean.name}--$it")
                it.printStackTrace()
            }

        }


        return@withContext DisableModResult(
            code = ResultCode.SUCCESS,
            failMods = failMods,
            successMods = successMods,
            passwordMod = mods[0],
            message = ""
        )


    }

    fun restoreGameFiles(backups: List<BackupBean?>) {
        if (backups.isEmpty()) return
        val checkPermission = permissionTools.checkPermission(backups[0]?.gameFilePath ?: "")
        val fileTools = fileToolsManager.getFileTools(checkPermission)
        val flags = mutableListOf<Boolean>()
        backups.forEachIndexed { index, backup ->
            if (checkPermission == PathType.DOCUMENT && backup != null) {
                flags.add(
                    fileTools?.copyFileByFD(
                        backup.backupPath!!, backup.gameFilePath!!
                    ) == true
                )
            } else if (backup != null) {
                flags.add(
                    fileTools?.copyFile(
                        backup.backupPath!!, backup.gameFilePath!!
                    ) == true
                )
            }
            this@DisableModsUserCase.setUnzipProgress?.let { it("${index}/${backups.size}") }

        }
        if (!flags.all { it }) {
            throw IOException(App.get().getString(R.string.toast_restore_failed))
        }
    }



    private suspend fun specialOperationDisable(
        backupBeans: List<BackupBean>,
        packageName: String,
        modBean: ModBean
    ) {
        var specialOperationFlag = true
         specialGameToolsManager.getSpecialGameTools(packageName)?.let {
            specialOperationFlag = it.specialOperationDisable(backupBeans, packageName, modBean)
        }

        if (!specialOperationFlag) {
            throw Exception(App.get().getString(R.string.toast_special_operation_failed))
        }
    }
}