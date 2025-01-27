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
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/cv_skin/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/bgms/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/picture/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/fight/effect",

            ),
        "",
        mutableListOf(
            "游戏模型",
            "登录页面",
            "角色语言",
            "皮肤语音",
            "背景音乐",
            "图册语音",
            "战斗音效",
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
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/cv_skin/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/bgms/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/picture/",
            ModTools.ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/fight/effect",

            ),
        "",
        mutableListOf(
            "游戏模型",
            "登录页面",
            "角色语言",
            "皮肤语音",
            "背景音乐",
            "图册语音",
            "战斗音效",

            ),
        false
    )

    val NO_GAME = GameInfoBean(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        mutableListOf(),
        "",
        mutableListOf(),
        true
    )
    val gameInfoList = mutableListOf(NO_GAME, CROSSCORE, CROSSCOREB)
}
