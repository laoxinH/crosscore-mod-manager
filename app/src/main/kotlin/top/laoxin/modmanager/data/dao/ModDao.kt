package top.laoxin.modmanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.ModBean

@Dao
interface ModDao {

    // ==================== 查询操作 ====================

    /** 获取所有 MOD */
    @Query("SELECT * FROM mods") fun getAll(): Flow<List<ModBean>>

    /** 通过 ID 查询 MOD */
    @Query("SELECT * FROM mods WHERE id = :id") suspend fun getModById(id: Int): ModBean?

    /** 通过 IDs 列表查询 MODs */
    @Query("SELECT * FROM mods WHERE id IN (:ids)")
    fun getModsByIds(ids: List<Int>): Flow<List<ModBean>>

    /** 通过 gamePackageName 查询所有 MODs */
    @Query("SELECT * FROM mods WHERE gamePackageName = :gamePackageName")
    fun getModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>

    /** 通过 gamePackageName 查询已启用的 MODs */
    @Query("SELECT * FROM mods WHERE gamePackageName = :gamePackageName AND isEnable = 1")
    fun getEnableModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>

    /** 通过 gamePackageName 查询未启用的 MODs */
    @Query("SELECT * FROM mods WHERE gamePackageName = :gamePackageName AND isEnable = 0")
    fun getDisableModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>

    /** 通过 gamePackageName 查询 MOD 数量 */
    @Query("SELECT COUNT(*) FROM mods WHERE gamePackageName = :gamePackageName")
    fun getModsCountByGamePackageName(gamePackageName: String): Flow<Int>

    /** 通过 gamePackageName 查询已启用 MOD 数量 */
    @Query("SELECT COUNT(*) FROM mods WHERE gamePackageName = :gamePackageName AND isEnable = 1")
    fun getModsEnableCountByGamePackageName(gamePackageName: String): Flow<Int>

    /** 通过 path 查询 MODs */
    @Query("SELECT * FROM mods WHERE path = :path")
    fun getModsByPath(path: String): Flow<List<ModBean>>

    /** 通过 path 查询 MOD 数量 */
    @Query("SELECT COUNT(*) FROM mods WHERE path = :path")
    fun getModsCountByPath(path: String): Flow<Int>

    /** 通过 path 和 gamePackageName 查询 MODs */
    @Query("SELECT * FROM mods WHERE path = :path AND gamePackageName = :gamePackageName")
    fun getModsByPathAndGamePackageName(path: String, gamePackageName: String): Flow<List<ModBean>>

    /** 通过 gamePackageName 和 name 模糊搜索 */
    @Query(
            "SELECT * FROM mods WHERE gamePackageName = :gamePackageName AND name LIKE '%' || :name || '%'"
    )
    fun getModsByGamePackageNameAndName(gamePackageName: String, name: String): Flow<List<ModBean>>

    // ==================== 插入操作 ====================

    /** 插入单个 MOD（冲突时替换） */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(mod: ModBean)

    /** 批量插入 MODs（冲突时替换） */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(mods: List<ModBean>)

    // ==================== 更新操作 ====================

    /** 更新单个 MOD */
    @Update suspend fun update(mod: ModBean)

    /** 批量更新 MODs */
    @Update suspend fun updateMods(mods: List<ModBean>)

    /** 更新 MOD 启用状态 */
    @Query("UPDATE mods SET isEnable = :isEnable WHERE id = :modId")
    suspend fun updateEnableState(modId: Int, isEnable: Boolean)

    // ==================== 删除操作 ====================

    /** 删除单个 MOD */
    @Delete suspend fun delete(mod: ModBean)

    /** 批量删除 MODs */
    @Delete suspend fun deleteMods(mods: List<ModBean>)

    /** 删除指定 gamePackageName 下所有未启用的 MODs */
    @Query("DELETE FROM mods WHERE gamePackageName = :gamePackageName AND isEnable = 0")
    fun deleteDisableMods(gamePackageName: String)
}
