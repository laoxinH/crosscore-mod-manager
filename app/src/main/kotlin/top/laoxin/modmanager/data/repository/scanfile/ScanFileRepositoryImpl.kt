package top.laoxin.modmanager.data.repository.scanfile

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.ScanFileBean
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import top.laoxin.modmanager.domain.repository.ScanFileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanFileRepositoryImpl @Inject constructor(private val database: ModManagerDatabase) :
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

    override suspend fun getByPathSync(path: String): ScanFileBean? {
        return scanFileDao.getByPathSync(path)
    }

    override fun getByGamePackageName(gamePackageName: String): Flow<List<ScanFileBean>> {
        return scanFileDao.getByGamePackageName(gamePackageName)
    }

    override suspend fun getByGamePackageNameSync(gamePackageName: String): List<ScanFileBean> {
        return scanFileDao.getByGamePackageNameSync(gamePackageName)
    }

    override suspend fun insertAll(scanFiles: List<ScanFileBean>) {
        scanFileDao.insertAll(scanFiles)
    }

    override suspend fun deleteAll() {
        scanFileDao.deleteAll()
    }

    override suspend fun delete(it: ScanFileBean) {
        scanFileDao.delete(it)
    }

    override suspend fun deleteByPath(path: String) {
        scanFileDao.deleteByPath(path)
    }

    override suspend fun deleteByGamePackageName(gamePackageName: String) {
        scanFileDao.deleteByGamePackageName(gamePackageName)
    }

    override suspend fun update(scanFile: ScanFileBean) {
        scanFileDao.update(scanFile)
    }
}