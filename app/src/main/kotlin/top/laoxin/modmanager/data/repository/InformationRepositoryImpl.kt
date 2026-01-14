package top.laoxin.modmanager.data.repository

import android.util.Log
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.InformationRepository
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InformationRepositoryImpl @Inject constructor() : InformationRepository {

    /**
     * 获取最新信息
     */
    override suspend fun getNewInformation(): Result<InfoBean?> {
        return try {
            val info = ModManagerApi.retrofitService.getInfo()
            Result.Success(info)
        } catch (e: UnknownHostException) {
            Result.Error(AppError.NetworkError.NoConnection)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.NetworkError.Timeout)
        } catch (e: retrofit2.HttpException) {
            Result.Error(AppError.NetworkError.ServerError(e.code(), e.message()))
        } catch (e: Exception) {
            Log.e("InfoRepo", "Failed to fetch information", e)
            Result.Error(AppError.NetworkError.Unknown(e.message ?: "获取信息失败"))
        }
    }
}
