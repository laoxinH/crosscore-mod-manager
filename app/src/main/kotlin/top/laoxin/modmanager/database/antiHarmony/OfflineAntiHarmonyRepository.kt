package top.laoxin.modmanager.database.antiHarmony

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.AntiHarmonyBean

class OfflineAntiHarmonyRepository(private val antiHarmonyDao: AntiHarmonyDao) :
    AntiHarmonyRepository {
    override suspend fun updateByGamePackageName(gamePackageName: String, isEnable: Boolean) {
        antiHarmonyDao.updateByGamePackageName(gamePackageName, isEnable)
    }

    override fun getByGamePackageName(gamePackageName: String): Flow<AntiHarmonyBean?> {
        return antiHarmonyDao.getByGamePackageName(gamePackageName)
    }

    override suspend fun insert(antiHarmonyBean: AntiHarmonyBean) {
        antiHarmonyDao.insert(antiHarmonyBean)
    }
}