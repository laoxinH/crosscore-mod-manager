package top.laoxin.modmanager.constant

import androidx.annotation.IntDef



interface RequestCode {
    @IntDef(*[STORAGE, DOCUMENT, SHIZUKU])
    annotation class RequestCode
    companion object {
        const val STORAGE = 0
        const val DOCUMENT = 1
        const val SHIZUKU = 2
    }
}

