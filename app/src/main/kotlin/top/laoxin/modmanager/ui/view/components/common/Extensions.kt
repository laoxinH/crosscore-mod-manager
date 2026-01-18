package top.laoxin.modmanager.ui.view.components.common

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** 扩展函数集合 提供常用的 UI 操作辅助函数 */

/**
 * 在浏览器中打开 URL
 * @param url 要打开的链接
 */
fun Context.openUrl(url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    startActivity(intent)
}

/**
 * 在应用商店中打开应用页面
 * @param packageName 应用包名，默认为当前应用
 */
fun Context.openAppInStore(packageName: String = this.packageName) {
    try {
        // 尝试打开 Google Play
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
    } catch (e: Exception) {
        // 回退到网页版
        startActivity(
                Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
        )
    }
}

/**
 * 分享文本内容
 * @param text 要分享的文本
 * @param title 分享对话框标题
 */
fun Context.shareText(text: String, title: String = "") {
    val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
    startActivity(Intent.createChooser(intent, title.ifEmpty { null }))
}

/**
 * 发送邮件
 * @param email 收件人邮箱
 * @param subject 邮件主题
 * @param body 邮件正文
 */
fun Context.sendEmail(email: String, subject: String = "", body: String = "") {
    val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:$email".toUri()
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
    startActivity(intent)
}



fun String.fuzzyContains(pattern: String): Boolean {
    val p = pattern.trim().lowercase()
    if (p.isEmpty()) return true
    var i = 0
    for (c in this.lowercase()) {
        if (i < p.length && c == p[i]) i++
    }
    return i == p.length
}
