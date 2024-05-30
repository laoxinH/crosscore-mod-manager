package top.laoxin.modmanager.bean

import androidx.room.Entity
import androidx.room.PrimaryKey


import android.os.Parcel
import android.os.Parcelable


/**
 * 备份实体类
 */
@Entity(tableName = "backups")
data class BackupBean(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val gamePath: String?,
    val backupPath: String?,
    val modPath : String?,
)