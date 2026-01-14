package top.laoxin.modmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import top.laoxin.modmanager.domain.bean.ReplacedFileBean

/** 被替换文件记录 DAO */
@Dao
interface ReplacedFileDao {

    // 插入单条记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(replacedFile: ReplacedFileBean)

    // 批量插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(replacedFiles: List<ReplacedFileBean>)

    // 通过 modId 查询
    @Query("SELECT * FROM replaced_files WHERE modId = :modId")
    suspend fun getByModId(modId: Int): List<ReplacedFileBean>

    // 通过 gameFilePath 查询
    @Query("SELECT * FROM replaced_files WHERE gameFilePath = :gameFilePath")
    suspend fun getByGameFilePath(gameFilePath: String): ReplacedFileBean?

    // 通过 gamePackageName 查询所有
    @Query("SELECT * FROM replaced_files WHERE gamePackageName = :gamePackageName")
    suspend fun getByGamePackageName(gamePackageName: String): List<ReplacedFileBean>

    // 通过 modId 删除
    @Query("DELETE FROM replaced_files WHERE modId = :modId") suspend fun deleteByModId(modId: Int)

    // 通过 gameFilePath 删除
    @Query("DELETE FROM replaced_files WHERE gameFilePath = :gameFilePath")
    suspend fun deleteByGameFilePath(gameFilePath: String)

    // 通过 gamePackageName 删除所有
    @Query("DELETE FROM replaced_files WHERE gamePackageName = :gamePackageName")
    suspend fun deleteByGamePackageName(gamePackageName: String)

    // 删除所有
    @Query("DELETE FROM replaced_files") suspend fun deleteAll()

    // 检查 gameFilePath 是否存在记录
    @Query("SELECT EXISTS(SELECT 1 FROM replaced_files WHERE gameFilePath = :gameFilePath)")
    suspend fun existsByGameFilePath(gameFilePath: String): Boolean
}
