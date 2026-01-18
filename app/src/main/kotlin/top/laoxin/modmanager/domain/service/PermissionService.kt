package top.laoxin.modmanager.domain.service

import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.domain.model.Result

/** 权限服务接口 封装 Shizuku、SAF、标准文件权限的检查和请求逻辑 */
interface PermissionService {

    // ==================== Shizuku 相关 ====================

    /** 注册 Shizuku 权限请求监听器 应在 Activity.onCreate 中调用 */
    fun registerShizukuListener(): Result<Unit>

    /** 解除 Shizuku 权限请求监听器 应在 Activity.onDestroy 中调用 */
    fun unregisterShizukuListener(): Result<Unit>

    /**
     * 检查 Shizuku 权限并绑定服务
     * @return Result<Boolean> 是否成功绑定
     */
    fun checkAndBindShizuku(): Result<Boolean>

    /** Shizuku 是否可用（已安装且正在运行） */
    fun isShizukuAvailable(): Boolean

    /** 是否已获得 Shizuku 权限 */
    fun hasShizukuPermission(): Boolean

    /**
     * 请求 Shizuku 权限 (挂起函数)
     * @return Result<Boolean> 权限授予结果 - true: 已授权, false: 被拒绝
     */
    suspend fun requestShizukuPermission(): Result<Boolean>

    // ==================== URI/SAF 权限 ====================

    /**
     * 检查是否有 URI 权限
     * @param path 文件路径
     * @return Result<Boolean> 是否有权限
     */
    fun hasUriPermission(path: String): Result<Boolean>

    /**
     * 检查是否有全局存储权限 (Android 11+ MANAGE_EXTERNAL_STORAGE)
     * @return Boolean 是否有权限
     */
    fun hasStoragePermission(): Boolean

    // ==================== 文件访问类型判断 ====================

    /**
     * 获取指定路径的文件访问类型
     * @param path 文件路径
     * @param osVersion 操作系统版本
     * @return FileAccessType 文件访问类型
     */
    fun getFileAccessType(path: String): FileAccessType

    /**
     * 获取需要请求权限的路径
     * @param path 原始路径
     * @param osVersion 操作系统版本
     * @return 需要请求权限的路径
     */
    fun getRequestPermissionPath(path: String): String

    // ==================== 路径辅助函数 ====================

    /** 判断路径是否在应用数据目录下 */
    fun isUnderAppDataPath(path: String): Boolean

    /** 获取应用数据目录路径 */
    fun getAppDataPath(path: String): String

    /** 判断是否是本应用的数据目录 */
    fun isFromMyPackageNamePath(path: String): Boolean

    /** 获取根路径 */
    fun getRootPath(): String

    /** 检查路径权限 */
    fun checkPathPermissions(gamePath: String): Result<Unit>
}
