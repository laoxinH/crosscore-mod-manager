package top.laoxin.modmanager.observer

import android.os.FileObserver
import android.util.Log
import top.laoxin.modmanager.tools.ArchiveUtil
import java.io.File

class ModPathObserver(file: File)  : FileObserver(file,FileObserver.ALL_EVENTS) {

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            FileObserver.CREATE -> Log.d("FileObserver", "New file $path has been created")
            FileObserver.DELETE -> Log.d("FileObserver", "File $path has been deleted")
            // Add more cases if you want to handle more events
            else -> return
        }
    }

    fun onFileCreate(path: String, file: File) {
        if (File(file.absolutePath, path).isDirectory) return
        val filepath = File(file.absolutePath, path).absolutePath
        if (!ArchiveUtil.isArchiveEncrypted(filepath)) return


    }
}