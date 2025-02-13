package top.laoxin.modmanager.constant

import android.os.Environment
import top.laoxin.modmanager.data.bean.GameInfoBean


object GameInfoConstant {
    val ROOT_PATH = Environment.getExternalStorageDirectory().path
    val CROSSCORE = GameInfoBean(
        "交错战线",
        "官服",
        "com.megagame.crosscore",
        ROOT_PATH + "/Android/data/com.megagame.crosscore/",
        "crosscore/",
        ROOT_PATH + "/Android/data/com.megagame.crosscore/files/internation.txt",
        "1",
        mutableListOf(
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/Custom/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/videos/login/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/cv_skin/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/bgms/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/picture/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore/files/sounds/cv/fight/effect",

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
        ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/",
        "crosscore/",
        ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/internation.txt",
        "1",
        mutableListOf(
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/Custom/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/videos/login/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/cv_skin/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/bgms/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/picture/",
            ROOT_PATH + "/Android/data/com.megagame.crosscore.bilibili/files/sounds/cv/fight/effect",
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
