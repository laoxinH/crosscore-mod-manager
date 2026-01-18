package top.laoxin.modmanager.ui.view.components.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton

@Composable
fun DialogCommonForUpdate(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean = false
) {
    if (showDialog) {
        // 检查是否为强制更新
        val isForceUpdate = content.contains("强制") ||
                            content.contains("重大") ||
                            content.contains("重要") ||
                            content.contains("forced", ignoreCase = true)

        AlertDialog(
            onDismissRequest = {
                // 非强制更新时允许点击外部关闭
                if (!isForceUpdate) {
                    onDismiss()
                }
            },
            title = { Text(text = title) },
            shape = MaterialTheme.shapes.extraLarge,
            text = {
                MarkdownText(
                    content,
                    Modifier.padding(16.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Justify,
                    ),
                )
            },
            confirmButton = {
                ExpressiveTextButton(onClick = {
                    onConfirm()
                }) {
                    Text(stringResource(id = R.string.download))
                }
            },
            dismissButton = {
                // 只有非强制更新时才显示"暂不更新"按钮
                if (!isForceUpdate) {
                    ExpressiveTextButton(onClick = {
                        onDismiss()
                    }) {
                        Text(stringResource(id = R.string.update_later))
                    }
                }
            }
        )

    }
}
