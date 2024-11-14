package top.laoxin.modmanager.tools

import android.widget.Toast
import androidx.annotation.StringRes
import top.laoxin.modmanager.App


object ToastUtils {
    private var sToast: Toast? = null
    fun shortCall(@StringRes resId: Int) {
        shortCall(App.get().getString(resId))
    }

    private fun shortCall(text: String?) {
        cancelToast()
        sToast = Toast.makeText(App.get(), text, Toast.LENGTH_SHORT)
        sToast!!.show()
    }

    fun longCall(@StringRes resId: Int) {
        longCall(App.get().getString(resId))
    }

    fun longCall(text: String?) {
        cancelToast()
        sToast = Toast.makeText(App.get(), text, Toast.LENGTH_LONG)
        sToast!!.show()
    }

    private fun cancelToast() {
        if (sToast != null) {
            sToast!!.cancel()
        }
    }
}

