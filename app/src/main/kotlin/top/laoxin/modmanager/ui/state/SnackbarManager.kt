package top.laoxin.modmanager.ui.state

import androidx.annotation.StringRes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** 全局 Snackbar 消息管理器 用于在应用的任何位置发送 Snackbar 消息 由 Hilt 管理为单例，可在 ViewModel 中注入使用 */
@Singleton
class SnackbarManager @Inject constructor() {

    private val _messages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** 发送文本消息 (suspend 版本) */
    suspend fun showMessage(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _messages.send(SnackbarMessage.Text(message, duration))
    }

    /** 发送资源 ID 消息 (suspend 版本) */
    suspend fun showMessage(
            @StringRes resId: Int,
            duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _messages.send(SnackbarMessage.Resource(resId, duration))
    }

    /** 发送带格式化参数的资源 ID 消息 (suspend 版本) */
    suspend fun showMessage(
            @StringRes resId: Int,
            vararg formatArgs: Any,
            duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _messages.send(SnackbarMessage.ResourceWithArgs(resId, formatArgs.toList(), duration))
    }

    /** 发送带操作按钮的消息 (suspend 版本) */
    suspend fun showMessageWithAction(
            message: String,
            actionLabel: String,
            duration: SnackbarDuration = SnackbarDuration.Long,
            onAction: () -> Unit
    ) {
        _messages.send(SnackbarMessage.WithAction(message, actionLabel, duration, onAction))
    }

    /** 发送文本消息 (非挂起版本，用于 ViewModel 或非协程上下文) */
    fun showMessageAsync(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _messages.trySend(SnackbarMessage.Text(message, duration))
    }

    /** 发送资源 ID 消息 (非挂起版本) */
    fun showMessageAsync(
            @StringRes resId: Int,
            duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _messages.trySend(SnackbarMessage.Resource(resId, duration))
    }

    /** 发送带格式化参数的资源 ID 消息 (非挂起版本) */
    fun showMessageAsync(
            @StringRes resId: Int,
            vararg formatArgs: Any,
            duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _messages.trySend(SnackbarMessage.ResourceWithArgs(resId, formatArgs.toList(), duration))
    }
}

/** Snackbar 消息类型 */
sealed class SnackbarMessage {
    abstract val duration: SnackbarDuration

    data class Text(
            val message: String,
            override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarMessage()

    data class Resource(
            val resId: Int,
            override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarMessage()

    data class ResourceWithArgs(

            val resId: Int,
            val formatArgs: List<Any>,
            override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarMessage()

    data class WithAction(
            val message: String,
            val actionLabel: String,
            override val duration: SnackbarDuration = SnackbarDuration.Long,
            val onAction: () -> Unit
    ) : SnackbarMessage()
}

/** Snackbar 显示时长 */
enum class SnackbarDuration {
    Short,
    Long,
    Indefinite
}
