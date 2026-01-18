package top.laoxin.modmanager.ui.view.components.console

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.viewmodel.ConsoleViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ConsoleTopBar(viewModel: ConsoleViewModel, modifier: Modifier = Modifier, configuration: Int) {
    TopAppBar(
            colors =
                    TopAppBarDefaults.topAppBarColors(
                            containerColor =
                                    if (configuration == Configuration.ORIENTATION_LANDSCAPE)
                                            MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceContainer,
                            // titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            // navigationIconContentColor =
                            // MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
            modifier = modifier,
            title = {
                if (configuration != Configuration.ORIENTATION_LANDSCAPE) {
                    Text(stringResource(id = R.string.console), style = MaterialTheme.typography.titleLarge)
                }
            },
            actions = {
                Text(text = stringResource(R.string.console_top_bar_start_game))
                IconButton(
                        onClick = {
                            // 在这里处理图标按钮的点击事件
                            viewModel.startGame()
                        }
                ) {
                    // Text(text = "启动游戏")
                    Icon(
                            imageVector = Icons.Default.PlayArrow, // 使用信息图标
                            contentDescription = "Start", // 为辅助功能提供描述
                            // tint = MaterialTheme.colorScheme.primaryContainer
                            )
                }

                // 添加更多的菜单项
            }
    )
}
