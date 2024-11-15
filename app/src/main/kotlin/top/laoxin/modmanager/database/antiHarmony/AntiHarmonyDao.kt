package top.laoxin.modmanager.database.antiHarmony

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.AntiHarmonyBean

@Dao
interface AntiHarmonyDao {
    // 插入数据
    @Insert(onConflict = OnConflictStrategy.IGNORE)  // 如果插入的数据已经存在，则忽略
    suspend fun insert(antiHarmonyBean: AntiHarmonyBean)

    // 通过gamePackageName更新数据
    @androidx.room.Query("UPDATE antiHarmony SET isEnable = :isEnable WHERE gamePackageName = :gamePackageName")
    suspend fun updateByGamePackageName(gamePackageName: String, isEnable: Boolean)

    // 通过gamePackageName读取
    @androidx.room.Query("SELECT * from antiHarmony WHERE gamePackageName = :gamePackageName")
    fun getByGamePackageName(gamePackageName: String): Flow<AntiHarmonyBean?>
}