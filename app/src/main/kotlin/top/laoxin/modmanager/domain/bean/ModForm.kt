package top.laoxin.modmanager.domain.bean

/** Mod形式枚举，限定只能为以下三种形式 */
enum class ModForm {
    /** 传统形式 - 被动扫描 */
    TRADITIONAL,

    /** 新形式 - 包含mod配置文件的主动式mod包 */
    ACTIVE,

    /** 打包式 - 包含配置文件的需要打包unity资源包的形式 */
    PACKAGED
}
