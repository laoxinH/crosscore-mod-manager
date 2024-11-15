package top.laoxin.modmanager.bean

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * 备份实体类
 */
@Entity(tableName = "backups")
data class BackupBean(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filename: String?,
    val gamePath: String?,
    val gameFilePath: String?,
    val backupPath: String?,
    val gamePackageName: String?,
    val modName: String?,
)