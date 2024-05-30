package top.laoxin.modmanager.data.backups

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.BackupBean

interface BackupRepository {

    // 插入数据
    suspend fun insert(backupBean: BackupBean)

    // 获取所有数据
    fun getAll(): List<BackupBean>


    // 通过name查询数据
    fun getByName(name: String):BackupBean?


    // 通过modPath查询
    suspend fun getByModPath(modPath: String): Flow<List<BackupBean?>>

    // 插入所有数据
    suspend fun insertAll(backups: List<BackupBean>)

    fun deleteAllBackups()
}