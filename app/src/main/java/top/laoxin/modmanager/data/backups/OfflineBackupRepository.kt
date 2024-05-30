package top.laoxin.modmanager.data.backups

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.BackupBean

class OfflineBackupRepository (private val backupDao: BackupDao): BackupRepository{

    override fun getAll(): List<BackupBean> {
        return backupDao.getAll()
    }

    override fun getByName(name: String): BackupBean? {
        return backupDao.getByName(name)
    }



    override suspend fun getByModPath(modPath: String): Flow<List<BackupBean?>> {
        return backupDao.getByModPath(modPath)
    }

    override suspend fun insertAll(backups: List<BackupBean>) {
        backupDao.insertAll(backups)
    }

    override fun deleteAllBackups() {
        backupDao.deleteAll()
    }

    override suspend fun insert(backupBean: BackupBean) {
        backupDao.insert(backupBean)
    }
}