package top.laoxin.modmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.AntiHarmonyBean

@Dao
interface AntiHarmonyDao {
    // 插入数据
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)  // 如果插入的数据已经存在，则忽略
    suspend fun insert(antiHarmonyBean: AntiHarmonyBean)

    // 通过gamePackageName更新数据
    @Query("UPDATE antiHarmony SET isEnable = :isEnable WHERE gamePackageName = :gamePackageName")
    suspend fun updateByGamePackageName(gamePackageName: String, isEnable: Boolean)

    // 通过gamePackageName读取
    @Query("SELECT * from antiHarmony WHERE gamePackageName = :gamePackageName")
    fun getByGamePackageName(gamePackageName: String): Flow<AntiHarmonyBean?>
}