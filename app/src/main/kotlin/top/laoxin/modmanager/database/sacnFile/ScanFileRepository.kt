package top.laoxin.modmanager.database.sacnFile

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.ScanFileBean

interface ScanFileRepository {

    // 插入一条数据
    suspend fun insert(scanFile: ScanFileBean)

    // 获取所有数据
    fun getAll(): Flow<List<ScanFileBean>>

    // 通过path查询
    fun getByPath(path: String): Flow<List<ScanFileBean>>

    // 插入多条数据
    suspend fun insertAll(scanFiles: List<ScanFileBean>)

    // 删除所有数据
    fun deleteAll()

    fun delete(it: ScanFileBean)


}