package top.laoxin.modmanager.constant

enum class GameInfoConstant(val gameName: String, val serviceName : String, val packageName: String, val gamePath: String) {
    CROSSCORE("交错战线", "官服","com.megagame.crosscore", "/storage/emulated/0/Android/data/com.megagame.crosscore/"),
    CROSSCOREB("交错战线", "B服","com.megagame.crosscore.bilibili", "/storage/emulated/0/Android/data/com.megagame.crosscore.bilibili/"),
}