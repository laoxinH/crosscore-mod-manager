package top.laoxin.modmanager.constant

import androidx.annotation.IntDef


interface PathType {
    @IntDef(*[FILE, DOCUMENT, PACKAGE_NAME, SHIZUKU])
    annotation class PathType1
    companion object {
        /**
         * 通过File接口，访问一般路径
         */
        const val FILE = 0

        /**
         * 通过Document API 访问特殊路径
         */
        const val DOCUMENT = 1

        /**
         * 安卓13及以上，直接用包名展示data、obb下的目录（因为data、obb不能直接授权了，只能对子目录授权）
         */
        const val PACKAGE_NAME = 2

        /**
         * 通过Shizuku授权访问特殊路径
         */
        const val SHIZUKU = 3

        /**
         * 无权限
         */
        const val NULL = -1
    }
}
