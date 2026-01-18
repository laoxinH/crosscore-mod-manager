package top.laoxin.modmanager.ui.view.components.common

import androidx.compose.material3.SnackbarDuration as M3SnackbarDuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.state.SnackbarDuration
import top.laoxin.modmanager.ui.state.SnackbarManager
import top.laoxin.modmanager.ui.state.SnackbarMessage

/** CompositionLocal 用于在 Compose 树中访问 SnackbarManager 主要用于需要在 Composable 函数中直接发送消息的场景 */
val LocalSnackbarManager =
        compositionLocalOf<SnackbarManager> {
            error(
                    "SnackbarManager not provided. Make sure to wrap your content with ProvideSnackbarHost."
            )
        }

/**
 * 全局 Snackbar 宿主组件 监听 SnackbarManager 的消息流并显示 Snackbar
 * 代码中的警告是合理现象，无需处理
 * @param snackbarManager 全局消息管理器
 * @param snackbarHostState Snackbar 宿主状态
 */

@Composable
fun GlobalSnackbarHost(
        snackbarManager: SnackbarManager,
        snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current

    // 监听消息流
    LaunchedEffect(snackbarManager, snackbarHostState) {
        snackbarManager.messages.collect { message ->
            // 关闭之前的 Snackbar
            snackbarHostState.currentSnackbarData?.dismiss()

            // 这里不能使用 @Composable 的 stringResource 函数，因为它不在 Composable 上下文中。
            // 应该使用 context.getString()。
            val text =
                    when (message) {
                        is SnackbarMessage.Text -> message.message

                        is SnackbarMessage.Resource ->  context.getString(message.resId)
                        is SnackbarMessage.ResourceWithArgs ->
                                context.getString(message.resId, *message.formatArgs.toTypedArray())
                        is SnackbarMessage.WithAction -> message.message
                    }

            val actionLabel =
                    when (message) {
                        is SnackbarMessage.WithAction -> message.actionLabel
                        else -> null
                    }

            val duration = message.duration.toM3Duration()

            val result =
                    snackbarHostState.showSnackbar(
                            message = text,
                            actionLabel = actionLabel,
                            duration = duration
                    )

            // 处理操作按钮点击
            if (result == SnackbarResult.ActionPerformed && message is SnackbarMessage.WithAction) {
                message.onAction()
            }
        }
    }

    // 自定义 SnackbarHost，使用 zIndex 确保在顶层显示
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.zIndex(Float.MAX_VALUE),
        snackbar = { snackbarData ->
            Snackbar(
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionContentColor = MaterialTheme.colorScheme.inversePrimary,
                action = snackbarData.visuals.actionLabel?.let { actionLabel ->
                    {
                        TextButton(onClick = { snackbarData.performAction() }) {
                            Text(
                                text = actionLabel,
                                color = MaterialTheme.colorScheme.inversePrimary
                            )
                        }
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App 图标
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // 消息文本
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )
}

/** 将自定义 SnackbarDuration 转换为 Material3 的 SnackbarDuration */
private fun SnackbarDuration.toM3Duration(): M3SnackbarDuration {
    return when (this) {
        SnackbarDuration.Short -> M3SnackbarDuration.Short
        SnackbarDuration.Long -> M3SnackbarDuration.Long
        SnackbarDuration.Indefinite -> M3SnackbarDuration.Indefinite
    }
}
