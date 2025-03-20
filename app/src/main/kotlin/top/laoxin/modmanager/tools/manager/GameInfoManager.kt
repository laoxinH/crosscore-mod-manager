package top.laoxin.modmanager.tools.manager

import android.util.Log
import com.google.gson.Gson
import top.laoxin.modmanager.constant.GameInfoConstant.CROSSCORE
import top.laoxin.modmanager.constant.GameInfoConstant.CROSSCOREB
import top.laoxin.modmanager.constant.GameInfoConstant.NO_GAME
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameInfoManager @Inject constructor(
    @param:FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools,
    private val appPathsManager: AppPathsManager,
) {
    private val gameInfoList = mutableListOf<GameInfoBean>()
    private var gameInfo: GameInfoBean = NO_GAME

    init {
        loadGameInfo()
    }

    fun loadGameInfo() {
        try {
            if (this.gameInfoList.isEmpty()) this.gameInfoList.addAll(
                mutableListOf(
                    NO_GAME,
                    CROSSCORE,
                    CROSSCOREB
                )
            )
            val listFiles =
                fileTools.listFiles(appPathsManager.getMyAppPath() + appPathsManager.getGameConfig())
            val gameInfoList = mutableListOf<GameInfoBean>()
            for (listFile in listFiles) {
                if (listFile.name.endsWith(".json")) {
                    try {
                        val fromJson =
                            Gson().fromJson(listFile.readText(), GameInfoBean::class.java)
                        val checkGameInfo = checkGameConfig(fromJson, appPathsManager.getRootPath())
                        gameInfoList.add(checkGameInfo)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val filter =
                gameInfoList.filter { this.gameInfoList.none { gameInfo -> gameInfo.packageName == it.packageName } }

            if (filter.isNotEmpty()) {
                this.gameInfoList.addAll(filter)
            }
        } catch (e: Exception) {
            Log.e("LoadGameConfig", "读取游戏配置失败: $e")
        }
    }


    private fun checkGameConfig(gameInfo: GameInfoBean, rootPath: String): GameInfoBean {
        Log.d("LoadGameConfig", "gameInfo: $gameInfo")
        var result = gameInfo.copy()
        if (gameInfo.gameName.isEmpty()) {
            throw Exception("gameName : 游戏名称不能为空")
        }

        if (gameInfo.packageName.isEmpty()) {
            throw Exception("packageName不能为空")
        } else {
            val pattern = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)+([.][a-zA-Z_][a-zA-Z0-9_]*)+\$\n")
            if (pattern.matches(gameInfo.packageName)) {
                throw Exception("packageName包名不合法")
            }
        }

        if (gameInfo.gamePath.isEmpty()) {
            throw Exception("gamePath : 游戏data根目录不能为空")
        } else {
            result = result.copy(
                gamePath = rootPath + "/Android/data/" + gameInfo.packageName + "/"
            )

        }
        if (gameInfo.antiHarmonyFile.isNotEmpty()) {
            result = result.copy(
                antiHarmonyFile = (rootPath + "/" + gameInfo.antiHarmonyFile).replace(
                    "//",
                    "/"
                )
            )
        }

        if (gameInfo.modType.isEmpty()) {
            throw Exception("modType不能为空")
        }
        if (gameInfo.gameFilePath.isEmpty()) {
            throw Exception("gameFilePath不能为空")
        } else {
            val paths = mutableListOf<String>()
            for (path in gameInfo.gameFilePath) {
                paths.add("$rootPath/$path/".replace("//", "/"))
            }
            result = result.copy(gameFilePath = paths)
        }
        if (gameInfo.serviceName.isEmpty()) {
            throw Exception("serviceName不能为空")
        }
        if (gameInfo.gameFilePath.size != gameInfo.modType.size) {
            throw Exception("gameFilePath和modType的列表必须对应")
        }
        return result
    }

    fun getGameInfoList(): List<GameInfoBean> {
        return gameInfoList
    }

    fun getGameInfoByIndex(index: Int): GameInfoBean {
        return gameInfoList[index]
    }

    // 设置当前游戏
    fun setGameInfo(gameInfo: GameInfoBean) {
        this.gameInfo = gameInfo
    }

    // 获取当前游戏
    fun getGameInfo(): GameInfoBean {
        return gameInfo
    }


}