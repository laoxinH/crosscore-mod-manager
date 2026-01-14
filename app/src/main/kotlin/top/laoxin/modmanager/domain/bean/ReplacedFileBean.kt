package top.laoxin.modmanager.domain.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 被替换文件记录实体 用于记录 MOD 管理器替换过的游戏文件，便于检测游戏更新是否覆盖 */
@Entity(tableName = "replaced_files")
data class ReplacedFileBean(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        /** 关联的 MOD ID */
        val modId: Int,
        /** 文件名 */
        val filename: String,
        /** 游戏文件路径 */
        val gameFilePath: String,
        /** 替换后文件的 MD5 */
        val md5: String,
        /** 游戏包名 */
        val gamePackageName: String,
        /** 替换时间 */
        val replaceTime: Long
)
