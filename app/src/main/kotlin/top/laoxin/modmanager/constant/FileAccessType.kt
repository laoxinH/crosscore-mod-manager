package top.laoxin.modmanager.constant

/** 文件访问类型枚举 用于标识当前路径应使用的文件访问方式 */
enum class FileAccessType {
    /** 标准 File API - 适用于应用私有目录和 Download 目录 */
    STANDARD_FILE,

    /** DocumentFile API (SAF) - 适用于 Android 11-13 的受限目录 */
    DOCUMENT_FILE,

    /** Shizuku API - 适用于获得 Shizuku 授权的情况 */
    SHIZUKU,

    /** 无权限访问 */
    NONE
}
