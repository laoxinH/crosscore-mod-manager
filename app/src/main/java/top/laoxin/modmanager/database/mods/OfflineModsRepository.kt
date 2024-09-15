package top.laoxin.modmanager.database.mods

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.ModBean

class OfflineModsRepository(private val modDao: ModDao) : ModRepository {
    override fun getAllIModsStream(): Flow<List<ModBean>> {
        return modDao.getAll()
    }

    override fun search(name: String, gamePackageName: String): Flow<List<ModBean>> {
        return modDao.getModsByGamePackageNameAndName(gamePackageName, name)
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


    override suspend fun insertAll(mods: List<ModBean>) {
        modDao.insertAll(mods)
    }

    override fun getDisableMods(gamePackageName: String): Flow<List<ModBean>> {
        return modDao.getDisableModsByGamePackageName(gamePackageName)
    }

    override suspend fun deleteAll(mods: List<ModBean>) {
        modDao.deleteMods(mods)
    }


    override suspend fun updateAll(mods: List<ModBean>) {
        modDao.updateMods(mods)
    }

    override fun getModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>> {
        return modDao.getModsByGamePackageName(gamePackageName)
    }

    override fun getModsCountByGamePackageName(gamePackageName: String): Flow<Int> {
        return modDao.getModsCountByGamePackageName(gamePackageName)
    }

    override fun getEnableMods(gamePackageName: String): Flow<List<ModBean>> {
        return modDao.getEnableModsByGamePackageName(gamePackageName)
    }


    override fun getModsByPathAndGamePackageName(
        path: String,
        gamePackageName: String
    ): Flow<List<ModBean>> {
        return modDao.getModsByPathAndGamePackageName(path, gamePackageName)
    }

    override fun getEnableModsCountByGamePackageName(gamePackageName: String): Flow<Int> {
        return modDao.getModsEnableCountByGamePackageName(gamePackageName)
    }

    override fun getModsByIds(ids: List<Int>): Flow<List<ModBean>> {
        return modDao.getModsByIds(ids)
    }

    override fun getModsCountByPath(path: String): Flow<Int> {
        return modDao.getModsCountByPath(path)
    }


}