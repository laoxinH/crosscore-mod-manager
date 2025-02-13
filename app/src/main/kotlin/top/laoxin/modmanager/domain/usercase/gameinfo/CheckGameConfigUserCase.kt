package top.laoxin.modmanager.domain.usercase.gameinfo

import android.util.Log
import top.laoxin.modmanager.data.bean.GameInfoBean
import top.laoxin.modmanager.di.FileToolsModule
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckGameConfigUserCase @Inject constructor(
    @FileToolsModule.FileToolsImpl private val fileTools: BaseFileTools
) {
    operator fun invoke(gameInfo: GameInfoBean, rootPath : String) : GameInfoBean  {
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

}