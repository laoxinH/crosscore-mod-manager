package top.laoxin.modmanager.tools.filetools

import android.os.Environment
import top.laoxin.modmanager.App
import top.laoxin.modmanager.constant.FileType
import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.tools.filetools.impl.ShizukuFileTools
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileToolsManager @Inject constructor(
    @FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools,
    @FileToolsModule.ShizukuFileToolsImpl private val shizukuFileTools: BaseFileTools,
    @FileToolsModule.DocumentFileToolsImpl private val documentFileTools: BaseFileTools
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