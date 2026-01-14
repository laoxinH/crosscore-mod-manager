package top.laoxin.modmanager.domain.repository

import top.laoxin.modmanager.domain.bean.ReplacedFileBean

/** 被替换文件记录 Repository */
interface ReplacedFileRepository {

    /** 保存替换文件记录列表 */
    suspend fun saveReplacedFiles(files: List<ReplacedFileBean>)

    /** 通过 modId 获取记录 */
    suspend fun getByModId(modId: Int): List<ReplacedFileBean>

    /** 通过 gameFilePath 获取记录 */
    suspend fun getByGameFilePath(gameFilePath: String): ReplacedFileBean?

    /** 通过 gamePackageName 获取所有记录 */
    suspend fun getByGamePackageName(gamePackageName: String): List<ReplacedFileBean>

    /** 通过 modId 删除记录 */
    suspend fun deleteByModId(modId: Int)

    /** 检查 gameFilePath 是否已有记录 */
    suspend fun existsByGameFilePath(gameFilePath: String): Boolean
}
