package top.laoxin.modmanager.constant

import android.os.Environment


object ScanModPath {
    val ROOT_PATH = Environment.getExternalStorageDirectory().path
    val MOD_PATH_QQ = ROOT_PATH + "/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv/"
    val MOD_PATH_DOWNLOAD = ROOT_PATH + "/Download/"
    val ANDROID_DATA = ROOT_PATH + "/Android/data/"
}