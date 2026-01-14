package top.laoxin.modmanager.domain.service

import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result

/** 打包式 MOD 扫描服务接口 负责扫描包含配置文件且需要打包 Unity 资源的 MOD 适用于 ModForm.PACKAGED 类型的 MOD */
interface PackagedModScanService {

    /**
     * 扫描压缩包，读取配置并处理 Unity 资源
     * @param archivePath 压缩包完整路径
     * @param gameInfo 游戏配置信息
     * @return 识别到的 MOD 列表
     */
    suspend fun scanArchive(archivePath: String, gameInfo: GameInfoBean): Result<List<ModBean>>

    /**
     * 扫描文件夹，读取配置并处理 Unity 资源
     * @param folderPath 文件夹完整路径
     * @param gameInfo 游戏配置信息
     * @return 识别到的 MOD 列表
     */
    suspend fun scanDirectoryMod(folderPath: String, gameInfo: GameInfoBean): Result<List<ModBean>>
}
