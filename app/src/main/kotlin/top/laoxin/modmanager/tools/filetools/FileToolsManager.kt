package top.laoxin.modmanager.tools.filetools

import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.di.FileToolsModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileToolsManager @Inject constructor(
    @param:FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools,
    @param:FileToolsModule.ShizukuFileToolsImpl private val shizukuFileTools: BaseFileTools,
    @param:FileToolsModule.DocumentFileToolsImpl private val documentFileTools: BaseFileTools
) {
    // 通过不同的权限获取方式，获取文件工具
    fun getFileTools(pathType : Int): BaseFileTools? {
        return when (pathType) {
            PathType.FILE -> fileTools
            PathType.DOCUMENT -> documentFileTools
            PathType.SHIZUKU -> shizukuFileTools
            else -> null
        }
    }
    // 获取fileTools
    fun getFileTools(): BaseFileTools {
        return fileTools
    }

    // 获取shizukuFileTools
    fun getShizukuFileTools(): BaseFileTools {
        return shizukuFileTools
    }

    // 获取documentFileTools
    fun getDocumentFileTools(): BaseFileTools {
        return documentFileTools
    }

}