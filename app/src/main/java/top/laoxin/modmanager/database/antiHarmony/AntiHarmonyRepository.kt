package top.laoxin.modmanager.database.antiHarmony

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.AntiHarmonyBean

interface AntiHarmonyRepository {
    // 通过gamePackageName更新数据
    suspend fun updateByGamePackageName(gamePackageName: String, isEnable: Boolean)

    // 通过gamePackageName读取
    fun getByGamePackageName(gamePackageName: String): Flow<AntiHarmonyBean?>

    // 插入数据
    suspend fun insert(antiHarmonyBean: AntiHarmonyBean)
}