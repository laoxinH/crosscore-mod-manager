package top.laoxin.modmanager.domain.usercase.mod


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.backup.BackupRepository
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.data.repository.scanfile.ScanFileRepository
import top.laoxin.modmanager.domain.usercase.app.CheckPermissionUserCase
import top.laoxin.modmanager.exception.PasswordErrorException
import top.laoxin.modmanager.observer.FlashModsObserverManager
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.MD5Tools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import top.laoxin.modmanager.tools.specialGameTools.SpecialGameToolsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class CheckModPasswordUserCase @Inject constructor(
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
    private val readModReadmeFileUserCase: ReadModReadmeFileUserCase,

    ) {

    companion object {
        const val TAG = "FlashModsUserCase"
    }

    private var setUnzipProgress: ((String) -> Unit)? = null

    suspend operator fun invoke(
        modBean: ModBean,
        password :String,
        setTipsText : (String) -> Unit
    ): Pair<Int,String> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            setTipsText(
                App.get().getString(R.string.tips_check_password)
            )
            val gameInfo = gameInfoManager.getGameInfo()
            setTipsText(
                "${App.get().getString(R.string.tips_unzip_mod)} ${modBean.name}"
            )
            val unZipPath =
                appPathsManager.getModsUnzipPath() + gameInfo.packageName + "/" + File(modBean.path!!).nameWithoutExtension + "/"
            val decompression = ArchiveUtil.decompression(
                modBean.path, unZipPath, password, true
            )
            if (!decompression) throw PasswordErrorException(
                App.get().getString(R.string.toast_password_error)
            )
            modRepository.getModsByPathAndGamePackageName(
                modBean.path, modBean.gamePackageName!!
            ).collect { mods ->
                Log.d("ModViewModel", "更新mod: $mods")
                setTipsText(App.get().getString(R.string.tips_special_operation))
                val newModList: MutableList<ModBean> = mutableListOf()
                mods.forEach {
                    val newMod = it.copy(password = password)
                    val mod: ModBean = readModInfo(unZipPath, newMod)
                    newModList.add(mod)
                }
                modRepository.updateAll(newModList)

            }
        }.onSuccess {
            return@withContext Pair(ResultCode.SUCCESS, "")
        }.onFailure {
            withContext(Dispatchers.Main) {
                if (it.message?.contains("StandaloneCoroutine was cancelled") != true) {
                    if (it.message?.contains("top.laoxin.modmanager") == true) {
                        ToastUtils.longCall(R.string.toast_password_error)
                    } else {
                        ToastUtils.longCall(it.message.toString())
                    }
                }
                LogTools.logRecord("校验密码失败:${modBean.name}--$it")
                it.printStackTrace()
            }
            return@withContext Pair(ResultCode.FAIL, "")

        }
        return@withContext Pair(ResultCode.FAIL, "")
    }

    // 读取mod信息
    suspend fun readModInfo(unZipPath: String, modBean: ModBean): ModBean {
        var bean = modBean.copy(description = "MOD已解密")
        // 读取readme
        bean = readModReadmeFileUserCase(unZipPath, bean)
        // 读取icon文件输入流
        val fileTools = fileToolsManager.getFileTools()
        if (bean.icon != null) {
            val iconPath = unZipPath + bean.icon
            val file = File(iconPath)
            val md5 = MD5Tools.calculateMD5(file.inputStream())
            fileTools.copyFile(iconPath, appPathsManager.getModsIconPath() + md5 + file.name)
            bean = bean.copy(icon = appPathsManager.getModsIconPath() + md5 + file.name)
        }
        // 读取images文件输入流
        if (bean.images != null) {
            val images = mutableListOf<String>()
            for (image in bean.images!!) {
                val imagePath = unZipPath + image
                val file = File(imagePath)
                val md5 = MD5Tools.calculateMD5(file.inputStream())
                fileTools.copyFile(imagePath, appPathsManager.getModsImagePath() + md5 + file.name)
                images.add(appPathsManager.getModsImagePath() + md5 + file.name)
            }
            bean = bean.copy(images = images)
        }
        return bean
    }
}