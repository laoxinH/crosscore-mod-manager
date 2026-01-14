package top.laoxin.modmanager.domain.repository

import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.bean.ThanksBean
import top.laoxin.modmanager.domain.model.Result

/**
 * 应用数据 Repository 接口
 * 整合应用级别的数据获取，如公告信息、感谢名单等
 */
interface AppDataRepository {
    /**
     * 获取最新公告信息
     * @return Result<InfoBean?> 成功返回信息，无信息返回 null，失败返回错误
     */
    suspend fun getNewInformation(): Result<InfoBean?>

    /**
     * 获取感谢名单
     * @return Result<List<ThanksBean>> 成功返回感谢名单列表，失败返回错误
     */
    suspend fun getThanksList(): Result<List<ThanksBean>>
}
