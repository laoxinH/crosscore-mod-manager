package top.laoxin.modmanager.data.repository.antiharmony

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.bean.AntiHarmonyBean
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import javax.inject.Inject

class OfflineAntiHarmonyRepository @Inject constructor(private val database: ModManagerDatabase) :
    AntiHarmonyRepository {
    private val antiHarmonyDao = database.antiHarmonyDao()
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