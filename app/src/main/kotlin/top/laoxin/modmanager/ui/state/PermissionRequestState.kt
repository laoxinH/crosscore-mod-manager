package top.laoxin.modmanager.ui.state

/** 权限请求状态 用于管理权限请求对话框的显示和请求类型 */
data class PermissionRequestState(
        /** 是否显示权限请求对话框 */
        val showDialog: Boolean = false,
        /** 需要请求权限的路径 */
        val requestPath: String = "",
        /** 权限类型 */
        val permissionType: PermissionType = PermissionType.NONE
)

/** 权限类型枚举 */
enum class PermissionType {
    /** 无需权限 */
    NONE,
    /** 全局存储权限 (Android 11+ MANAGE_EXTERNAL_STORAGE) */
    STORAGE,
    /** SAF 目录权限 */
    URI_SAF,
    /** Shizuku 权限 */
    SHIZUKU,
    /** 通知权限 */
    NOTIFICATION
}
