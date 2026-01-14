package top.laoxin.modmanager.domain.repository

import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.model.Result

/**
 * 信息 Repository 接口
 */
interface InformationRepository {
    /**
     * 获取最新信息
     * @return Result<InfoBean?> 成功返回信息，无信息返回 null，失败返回错误
     */
    suspend fun getNewInformation(): Result<InfoBean?>
}
