package top.laoxin.modmanager.bean

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "antiHarmony", indices = [Index(value = ["gamePackageName"])])
data class AntiHarmonyBean(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gamePackageName: String,
    val isEnable: Boolean,
)
