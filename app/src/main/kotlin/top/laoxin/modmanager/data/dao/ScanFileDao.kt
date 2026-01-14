package top.laoxin.modmanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.ScanFileBean

@Dao
interface ScanFileDao {
    // 插入一条数据
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(scanFile: ScanFileBean)

    // 获取所有数据
    @Query("SELECT * from scanFiles")
    fun getAll(): Flow<List<ScanFileBean>>

    // 通过path查询（Flow）
    @Query("SELECT * from scanFiles WHERE path = :path")
    fun getByPath(path: String): Flow<List<ScanFileBean>>

    // 通过path查询单条记录（suspend，用于快速检查）
    @Query("SELECT * from scanFiles WHERE path = :path LIMIT 1")
    suspend fun getByPathSync(path: String): ScanFileBean?

    // 通过gamePackageName查询
    @Query("SELECT * from scanFiles WHERE gamePackageName = :gamePackageName")
    fun getByGamePackageName(gamePackageName: String): Flow<List<ScanFileBean>>

    // 通过gamePackageName查询（suspend）
    @Query("SELECT * from scanFiles WHERE gamePackageName = :gamePackageName")
    suspend fun getByGamePackageNameSync(gamePackageName: String): List<ScanFileBean>

    // 插入多条数据
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(scanFiles: List<ScanFileBean>)

    // 删除所有数据
    @Query("DELETE FROM scanFiles")
    suspend fun deleteAll()

    // 删除一条数据
    @Delete
    suspend fun delete(it: ScanFileBean)

    // 通过path删除
    @Query("DELETE FROM scanFiles WHERE path = :path")
    suspend fun deleteByPath(path: String)

    // 通过gamePackageName删除
    @Query("DELETE FROM scanFiles WHERE gamePackageName = :gamePackageName")
    suspend fun deleteByGamePackageName(gamePackageName: String)

    // 更新一条数据
    @Update
    suspend fun update(scanFile: ScanFileBean)
}