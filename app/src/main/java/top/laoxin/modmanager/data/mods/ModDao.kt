package top.laoxin.modmanager.data.mods

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.ModBean

@Dao
interface ModDao {

    // 插入数据
    @Insert(onConflict = OnConflictStrategy.IGNORE)  // 如果插入的数据已经存在，则忽略
    suspend fun insert(modBean: ModBean)

    // 更新数据
    @Update
    suspend fun update(modBean: ModBean)

    // 删除数据
    @Delete
    suspend fun delete(modBean: ModBean)

    // 通过id查询数据
    @Query("SELECT * from mods WHERE id = :id")
    fun getItem(id: Int): Flow<ModBean?>

    // 查询所有数据
    @Query("SELECT * from mods ORDER BY date DESC")
    fun getAll(): Flow<List<ModBean>>

    // 通过名字模糊查询
    @Query("SELECT * from mods WHERE name LIKE '%' || :name || '%' ORDER BY id DESC")
    fun search(name: String): Flow<List<ModBean>>

    // 查询启用的mod
    @Query("SELECT * from mods WHERE isEnable = 1")
    fun getEnableMods(): Flow<List<ModBean>>

    // 查询所有mod并通过启用状态排序
    @Query("SELECT * from mods ORDER BY isEnable DESC")
    fun getAllOrderByEnable(): Flow<List<ModBean>>

    // 通过List<ModBean>插入数据
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(mods: List<ModBean>)


    // 查询mod数量
    @Query("SELECT COUNT(*) FROM mods")
    fun getModCount(): Flow<Int>

    // 查询已开启mod数量
    @Query("SELECT COUNT(*) FROM mods WHERE isEnable = 1 ORDER BY id DESC")
    fun getEnableModCount(): Flow<Int>

    // 查询关闭的mod
    @Query("SELECT * from mods WHERE isEnable = 0 ORDER BY id DESC")
    fun getDisableMods(): Flow<List<ModBean>>


    // 通过list批量删除mods
    @Delete
    suspend fun deleteMods(mods: List<ModBean>)

    // 通过path查询mod
    @Query("SELECT * from mods WHERE path = :path")
    fun getModByPath(path: String): Flow<List<ModBean>>

    // 通过List<modbean>更新
    @Update
    suspend fun updateMods(mods: List<ModBean>)

}