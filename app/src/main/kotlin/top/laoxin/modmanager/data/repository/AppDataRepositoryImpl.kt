package top.laoxin.modmanager.data.repository

import android.util.Log
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.bean.ThanksBean
import top.laoxin.modmanager.data.network.ModManagerApi
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.AppDataRepository
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用数据 Repository 实现
 * 整合应用级别的数据获取，如公告信息、感谢名单等
 */
@Singleton
class AppDataRepositoryImpl @Inject constructor() : AppDataRepository {

    companion object {
        private const val TAG = "AppDataRepository"
    }

    /**
     * 获取最新公告信息
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
            Log.e(TAG, "获取公告信息失败", e)
            Result.Error(AppError.NetworkError.Unknown(e.message ?: "获取信息失败"))
        }
    }

    /**
     * 获取感谢名单
     */
    override suspend fun getThanksList(): Result<List<ThanksBean>> {
        return try {
            val thanks = ModManagerApi.retrofitService.getThanksList()
            Result.Success(thanks)
        } catch (e: UnknownHostException) {
            Result.Error(AppError.NetworkError.NoConnection)
        } catch (e: SocketTimeoutException) {
            Result.Error(AppError.NetworkError.Timeout)
        } catch (e: retrofit2.HttpException) {
            Result.Error(AppError.NetworkError.ServerError(e.code(), e.message()))
        } catch (e: Exception) {
            Log.e(TAG, "获取感谢名单失败", e)
            Result.Error(AppError.NetworkError.Unknown(e.message ?: "获取感谢名单失败"))
        }
    }
}
