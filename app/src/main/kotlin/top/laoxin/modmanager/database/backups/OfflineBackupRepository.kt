package top.laoxin.modmanager.database.backups

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.BackupBean

class OfflineBackupRepository(private val backupDao: BackupDao) : BackupRepository {


    override suspend fun getByModPath(modPath: String): Flow<List<BackupBean>> {
        return backupDao.getByModPath(modPath)
    }

    override suspend fun insertAll(backups: List<BackupBean>) {
        backupDao.insertAll(backups)
    }

    override fun deleteAllBackups() {
        backupDao.deleteAll()
    }

    override suspend fun deleteByGamePackageName(gamePackageName: String) {
        backupDao.deleteByGamePackageName(gamePackageName)
    }

    override fun getByModNameAndGamePackageName(
        modName: String,
        gamePackageName: String
    ): Flow<List<BackupBean>> {
        return backupDao.getByModNameAndGamePackageName(modName, gamePackageName)
    }


    override suspend fun insert(backupBean: BackupBean) {
        backupDao.insert(backupBean)
    }
}