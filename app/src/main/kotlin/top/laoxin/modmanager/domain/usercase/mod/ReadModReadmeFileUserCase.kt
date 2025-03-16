package top.laoxin.modmanager.domain.usercase.mod

import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.tools.LogTools.logRecord
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadModReadmeFileUserCase @Inject constructor() {
    // 读取readme文件
    operator fun invoke(unZipPath: String, modBean: ModBean): ModBean {
        try {
            var readmeFile: File? = null
            if (modBean.isZipFile && modBean.readmePath != null) {
                val path = unZipPath + modBean.readmePath
                readmeFile = File(path)
            }

            if (modBean.isZipFile && modBean.fileReadmePath != null) {
                val path = unZipPath + modBean.fileReadmePath
                readmeFile = File(path)
            }

            if (!modBean.isZipFile && modBean.readmePath != null) {
                readmeFile = File(modBean.readmePath!!)
            }

            if (!modBean.isZipFile && modBean.fileReadmePath != null) {
                readmeFile = File(modBean.fileReadmePath!!)
            }
            readmeFile?.let {
                return modBean.copy(
                    description = readmeFile.readText()
                )
            }
        } catch (_: Exception) {
            logRecord("ReadModReadmeFileUserCase: 读取readme文件失败")
        }

        return modBean
    }
}