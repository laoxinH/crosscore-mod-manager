package top.laoxin.modmanager.domain.usercase.mod

import javax.inject.Inject
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.service.ModDecryptService

/**
 * 刷新 MOD 详情 UseCase
 * 
 * 当用户误删缓存导致图片或描述无法显示时：
 * - 压缩包 MOD：重新从压缩包提取图片和 README 到缓存
 * - 文件夹 MOD：重新扫描文件夹中的图片和 README 文件
 */
class RefreshModDetailUseCase
@Inject
constructor(
    private val modDecryptService: ModDecryptService,
    private val modRepository: ModRepository
) {
    /**
     * 刷新单个 MOD 的详情（图片、图标、README）
     * 
     * @param mod 需要刷新的 MOD
     * @return Result<ModBean> 刷新后的 MOD
     */
    suspend operator fun invoke(mod: ModBean): Result<ModBean> {
        // 1. 调用服务刷新 MOD 详情（压缩包或文件夹）
        val result = modDecryptService.refreshModDetail(mod)
        if (result is Result.Error) {
            return result
        }

        // 2. 更新数据库中的 MOD 信息
        val updatedMod = (result as Result.Success).data
        modRepository.updateMod(updatedMod)

        return Result.Success(updatedMod)
    }
}

