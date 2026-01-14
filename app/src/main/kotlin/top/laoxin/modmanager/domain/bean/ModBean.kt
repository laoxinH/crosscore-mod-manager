package top.laoxin.modmanager.domain.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

/** mod实体类 */
@Entity(tableName = "mods")
data class ModBean(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
        // 非空字段
    val name: String = "",
    val description: String = "",
    val date: Long = 0L,
    val path: String = "",
    val modFiles: List<String> = emptyList(),
    val gameFilesPath: List<String> = emptyList(),
    val modForm: ModForm = ModForm.TRADITIONAL,
    val isEncrypted: Boolean = false,
    val password: String = "",
    val gamePackageName: String = "",
    val gameModPath: String = "",
    val modType: String = "",
    val isEnable: Boolean = false,
    val isZipFile: Boolean = true,
        // 可空字段
    val version: String? = null,
    val virtualPaths: String? = null,
    val icon: String? = null,
    val images: List<String>? = null,
    val readmePath: String? = null,
    val fileReadmePath: String? = null,
    val author: String? = null,
    val modConfig: String? = null,
    /** 压缩包内的相对路径，用于整合包中定位具体 MOD 位置 */
        val modRelativePath: String? = null,
    /** 最后更新时间，用于触发 UI 重组和图片缓存刷新 */
        val updateAt: Long = System.currentTimeMillis(),
)
