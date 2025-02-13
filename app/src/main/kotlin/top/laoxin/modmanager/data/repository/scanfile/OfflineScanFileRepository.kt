package top.laoxin.modmanager.data.repository.scanfile

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.bean.ScanFileBean
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineScanFileRepository @Inject constructor(private val database: ModManagerDatabase) :
    ScanFileRepository {
    private val scanFileDao = database.scanFileDao()
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

    override fun update(scanFile: ScanFileBean) {
        scanFileDao.update(scanFile)
    }

}