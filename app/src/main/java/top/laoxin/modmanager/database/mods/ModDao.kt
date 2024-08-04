package top.laoxin.modmanager.database.mods

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

    // 查询所有数据
    @Query("SELECT * from mods ORDER BY date DESC")
    fun getAll(): Flow<List<ModBean>>

    // 通过gamePackageName和名字模糊查询
    @Query("SELECT * from mods WHERE gamePackageName = :gamePackageName AND name LIKE '%' || :name || '%'")
    fun getModsByGamePackageNameAndName(gamePackageName: String, name: String): Flow<List<ModBean>>

    // 通过List<ModBean>插入数据
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(mods: List<ModBean>)


    //通过gamePackageName 查询关闭的mod
    @Query("SELECT * from mods WHERE gamePackageName = :gamePackageName AND isEnable = 0 ORDER BY id DESC")
    fun getDisableModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>


    // 通过list批量删除mods
    @Delete
    suspend fun deleteMods(mods: List<ModBean>)


    // 通过List<modbean>更新
    @Update
    suspend fun updateMods(mods: List<ModBean>)

    // 通过gamePackageName查询mods
    @Query("SELECT * from mods WHERE gamePackageName = :gamePackageName ORDER BY id DESC")
    fun getModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>

    // 通过gamePackageName查询mods数量
    @Query("SELECT COUNT(*) FROM mods WHERE gamePackageName = :gamePackageName")
    fun getModsCountByGamePackageName(gamePackageName: String): Flow<Int>

    // 通过gamePackageName查询已开启的mods
    @Query("SELECT * from mods WHERE gamePackageName = :gamePackageName AND isEnable = 1 ORDER BY id DESC")
    fun getEnableModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>

    // 通过path和gamePackageName查询mods
    @Query("SELECT * from mods WHERE path = :path AND gamePackageName = :gamePackageName ORDER BY id DESC")
    fun getModsByPathAndGamePackageName(path: String, gamePackageName: String): Flow<List<ModBean>>


    //通过包名查询已开启的mods数量
    @Query("SELECT COUNT(*) FROM mods WHERE gamePackageName = :gamePackageName AND isEnable = 1")
    fun getModsEnableCountByGamePackageName(gamePackageName: String): Flow<Int>

    // 用过ids查询mods
    @Query("SELECT * from mods WHERE id IN (:ids)")
    fun getModsByIds(ids: List<Int>): Flow<List<ModBean>>

    // 通过modpath查询mod数量`
    @Query("SELECT COUNT(*) FROM mods WHERE path = :path")
    fun getModsCountByPath(path: String): Flow<Int>
}