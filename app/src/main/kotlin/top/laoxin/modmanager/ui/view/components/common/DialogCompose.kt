package top.laoxin.modmanager.ui.view.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton

/**
 * 对话框类型枚举
 */
enum class DialogType {
    INFO,       // 信息通知
    SUCCESS,    // 成功提示
    WARNING,    // 警告
    ERROR       // 错误
}

/**
 * 获取对话框类型的配置信息
 */
private data class DialogTypeConfig(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color
)

@Composable
private fun getDialogTypeConfig(type: DialogType): DialogTypeConfig {
    return when (type) {
        DialogType.INFO -> DialogTypeConfig(
            icon = Icons.Default.Info,
            iconColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
        DialogType.SUCCESS -> DialogTypeConfig(
            icon = Icons.Default.CheckCircle,
            iconColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
        DialogType.WARNING -> DialogTypeConfig(
            icon = Icons.Default.Warning,
            iconColor = Color(0xFFFF9800),
            backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f)
        )
        DialogType.ERROR -> DialogTypeConfig(
            icon = Icons.Default.Error,
            iconColor = Color(0xFFF44336),
            backgroundColor = Color(0xFFF44336).copy(alpha = 0.1f)
        )
    }
}

@Composable
fun DialogCommon(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    showDialog: Boolean = false,
    type: DialogType = DialogType.INFO,
    confirmText: String? = null,
    cancelText: String? = null,
    showCancelButton: Boolean = true
) {
    if (showDialog) {
        val typeConfig = getDialogTypeConfig(type)

        AlertDialog(
            onDismissRequest = { onCancel() },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 图标区域
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = typeConfig.backgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeConfig.icon,
                            contentDescription = null,
                            tint = typeConfig.iconColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 标题
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 内容
                    MarkdownText(
                        markdown = content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        style = TextStyle(
                            fontSize = 16.sp,
                            textAlign = TextAlign.Start,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (showCancelButton) {
                            Arrangement.spacedBy(12.dp)
                        } else {
                            Arrangement.Center
                        }
                    ) {
                        if (showCancelButton) {
                            ExpressiveTextButton(
                                onClick = { onCancel() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = cancelText ?: stringResource(id = R.string.dialog_button_request_close),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        ExpressiveTextButton(
                            onClick = { onConfirm() },
                            modifier = if (showCancelButton) Modifier.weight(1f) else Modifier
                        ) {
                            Text(
                                text = confirmText ?: stringResource(id = R.string.dialog_button_confirm),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}
