package top.laoxin.modmanager.constant

import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.tools.ModTools

object GameInfoConstant{
    val CROSSCORE = GameInfo(
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
    val CROSSCOREB = GameInfo(
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
    val NO_GAME = GameInfo(
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
    val gameInfoList = mutableListOf(NO_GAME,CROSSCORE,CROSSCOREB)
}
