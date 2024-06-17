package top.laoxin.modmanager.constant

import top.laoxin.modmanager.tools.specialGameTools.ArknightsTools
import top.laoxin.modmanager.tools.specialGameTools.BaseSpecialGameTools

enum class SpecialGame(val packageName : String,val BaseSpecialGameTools : BaseSpecialGameTools) {
    ARKNIGHTS("hypergryph.arknights", ArknightsTools),
}