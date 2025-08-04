package top.laoxin.modmanager.constant


interface PathType {
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
         * 通过Shizuku授权访问特殊路径
         */
        const val SHIZUKU = 3

        /**
         * 无权限
         */
        const val NULL = -1
    }
}
