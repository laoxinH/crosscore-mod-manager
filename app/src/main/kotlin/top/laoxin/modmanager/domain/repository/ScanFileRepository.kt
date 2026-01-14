package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.ScanFileBean

interface ScanFileRepository {

    // 插入一条数据
    suspend fun insert(scanFile: ScanFileBean)

    // 获取所有数据
    fun getAll(): Flow<List<ScanFileBean>>

    // 通过path查询（Flow）
    fun getByPath(path: String): Flow<List<ScanFileBean>>

    // 通过path查询单条记录（suspend，用于快速检查）
    suspend fun getByPathSync(path: String): ScanFileBean?

    // 通过gamePackageName查询
    fun getByGamePackageName(gamePackageName: String): Flow<List<ScanFileBean>>

    // 通过gamePackageName查询（suspend）
    suspend fun getByGamePackageNameSync(gamePackageName: String): List<ScanFileBean>

    // 插入多条数据
    suspend fun insertAll(scanFiles: List<ScanFileBean>)

    // 删除所有数据
    suspend fun deleteAll()

    // 删除一条数据
    suspend fun delete(it: ScanFileBean)

    // 通过path删除
    suspend fun deleteByPath(path: String)

    // 通过gamePackageName删除
    suspend fun deleteByGamePackageName(gamePackageName: String)

    // 更新一条数据
    suspend fun update(scanFile: ScanFileBean)
}