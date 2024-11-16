package top.laoxin.modmanager.observer

import android.os.Build
import android.os.FileObserver
import androidx.annotation.RequiresApi
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
class FlashModsObserver(val path: String) : FileObserver(File(path), ALL_EVENTS) {
    companion object {
        var flashObserver: FlashObserverInterface? = null
    }

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            CREATE, DELETE, MOVED_FROM, MOVED_TO -> {
                flashObserver?.onFlash()
            }

            else -> return
        }
    }
}

// 安卓10以下
@Suppress("DEPRECATION")
class FlashModsObserverLow(val path: String) : FileObserver(path, ALL_EVENTS) {
    companion object {
        var flashObserver: FlashObserverInterface? = null
    }

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            CREATE, DELETE, MOVED_FROM, MOVED_TO -> {
                flashObserver?.onFlash()
            }

            else -> return
        }
    }
}

