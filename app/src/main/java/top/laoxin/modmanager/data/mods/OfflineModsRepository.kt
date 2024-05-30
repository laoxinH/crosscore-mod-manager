package top.laoxin.modmanager.data.mods

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.data.mods.ModDao
import top.laoxin.modmanager.data.mods.ModRepository

class OfflineModsRepository(private val modDao: ModDao) : ModRepository {
    override fun getAllIModsStream(): Flow<List<ModBean>> {
        return modDao.getAll()
    }

    override fun getModStream(id: Int): Flow<ModBean?> {
        return  modDao.getItem(id)
    }

    override suspend fun insertMod(mod: ModBean) {
        modDao.insert(mod)
    }

    override suspend fun deleteMod(mod: ModBean) {
        modDao.delete(mod)
    }

    override suspend fun updateMod(mod: ModBean) {
        modDao.update(mod)
    }

    override fun search(name: String): Flow<List<ModBean>> {
        return  modDao.search(name)
    }

    override fun getEnableMods(): Flow<List<ModBean>> {
       return modDao.getEnableMods()
    }

    override suspend fun insertAll(mods: List<ModBean>) {
        modDao.insertAll(mods)
    }

    override fun getModCount(): Flow<Int> {
        return modDao.getModCount()
    }

    override fun getEnableModCount(): Flow<Int> {
        return modDao.getEnableModCount()
    }

    override fun getDisableMods(): Flow<List<ModBean>> {
        return modDao.getDisableMods()
    }

    override suspend fun deleteAll(mods: List<ModBean>) {
        modDao.deleteMods(mods)
    }

    override fun getByPath(path: String): Flow<List<ModBean>> {
        return modDao.getModByPath(path)
    }

    override suspend fun updateAll(mods: List<ModBean>) {
        modDao.updateMods(mods)
    }
}