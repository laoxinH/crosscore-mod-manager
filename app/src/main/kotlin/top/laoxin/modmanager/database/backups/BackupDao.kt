package top.laoxin.modmanager.database.backups

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

    @androidx.room.Query("SELECT * from backups WHERE modName = :modPath")
    fun getByModPath(modPath: String): Flow<List<BackupBean>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(backups: List<BackupBean>)

    @androidx.room.Query("DELETE FROM backups")
    fun deleteAll()


    //通过gamePackageName删除
    @androidx.room.Query("DELETE FROM backups WHERE gamePackageName = :gamePackageName")
    fun deleteByGamePackageName(gamePackageName: String)


    // 通过modName和gamePackageName查询backups
    @androidx.room.Query("SELECT * from backups WHERE modName = :modName AND gamePackageName = :gamePackageName")
    fun getByModNameAndGamePackageName(
        modName: String,
        gamePackageName: String
    ): Flow<List<BackupBean>>


}