package top.laoxin.modmanager.ui.view.commen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import top.laoxin.modmanager.R

@Composable
        /**
         * 更新对话框
         */
fun DialogCommonForUpdate(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean = false
) {
    if (showDialog) {
        AlertDialog(
            // 空的 lambda 函数，表示点击对话框外的区域不会关闭对话框
            onDismissRequest = {},
            title = { Text(text = title) },
            text = { Text(text = content) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                }) {
                    Text(stringResource(id = R.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(stringResource(id = R.string.download_universal))
                }
            }
        )

    }
}