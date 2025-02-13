package top.laoxin.modmanager.constant

import androidx.annotation.IntDef

interface ResultCode {
    @IntDef(*[NO_PERMISSION, FAIL,SUCCESS,NOT_SUPPORT,NO_MY_APP_PERMISSION,NO_SELECTED_GAME, MOD_NEED_PASSWORD, NO_EXECUTE, HAVE_ENABLE_MODS])
    annotation class ResultCode
    companion object {


        // 权限不足
        const val NO_PERMISSION = -1
        // 失败
        const val FAIL = -2
        // 不支持
        const val NOT_SUPPORT = -3
        // 无本APP文件目录权限
        const val NO_MY_APP_PERMISSION = -4
        // 未选择游戏
        const val NO_SELECTED_GAME = -5
        // mod需要密码
        const val MOD_NEED_PASSWORD = -6
        // 不执行
        const val NO_EXECUTE = -7
        // 存在开启的mods
        const val HAVE_ENABLE_MODS = -8

        // 成功
        const val SUCCESS = 0
    }
}

