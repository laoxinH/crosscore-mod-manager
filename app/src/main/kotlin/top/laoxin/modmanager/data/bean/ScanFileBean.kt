package top.laoxin.modmanager.data.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanFiles")
data class ScanFileBean(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val name: String,
    val modifyTime: Long,
    val size: Long
)