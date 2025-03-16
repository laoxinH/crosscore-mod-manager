package top.laoxin.modmanager.ui.view.commen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import top.laoxin.modmanager.R

// 通用对话框
@Composable
fun DialogCommon(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    showDialog: Boolean = false
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {}, // 空的 lambda 函数，表示点击对话框外的区域不会关闭对话框
            title = { Text(text = title) },
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
                TextButton(onClick = {
                    onConfirm()
                }) {
                    Text(stringResource(id = R.string.dialog_button_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onCancel()
                }) {
                    Text(stringResource(id = R.string.dialog_button_request_close))
                }
            }
        )

    }
}