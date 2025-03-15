package top.laoxin.modmanager.domain.usercase.mod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.constant.ResultCode
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.LogTools
import top.laoxin.modmanager.tools.manager.AppPathsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


data class FlashModDetailResult(
    val code: Int,
    val mod: ModBean,
    )

@Singleton
class FlashModDetailUserCase @Inject constructor(
    private val appPathsManager: AppPathsManager,
    private val modRepository: ModRepository,
    private val readModReadmeFileUserCase: ReadModReadmeFileUserCase,

) {
    suspend operator fun invoke(modBean: ModBean): FlashModDetailResult {
        val newMod : ModBean
        // 有 README 文件
        if (modBean.readmePath != null || modBean.fileReadmePath != null) {
            newMod = flashModDetail(modBean).mod
            LogTools.logRecord("old:$modBean")
            LogTools.logRecord("new:$newMod")
            // 更新数据库
            modRepository.updateMod(newMod)

            LogTools.logRecord("use:" + modRepository.getModById(modBean.id).toString())
        } else {
            // 无 README 文件
            newMod = modBean.copy(
                name = modBean.path?.substringAfterLast("/")?.substringBeforeLast(".")
                        + "(${
                    modBean.virtualPaths?.removePrefix(modBean.path ?: "")?.trimStart('/')
                })".replace("/", "|"),
                description = App.get().getString(R.string.mod_bean_no_readme),
                author = App.get().getString(R.string.mod_bean_no_author),
                version = "1.0"
            )


            // 更新数据库
            modRepository.updateMod(newMod)

            LogTools.logRecord("use:" + modRepository.getModById(modBean.id).toString())
        }
        return FlashModDetailResult(ResultCode.SUCCESS, newMod)
    }

    // 读取readme文件
    suspend fun flashModDetail(modBean: ModBean): FlashModDetailResult =
        withContext(Dispatchers.IO) {
            val infoMap = mutableMapOf<String, String>()
            var path = ""

            // 判断是否需要密码
            if (modBean.isEncrypted && modBean.password == null) {
                return@withContext FlashModDetailResult(ResultCode.MOD_NEED_PASSWORD, modBean)
            }

            // 判断是否是压缩包
            if (modBean.isZipFile) {
                try {
                    // 解压操作
                    if (!modBean.fileReadmePath.isNullOrEmpty()) {
                        ArchiveUtil.extractSpecificFile(
                            modBean.path!!,
                            listOf(modBean.fileReadmePath!!),
                            appPathsManager.getModsUnzipPath() + File(modBean.path).nameWithoutExtension,
                            modBean.password,
                            false
                        )
                    } else if (!modBean.readmePath.isNullOrEmpty()) {
                        ArchiveUtil.extractSpecificFile(
                            modBean.path!!,
                            listOf(modBean.readmePath!!),
                            appPathsManager.getModsUnzipPath() + File(modBean.path).nameWithoutExtension,
                            modBean.password,
                            false
                        )
                    }
                } catch (e: Exception) {
                    LogTools.logRecord("解压或读取文件失败: ${e.message}")
                }
            }

            // 设置文件路径
            path = getModReadmePath(modBean)

            // 读取 README 文件
/*            val readmeFile = File(path)
            if (readmeFile.exists()) {
                val reader = readmeFile.bufferedReader()
                val lines = reader.readLines()
                for (line in lines) {
                    val parts = line.split("：")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        infoMap[key] = value
                    }
                }
            }
            LogTools.logRecord("info:$infoMap")*/

            // 返回更新后的 modBean

            return@withContext FlashModDetailResult(
                ResultCode.SUCCESS, readModReadmeFileUserCase(appPathsManager.getModsUnzipPath(), modBean)
            )
        }

    // 获取 README 路径的逻辑
    private fun getModReadmePath(modBean: ModBean): String {
        return if (modBean.isZipFile) {
            if (modBean.readmePath != null) {
                appPathsManager.getModsUnzipPath() + File(modBean.path!!).nameWithoutExtension + '/' + modBean.readmePath!!
            } else if (modBean.fileReadmePath != null) {
                appPathsManager.getModsUnzipPath() + File(modBean.path!!).nameWithoutExtension + '/' + modBean.fileReadmePath!!
            } else {
                ""
            }
        } else {
            modBean.readmePath ?: modBean.fileReadmePath ?: ""
        }
    }
}