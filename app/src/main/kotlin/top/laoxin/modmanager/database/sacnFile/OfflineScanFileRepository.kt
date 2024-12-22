package top.laoxin.modmanager.database.sacnFile

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ScanFileBean

class OfflineScanFileRepository(private val scanFileDao: ScanFileDao) : ScanFileRepository {
    override suspend fun insert(scanFile: ScanFileBean) {
        scanFileDao.insert(scanFile)
    }

    override fun getAll(): Flow<List<ScanFileBean>> {
        return scanFileDao.getAll()
    }

    override fun getByPath(path: String): Flow<List<ScanFileBean>> {
        return scanFileDao.getByPath(path)
    }

    override suspend fun insertAll(scanFiles: List<ScanFileBean>) {
        scanFileDao.insertAll(scanFiles)
    }

    override fun deleteAll() {
        scanFileDao.deleteAll()
    }

    override fun delete(it: ScanFileBean) {
        scanFileDao.delete(it)
    }

}