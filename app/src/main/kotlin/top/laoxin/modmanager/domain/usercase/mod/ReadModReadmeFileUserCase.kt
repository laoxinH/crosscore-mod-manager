package top.laoxin.modmanager.domain.usercase.mod

import top.laoxin.modmanager.data.bean.ModBean
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadModReadmeFileUserCase @Inject constructor(

) {
    // 读取readme文件
    operator suspend fun invoke(unZipPath: String, modBean: ModBean): ModBean {
        // 判断是否存在readme文件
        val infoMap = mutableMapOf<String, String>()
        if (modBean.readmePath != null) {
            val readmeFile = File(unZipPath + modBean.readmePath)
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
        } else if (modBean.fileReadmePath != null) {
            val readmeFile = File(unZipPath + modBean.fileReadmePath)
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
        }
        return if (infoMap.isNotEmpty()) {
            modBean.copy(
                name = infoMap["名称"],
                description = infoMap["描述"],
                version = infoMap["版本"],
                author = infoMap["作者"]
            )
        } else {
            modBean
        }
    }
}