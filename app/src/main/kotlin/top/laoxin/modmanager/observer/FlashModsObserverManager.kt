package top.laoxin.modmanager.observer

import android.os.Build
import android.os.FileObserver
import android.util.Log
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashModsObserverManager @Inject constructor(

) {

    lateinit var observer: ModScanViewModel
    private var selectedDictionaryFileObserver: FileObserver? = null
    private var modDictionaryFileObserver: FileObserver? = null

    companion object {
        private const val TAG = "FlashModsObserverManager"
    }

    fun openSelectedDictionaryObserver(selectedDirectory: String) {
        selectedDictionaryFileObserver?.stopWatching()

        selectedDictionaryFileObserver =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FlashModsObserver(PathConstants.ROOT_PATH + selectedDirectory)
            } else {
                FlashModsObserverLow(PathConstants.ROOT_PATH + selectedDirectory)
            }
        selectedDictionaryFileObserver?.startWatching()
    }

    fun openModDictionaryObserver(modDirectory: String) {
        modDictionaryFileObserver?.stopWatching()

        modDictionaryFileObserver =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FlashModsObserver(PathConstants.ROOT_PATH + modDirectory)
            } else {
                FlashModsObserverLow(PathConstants.ROOT_PATH + modDirectory)
            }
        modDictionaryFileObserver?.startWatching()
    }

    fun stopWatching() {
        selectedDictionaryFileObserver?.stopWatching()
        modDictionaryFileObserver?.stopWatching()
    }

    fun startWatching() {
        Log.d(TAG, "startWatching: 开启文件监听")
        selectedDictionaryFileObserver?.startWatching()
        modDictionaryFileObserver?.startWatching()
    }
}