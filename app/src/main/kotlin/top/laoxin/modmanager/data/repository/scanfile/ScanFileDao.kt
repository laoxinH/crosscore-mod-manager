package top.laoxin.modmanager.data.repository.scanfile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.bean.ScanFileBean


@Dao
interface ScanFileDao {
    // 插入一条数据`
    @Insert(onConflict = OnConflictStrategy.REPLACE)  // 如果插入的数据已经存在，则替换
    suspend fun insert(scanFile: ScanFileBean)

    // 获取所有数据
    @androidx.room.Query("SELECT * from scanFiles")
    fun getAll(): Flow<List<ScanFileBean>>

    // 通过path查询
    @androidx.room.Query("SELECT * from scanFiles WHERE path = :path")
    fun getByPath(path: String): Flow<List<ScanFileBean>>

    // 插入多条数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scanFiles: List<ScanFileBean>)

    // 删除所有数据
    @androidx.room.Query("DELETE FROM scanFiles")
    fun deleteAll()

    // 删除一条数据
    @androidx.room.Delete
    fun delete(it: ScanFileBean)

    // 更新一条数据
    @androidx.room.Update
    fun update(scanFile: ScanFileBean)

}