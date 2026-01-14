package top.laoxin.modmanager.domain.service

import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.Result

/** 主动式 MOD 扫描服务接口 负责扫描包含 JSON 配置文件的 MOD 适用于 ModForm.ACTIVE 类型的 MOD */
interface ActiveModScanService {

    /**
     * 扫描压缩包，读取 JSON 配置识别 MOD
     * @param archivePath 压缩包完整路径
     * @param gameInfo 游戏配置信息
     * @return 识别到的 MOD 列表
     */
    suspend fun scanArchive(archivePath: String, gameInfo: GameInfoBean): Result<List<ModBean>>

    /**
     * 扫描文件夹，读取 JSON 配置识别 MOD
     * @param folderPath 文件夹完整路径
     * @param gameInfo 游戏配置信息
     * @return 识别到的 MOD 列表
     */
    suspend fun scanDirectoryMod(folderPath: String, gameInfo: GameInfoBean): Result<List<ModBean>>
}
