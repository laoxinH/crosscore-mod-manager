package top.laoxin.modmanager.tools.specialGameTools

import android.util.Log
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
        "projectsnow" to projectSnowTools,
        "snowbreak" to projectSnowTools
    )
    companion object {
       const val TAG = "SpecialGameToolsManager"
    }

    fun getSpecialGameTools(packageName: String): BaseSpecialGameTools? {
        // 遍历map
        Log.d(TAG, "getSpecialGameTools查询: $packageName")
        for ((key, value) in specialGameTools) {
            if (packageName.contains(key)) {
                return value
            }
        }
        Log.d(TAG, "getSpecialGameTools查询失败: $packageName")
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