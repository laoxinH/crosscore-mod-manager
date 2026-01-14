package top.laoxin.modmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.BackupBean

@Dao
interface BackupDao {

    // 插入数据
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(backupBean: BackupBean)

    // 获取所有数据
    @Query("SELECT * from backups")
    fun getAll(): List<BackupBean>

    @Query("SELECT * from backups WHERE modId = :modId")
    fun getByModId(modId: Int): Flow<List<BackupBean>>

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(backups: List<BackupBean>)

    @Query("DELETE FROM backups")
    fun deleteAll()

    // 通过gamePackageName删除
    @Query("DELETE FROM backups WHERE gamePackageName = :gamePackageName")
    fun deleteByGamePackageName(gamePackageName: String)

    // 通过modId和gamePackageName查询backups
    @Query("SELECT * from backups WHERE modId = :modId AND gamePackageName = :gamePackageName")
    fun getByModIdAndGamePackageName(
        modId: Int,
        gamePackageName: String
    ): Flow<List<BackupBean>>

    // 通过modId查询backups（suspend，同步获取）
    @Query("SELECT * from backups WHERE modId = :modId")
    suspend fun getByModIdSync(modId: Int): List<BackupBean>

    // 通过modId删除backups
    @Query("DELETE FROM backups WHERE modId = :modId")
    suspend fun deleteByModId(modId: Int)

    // ==================== MD5 校验相关 ====================

    // 通过 gameFilePath 查询备份（检查是否已有共享备份）
    @Query("SELECT * FROM backups WHERE gameFilePath = :gameFilePath LIMIT 1")
    suspend fun getByGameFilePath(gameFilePath: String): BackupBean?

    // 通过 modId 和 gameFilePath 查询（检查 MOD 开关重复）
    @Query("SELECT * FROM backups WHERE modId = :modId AND gameFilePath = :gameFilePath")
    suspend fun getByModIdAndGameFilePath(modId: Int, gameFilePath: String): BackupBean?

    // 更新备份记录
    @Update
    suspend fun update(backup: BackupBean)

    // 通过 gameFilePath 更新所有 originalMd5（游戏更新后批量更新）
    @Query("UPDATE backups SET originalMd5 = :md5, backupTime = :backupTime WHERE gameFilePath = :gameFilePath")
    suspend fun updateOriginalMd5ByGameFilePath(gameFilePath: String, md5: String, backupTime: Long)
}
