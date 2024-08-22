package top.laoxin.modmanager.observer

import android.os.FileObserver

class FlashModsObserver(path: String) : FileObserver(path, ALL_EVENTS) {
    companion object {
        var flashObserver: FlashObserverInterface? = null
    }

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            CREATE, DELETE, MOVED_FROM, MOVED_TO -> flashObserver?.onFlash()
            // Add more cases if you want to handle more events
            else -> return
        }
    }
}
