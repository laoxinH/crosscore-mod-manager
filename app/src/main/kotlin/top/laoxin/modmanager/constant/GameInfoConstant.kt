package top.laoxin.modmanager.constant

import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.tools.ModTools

object GameInfoConstant {
    val CROSSCORE = GameInfoBean(
        "交错战线",
        "官服",
        "com.megagame.crosscore",
        ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/",
        "crosscore/",
        ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/internation.txt",
        "1",
        mutableListOf(
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/Custom/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/videos/login/",
        ),
        "",
        mutableListOf(
            "游戏模型",
            "登录页面",
        ),
        false
    )
    val CROSSCOREB = GameInfoBean(
        "交错战线",
        "B服",
        "com.megagame.crosscore.bilibili",
        ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/",
        "crosscore/",
        ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/internation.txt",
        "1",
        mutableListOf(
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/Custom/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/videos/login/",
        ),
        "",
        mutableListOf(
            "游戏模型",
            "登录页面",
        ),
        false
    )

    /*    val PROJECTSNOW = GameInfo(
            "尘白禁区",
            "官服",
            "com.dragonli.projectsnow.lhm",
            ModTools.ROOT_PATH + "/Android/data/com.dragonli.projectsnow.lhm/",
            "",
            "",
            "",
            mutableListOf(
                ModTools.ROOT_PATH + "/Android/data/com.dragonli.projectsnow.lhm/files/2.0.0/",
            ),
            "2.0.0",
            mutableListOf(
                "游戏模型",
            ),
            isGameFileRepeat = false,
            enableBackup = false,
            tips = "第一次选择尘白禁区游戏需要先清除游戏数据或者卸载重装再选择(清除或者卸载重装后先运行游戏到登录界面,然后回到这里选择游戏后再运行游戏下载游戏数据),否则无法开启MOD"
        )*/
    val NO_GAME = GameInfoBean(
        "请前往设置页面选择游戏",
        "请前往设置页面选择游戏",
        "",
        "",
        "",
        "",
        "",
        mutableListOf(),
        "请前往设置页面选择游戏",
        mutableListOf(),
        true
    )
    val gameInfoList = mutableListOf(NO_GAME, CROSSCORE, CROSSCOREB)
}
