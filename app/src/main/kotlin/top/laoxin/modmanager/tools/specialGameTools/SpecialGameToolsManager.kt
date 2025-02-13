package top.laoxin.modmanager.tools.specialGameTools

import top.laoxin.modmanager.di.SpecialGameToolsModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpecialGameToolsManager @Inject constructor(
    @SpecialGameToolsModule.ArknightsToolsImpl private val arknightsTools: BaseSpecialGameTools,
    @SpecialGameToolsModule.ProjectSnowToolsImpl private val projectSnowTools: BaseSpecialGameTools
) {
    private val specialGameTools : Map<String, BaseSpecialGameTools>  = mapOf(
        "hypergryph.arknights" to arknightsTools,
        "com.mrfz" to arknightsTools,
        "SpecialGame.PROJECTSNOW.packageName" to projectSnowTools
    )

    fun getSpecialGameTools(packageName: String): BaseSpecialGameTools? {
        // 遍历map
        for ((key, value) in specialGameTools) {
            if (packageName.contains(key)) {
                return value
            }
        }
        return null
    }

    // 获取arknightsTools
    fun getArknightsTools(): BaseSpecialGameTools {
        return arknightsTools
    }

    // 获取projectSnowTools
    fun getProjectSnowTools(): BaseSpecialGameTools {
        return projectSnowTools
    }

}