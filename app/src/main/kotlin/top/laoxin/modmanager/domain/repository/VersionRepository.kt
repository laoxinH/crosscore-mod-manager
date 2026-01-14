package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.model.Result

/**
 * 更新信息
 */
data class UpdateInfo(
    val downloadUrl: String,
    val universalUrl: String,
    val changelog: String,
    val versionName: String
)

/**
 * 版本检查 Repository 接口
 */
interface VersionRepository {
    /**
     * 更新信息 Flow
     */
    val updateInfo: Flow<UpdateInfo?>

    /**
     * 保存更新信息
     */
    suspend fun saveUpdateInfo(updateInfo: UpdateInfo?): Result<Unit>

    /**
     * 从网络获取最新更新信息
     * @return Result<UpdateInfo?> 有更新返回更新信息，无更新返回 null，失败返回错误
     */
    suspend fun getNewUpdateInfo(): Result<UpdateInfo?>
}
