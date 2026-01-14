package top.laoxin.modmanager.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.data.network.GithubApi
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.UpdateInfo
import top.laoxin.modmanager.domain.repository.VersionRepository
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionRepositoryImpl @Inject constructor() : VersionRepository {

    private val _updateInfo: MutableSharedFlow<UpdateInfo?> = MutableSharedFlow()
    override val updateInfo: Flow<UpdateInfo?> = _updateInfo

    override suspend fun saveUpdateInfo(updateInfo: UpdateInfo?): Result<Unit> {
        return try {
            _updateInfo.emit(updateInfo)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun getNewUpdateInfo(): Result<UpdateInfo?> {
        return try {
            val currentVersion = BuildConfig.VERSION_NAME
            val release = GithubApi.retrofitService.getLatestRelease()
            if (release.version != currentVersion) {
                Log.d("VersionRepo", "Update available from network: ${release.version}")
                val newUpdate = UpdateInfo(
                    downloadUrl = release.getDownloadLink(),
                    universalUrl = release.getDownloadLinkUniversal(),
                    changelog = release.info,
                    versionName = release.version
                )
                Result.Success(newUpdate)
            } else {
                Log.d("VersionRepo", "App is up to date.")

                Result.Success(null) // 无更新
            }
        } catch (e: UnknownHostException) {
            Result.Error(AppError.NetworkError.NoConnection)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.NetworkError.Timeout)
        } catch (e: retrofit2.HttpException) {
            Result.Error(AppError.NetworkError.ServerError(e.code(), e.message()))
        } catch (e: Exception) {
            Log.e("VersionRepo", "Failed to fetch update info", e)
            Result.Error(AppError.NetworkError.Unknown(e.message ?: "获取更新信息失败"))
        }
    }
}
