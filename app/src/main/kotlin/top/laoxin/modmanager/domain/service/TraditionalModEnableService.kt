package top.laoxin.modmanager.domain.service

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.ModBean

/** 传统形式 MOD 开启服务接口 处理压缩包和文件夹形式的 MOD 文件操作 */
interface TraditionalModEnableService {

    /**
     * 开启压缩包 MOD 使用流式提取，将 modFiles 中的文件替换到 gameFilesPath
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<EnableFileEvent> 文件级事件流
     */
    fun enableZipMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent>

    /**
     * 关闭压缩包 MOD（通过还原备份实现）
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<EnableFileEvent> 文件级事件流
     */
    fun disableZipMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent>

    /**
     * 开启文件夹 MOD 将 modFiles 中的文件复制到 gameFilesPath
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<EnableFileEvent> 文件级事件流
     */
    fun enableFolderMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent>

    /**
     * 关闭文件夹 MOD（通过还原备份实现）
     * @param mod MOD 信息
     * @param gameInfo 游戏信息
     * @return Flow<EnableFileEvent> 文件级事件流
     */
    fun disableFolderMod(mod: ModBean, gameInfo: GameInfoBean): Flow<EnableFileEvent>
}
