package top.laoxin.modmanager.domain.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 备份实体类 */
@Entity(tableName = "backups")
data class BackupBean(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        /** 关联的 Mod ID */
        val modId: Int,
        /** 备份的文件名 */
        val filename: String,
        /** 游戏安装路径 */
        val gamePath: String,
        /** 游戏文件路径 */
        val gameFilePath: String,
        /** 备份文件存储路径 */
        val backupPath: String,
        /** 游戏包名 */
        val gamePackageName: String,
        /** 备份创建时间 */
        val backupTime: Long,
        /** 备份文件复制完成时间 */
        val copyTime: Long,
        /** 原始游戏文件 MD5（备份前） */
        val originalMd5: String = "",
        /** 替换用的 MOD 文件 MD5 */
        val modFileMd5: String = ""
)
