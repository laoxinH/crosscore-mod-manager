package top.laoxin.modmanager.ui.view.modView

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean

/** 密码输入对话框 用于解密加密的 MOD */
@Composable
fun PasswordInputDialog(
        mod: ModBean?,
        errorMessage: String?,
        onDismiss: () -> Unit,
        onSubmit: (String) -> Unit
) {
    if (mod == null) return

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                        text = stringResource(R.string.decrypt_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                            text = stringResource(R.string.decrypt_dialog_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // MOD 名称
                    Text(
                            text = mod.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 密码输入框
                    OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.decrypt_dialog_password_hint)) },
                            singleLine = true,
                            isError = errorMessage != null,
                            supportingText =
                                    if (errorMessage != null) {
                                        {
                                            Text(
                                                    errorMessage,
                                                    color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else null,
                            visualTransformation =
                                    if (passwordVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                            imageVector =
                                                    if (passwordVisible) {
                                                        Icons.Outlined.VisibilityOff
                                                    } else {
                                                        Icons.Outlined.Visibility
                                                    },
                                            contentDescription =
                                                    if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            keyboardOptions =
                                    KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                    ),
                            keyboardActions =
                                    KeyboardActions(
                                            onDone = {
                                                if (password.isNotBlank()) onSubmit(password)
                                            }
                                    ),
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSubmit(password) }, enabled = password.isNotBlank()) {
                    Text(stringResource(R.string.decrypt_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.decrypt_dialog_cancel))
                }
            }
    )
}
