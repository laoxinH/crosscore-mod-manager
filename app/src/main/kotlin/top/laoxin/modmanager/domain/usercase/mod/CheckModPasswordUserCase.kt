package top.laoxin.modmanager.domain.usercase.mod


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.exception.PasswordErrorException
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.MD5Tools
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.tools.ToastUtils
import top.laoxin.modmanager.tools.filetools.FileToolsManager
import top.laoxin.modmanager.tools.manager.AppPathsManager
import top.laoxin.modmanager.tools.manager.GameInfoManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class CheckModPasswordUserCase @Inject constructor(
    private val gameInfoManager: GameInfoManager,
    private val appPathsManager: AppPathsManager,
    private val modRepository: ModRepository,
    private val permissionTools: PermissionTools,
    private val fileToolsManager: FileToolsManager,
    private val readModReadmeFileUserCase: ReadModReadmeFileUserCase,

    ) {

    companion object {
        const val TAG = "FlashModsUserCase"
    }

    suspend operator fun invoke(
        modBean: ModBean,
        password: String,
        setTipsText: (String) -> Unit
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
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

            // 立即检查解压结果，密码错误时立即返回错误并显示Toast通知
            if (!decompression) {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_password_error)
                }
                LogTools.logRecord("密码验证失败:${modBean.name}--密码错误")
                return@withContext Pair(
                    ResultCode.FAIL,
                    App.get().getString(R.string.toast_password_error)
                )
            }

            setTipsText(App.get().getString(R.string.tips_reload_decrypted_mod_info))

            // 获取需要更新的模组列表
            val mods = modRepository.getModsByPathAndGamePackageName(
                modBean.path, modBean.gamePackageName!!
            ).firstOrNull() ?: emptyList()

            val newModList: MutableList<ModBean> = mutableListOf()

            for (mod in mods) {
                val newMod = mod.copy(password = password)
                val updatedMod: ModBean = readModInfo(unZipPath, newMod)
                newModList.add(updatedMod)
            }

            if (newModList.isNotEmpty()) {
                modRepository.updateAll(newModList)
            }

            // 返回成功结果
            return@withContext Pair(ResultCode.SUCCESS, "")
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (e.message?.contains("StandaloneCoroutine was cancelled") != true) {
                    if (e is PasswordErrorException || e.message?.contains("top.laoxin.modmanager") == true) {
                        ToastUtils.longCall(R.string.toast_password_error)
                    } else {
                        ToastUtils.longCall(e.message.toString())
                    }
                }

                LogTools.logRecord("校验密码失败:${modBean.name}--$e")
                e.printStackTrace()
            }

            return@withContext Pair(ResultCode.FAIL, e.message ?: "")
        }
        return@withContext Pair(ResultCode.FAIL, "")
    }

    // 读取mod信息
    fun readModInfo(unZipPath: String, modBean: ModBean): ModBean {
        var bean = modBean.copy(description = "MOD已解密")
        // 读取readme
        bean = readModReadmeFileUserCase(unZipPath, bean)
        // 读取icon文件输入流
        val fileTools = fileToolsManager.getFileTools(permissionTools.checkPermission(unZipPath))
            ?: fileToolsManager.getFileTools()
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
            for (image in bean.images) {
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
