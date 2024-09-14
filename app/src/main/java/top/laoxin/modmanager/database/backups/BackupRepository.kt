package top.laoxin.modmanager.database.backups

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.BackupBean

interface BackupRepository {

    // 插入数据
    suspend fun insert(backupBean: BackupBean)


    // 通过modPath查询
    suspend fun getByModPath(modPath: String): Flow<List<BackupBean>>

    // 插入所有数据
    suspend fun insertAll(backups: List<BackupBean>)

    fun deleteAllBackups()

    // 通过包名删除mods
    suspend fun deleteByGamePackageName(gamePackageName: String)

    // 通过modName和gamePackageName查询backups
    fun getByModNameAndGamePackageName(
        modName: String,
        gamePackageName: String
    ): Flow<List<BackupBean>>

}