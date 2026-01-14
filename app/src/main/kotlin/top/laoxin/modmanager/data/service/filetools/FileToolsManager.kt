package top.laoxin.modmanager.data.service.filetools

import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.data.service.filetools.impl.DocumentFileTools
import top.laoxin.modmanager.data.service.filetools.impl.FileTools
import top.laoxin.modmanager.data.service.filetools.impl.ShizukuFileTools

import top.laoxin.modmanager.domain.service.PermissionService

@Singleton
class FileToolsManager
@Inject
constructor(
    private val fileTools: FileTools,
    private val shizukuFileTools: ShizukuFileTools,
    private val documentFileTools: DocumentFileTools,
    private val permissionService: PermissionService
) {
    /** 通过文件访问类型获取对应的文件工具 */
    fun getFileToolsBySinglePath(path: String): BaseFileTools? {

        val accessType = permissionService.getFileAccessType(path)
        return when (accessType) {
            FileAccessType.STANDARD_FILE -> fileTools
            FileAccessType.DOCUMENT_FILE -> documentFileTools
            FileAccessType.SHIZUKU -> shizukuFileTools
            FileAccessType.NONE -> null
        }
    }
    /**
     * 通过源文件路径和目标文件路径获取对应的文件工具 权限优先级: SHIZUKU > DOCUMENT_FILE > STANDARD_FILE
     * - 如果任一路径需要 SHIZUKU 权限，则使用 ShizukuFileTools
     * - 如果两者权限相同，返回对应工具
     * - 如果权限不同，使用更高优先级的工具
     * @param srcPath 源文件路径
     * @param destPath 目标文件路径
     * @return 文件工具
     */
    fun getFileToolsByPaths(srcPath: String, destPath: String): BaseFileTools? {
        val srcAccessType = permissionService.getFileAccessType(srcPath)
        val destAccessType = permissionService.getFileAccessType(destPath)

        // 如果任一为 NONE，无法操作
        if (srcAccessType == FileAccessType.NONE || destAccessType == FileAccessType.NONE) {
            return null
        }

        // 如果任一需要 SHIZUKU，使用 ShizukuFileTools（最高优先级）
        if (srcAccessType == FileAccessType.SHIZUKU || destAccessType == FileAccessType.SHIZUKU) {
            return shizukuFileTools
        }

        // 如果任一需要 DOCUMENT_FILE，使用 DocumentFileTools
        if (srcAccessType == FileAccessType.DOCUMENT_FILE ||
                        destAccessType == FileAccessType.DOCUMENT_FILE
        ) {
            return documentFileTools
        }

        // 都是 STANDARD_FILE
        return fileTools
    }
    /*
    获取文件访问类型 */
    fun getFileAccessType(path: String): FileAccessType {
        return permissionService.getFileAccessType(path)
    }

    /** 获取标准 FileTools */
    fun getStandardFileTools(): BaseFileTools = fileTools

    /** 获取 ShizukuFileTools */
    fun getShizukuFileTools(): BaseFileTools = shizukuFileTools

    /** 获取 DocumentFileTools */
    fun getDocumentFileTools(): BaseFileTools = documentFileTools
}
