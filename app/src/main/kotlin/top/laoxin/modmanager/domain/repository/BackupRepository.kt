package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.ModBean

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

    // 备份mod的原始文件
    suspend fun backupModOriginalFiles(
        modBean: ModBean,
        onProgress: suspend (progress:Int, total:Int) -> Unit
    ): List<BackupBean>

    suspend fun getByModIdSync(id: Int):List<BackupBean>
    // 通过modId删除backups
    suspend fun deleteByModId(id: Int)

}