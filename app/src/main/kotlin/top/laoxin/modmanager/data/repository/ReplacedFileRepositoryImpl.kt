package top.laoxin.modmanager.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.domain.bean.ReplacedFileBean
import top.laoxin.modmanager.data.dao.ReplacedFileDao
import top.laoxin.modmanager.domain.repository.ReplacedFileRepository

/** 被替换文件记录 Repository 实现 */
@Singleton
class ReplacedFileRepositoryImpl @Inject constructor(private val database: ModManagerDatabase,) :
        ReplacedFileRepository {
    val replacedFileDao: ReplacedFileDao = database.replacedFileDao()
    override suspend fun saveReplacedFiles(files: List<ReplacedFileBean>) {
        replacedFileDao.insertAll(files)
    }

    override suspend fun getByModId(modId: Int): List<ReplacedFileBean> {
        return replacedFileDao.getByModId(modId)
    }

    override suspend fun getByGameFilePath(gameFilePath: String): ReplacedFileBean? {
        return replacedFileDao.getByGameFilePath(gameFilePath)
    }

    override suspend fun getByGamePackageName(gamePackageName: String): List<ReplacedFileBean> {
        return replacedFileDao.getByGamePackageName(gamePackageName)
    }

    override suspend fun deleteByModId(modId: Int) {
        replacedFileDao.deleteByModId(modId)
    }

    override suspend fun existsByGameFilePath(gameFilePath: String): Boolean {
        return replacedFileDao.existsByGameFilePath(gameFilePath)
    }
}
