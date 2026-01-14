package top.laoxin.modmanager.domain.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanFiles")
data class ScanFileBean(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val name: String,
    val modifyTime: Long,
    val size: Long,
    val isDirectory: Boolean,
    val md5: String = "",              // 文件 MD5 校验值
    val gamePackageName: String = ""   // 关联游戏包名
)