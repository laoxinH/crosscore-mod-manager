package top.laoxin.modmanager.tools

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import top.laoxin.modmanager.App

object ToastUtils {
    private var sToast: Toast? = null
    private val handler = Handler(Looper.getMainLooper())
    fun shortCall(@StringRes resId: Int) {
        shortCall(App.get().getString(resId))
    }

    private fun shortCall(text: String?) {
        handler.post {
            cancelToast()
            sToast = Toast.makeText(App.get(), text, Toast.LENGTH_SHORT)
            sToast!!.show()
        }

    }

    fun longCall(@StringRes resId: Int) {
        longCall(App.get().getString(resId))
    }

    fun longCall(text: String?) {
        handler.post {
            cancelToast()
            sToast = Toast.makeText(App.get(), text, Toast.LENGTH_LONG)
            sToast!!.show()
        }
    }

    private fun cancelToast() {
        if (sToast != null) {
            sToast!!.cancel()
        }
    }
}

