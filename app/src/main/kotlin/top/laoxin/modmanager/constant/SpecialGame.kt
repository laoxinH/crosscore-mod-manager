package top.laoxin.modmanager.constant

import top.laoxin.modmanager.tools.specialGameTools.ArknightsTools
import top.laoxin.modmanager.tools.specialGameTools.BaseSpecialGameTools
import top.laoxin.modmanager.tools.specialGameTools.ProjectSnowTools

enum class SpecialGame(
    val packageName: String,
    val baseSpecialGameTools: BaseSpecialGameTools,
    val needGameService: Boolean = false
) {
    ARKNIGHTS("hypergryph.arknights", ArknightsTools),
    ARKNIGHTS_MOD("com.mrfz", ArknightsTools),
    PROJECTSNOW("dragonli.projectsnow", ProjectSnowTools, true)
}