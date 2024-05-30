package top.laoxin.modmanager.data.backups

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.BackupBean


@Dao
interface BackupDao {

    // 插入数据
    @Insert(onConflict = OnConflictStrategy.IGNORE)  // 如果插入的数据已经存在，则忽略
    suspend fun insert(backupBean: BackupBean)

    // 获取所有数据
    @androidx.room.Query("SELECT * from backups")
    fun getAll(): List<BackupBean>

    // 通过name查询数据
    @androidx.room.Query("SELECT * from backups WHERE name = :name")
    fun getByName(name: String): BackupBean?

    // 通过modPath查询
    @androidx.room.Query("SELECT * from backups WHERE modPath = :modPath")
    fun getByModPath(modPath: String): Flow<List<BackupBean?>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(backups: List<BackupBean>)

    @androidx.room.Query("DELETE FROM backups")
    fun deleteAll()
}