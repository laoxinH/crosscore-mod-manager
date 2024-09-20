package top.laoxin.modmanager.ui.view.modview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.view.commen.DialogCommon
import top.laoxin.modmanager.ui.viewmodel.ModViewModel


@Composable
fun Tips(
    text: String, showTips: Boolean, onDismiss: () -> Unit, uiState: ModUiState
) {
    if (showTips) {
        val tipsStart = if (uiState.unzipProgress.isNotEmpty()) {
            "$text : ${uiState.unzipProgress}"
        } else {
            text
        }
        val tipsEnd = if (uiState.multitaskingProgress.isNotEmpty()) {
            "总进度 : ${uiState.multitaskingProgress}"
        } else {
            ""
        }
        Snackbar(
            action = {
                TextButton(onClick = { onDismiss() }) {

                    Text(
                        stringResource(R.string.tips_btn_close),
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                }
            },
            containerColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(8.dp)

        ) {
            Text(text = "$tipsStart $tipsEnd", style = MaterialTheme.typography.bodyMedium)

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModTopBar(viewModel: ModViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.isMultiSelect) {
        DialogCommon(
            title = stringResource(R.string.dialog_del_selected_mods_title),
            content = stringResource(R.string.dialog_del_selected_mods_content),
            onConfirm = { viewModel.delSelectedMods() },
            onCancel = { viewModel.setShowDelSelectModsDialog(false) },
            showDialog = uiState.showDelSelectModsDialog
        )
        MultiSelectTopBar(viewModel, uiState)
    } else {
        GeneralTopBar(viewModel, uiState)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(viewModel: ModViewModel, uiState: ModUiState) {
    val modList = when (uiState.modsView) {
        NavigationIndex.ALL_MODS -> uiState.modList
        NavigationIndex.ENABLE_MODS -> uiState.enableModList
        NavigationIndex.DISABLE_MODS -> uiState.disableModList
        NavigationIndex.SEARCH_MODS -> uiState.searchModList
    }
    Column {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ), title = {
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    stringResource(id = uiState.modsView.title),
                    style = MaterialTheme.typography.titleLarge
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Row(
                        modifier = Modifier.padding(top = 40.dp),
                    ) {

                        /* if (uiState.modsSelected.isNotEmpty()){
                         Text(
                             text = "已选：${uiState.modsSelected.size}",
                             style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onPrimary,
                             textAlign = TextAlign.Start,
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                     }*/
                        val total =
                            if (uiState.modsSelected.isNotEmpty()) "${uiState.modsSelected.size}/${modList.size}" else "${modList.size}"

                        Text(
                            text = "统计：$total",
                            style = MaterialTheme.typography.labelMedium,
                            // color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Start,
                        )
                    }

                }
            }

        }, actions = {
            // 全选
            IconButton(onClick = {
                // 在这里处理图标按钮的点击事件
                viewModel.allSelect(modList)
            }) {
                Icon(
                    imageVector = Icons.Default.SelectAll, // 使用信息图标
                    contentDescription = "Info", // 为辅助功能提供描述
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            // 取消选择
            IconButton(onClick = {
                // 在这里处理图标按钮的点击事件
                viewModel.deselect()
            }) {
                Icon(
                    imageVector = Icons.Default.Deselect, // 使用信息图标
                    contentDescription = "Info", // 为辅助功能提供描述
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = {
                // 在这里处理图标按钮的点击事件
                viewModel.switchSelectMod(modList, true)
            }) {
                Icon(
                    imageVector = Icons.Default.FlashOn, // 使用信息图标
                    contentDescription = "Info", // 为辅助功能提供描述
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = {
                viewModel.switchSelectMod(modList, false)

                // 请求焦点
            }, modifier = Modifier) {
                Icon(
                    imageVector = Icons.Filled.FlashOff, contentDescription = null,
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = {
                viewModel.delSelectedMods()
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete, // 使用刷新图标
                    contentDescription = "Refresh", // 为辅助功能提供描述
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = { viewModel.exitSelect() }, modifier = Modifier) {
                Icon(
                    imageVector = Icons.Filled.Close, contentDescription = null,
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
        })

        // 添加 Spacer 以分离 SearchBox 和 TopAppBar
        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = uiState.searchBoxVisible) {

            // 根据 MutableState 显示或隐藏搜索框
            SearchBox(text = viewModel.getSearchText(),
                onValueChange = { viewModel.setSearchText(it) },
                hint = stringResource(R.string.mod_page_search_hit),
                onClose = { viewModel.setSearchBoxVisible(false) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralTopBar(viewModel: ModViewModel, uiState: ModUiState) {

    var showMenu by remember { mutableStateOf(false) }

    Column {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ), title = {
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    stringResource(id = uiState.modsView.title),
                    style = MaterialTheme.typography.titleLarge
                )
                Box {
                    Row(
                        modifier = Modifier.padding(top = 40.dp),
                    ) {
                        val modList = when (uiState.modsView) {
                            NavigationIndex.ALL_MODS -> uiState.modList
                            NavigationIndex.ENABLE_MODS -> uiState.enableModList
                            NavigationIndex.DISABLE_MODS -> uiState.disableModList
                            NavigationIndex.SEARCH_MODS -> uiState.searchModList
                        }
                        val total =
                            if (uiState.modsSelected.isNotEmpty()) "${uiState.modsSelected.size}/${modList.size}" else "${modList.size}"

                        Text(
                            text = "统计：$total",
                            style = MaterialTheme.typography.labelMedium,
                            //color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Start,
                        )
                    }

                }
            }

        }, actions = {

            IconButton(onClick = {
                // 在这里处理图标按钮的点击事件
                viewModel.setUserTipsDialog(true)
            }) {
                Icon(
                    imageVector = Icons.Default.Info, // 使用信息图标
                    contentDescription = "Info", // 为辅助功能提供描述
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = {
                viewModel.setSearchBoxVisible(true)
                viewModel.setModsView(NavigationIndex.SEARCH_MODS)
                // 请求焦点
            }, modifier = Modifier) {
                Icon(
                    imageVector = Icons.Filled.Search, contentDescription = null,
                    // tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = {
                viewModel.flashMods(false, true)
            }) {
                Icon(
                    imageVector = Icons.Filled.Refresh, // 使用刷新图标
                    contentDescription = "Refresh", // 为辅助功能提供描述
                    //tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            IconButton(onClick = { showMenu = true }, modifier = Modifier) {
                Icon(
                    imageVector = Icons.Filled.Menu, contentDescription = null,
                    // tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_enable_mods)) },
                    onClick = {
                        viewModel.setModsView(NavigationIndex.ENABLE_MODS)

                    })
                DropdownMenuItem(text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_disable_mods)) },
                    onClick = {
                        viewModel.setModsView(NavigationIndex.DISABLE_MODS)
                    })
                DropdownMenuItem(text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_all_mods)) },
                    onClick = {
                        viewModel.setModsView(NavigationIndex.ALL_MODS)
                    })
                // 添加更多的菜单项
            }
        })

        // 添加 Spacer 以分离 SearchBox 和 TopAppBar
        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = uiState.searchBoxVisible) {

            // 根据 MutableState 显示或隐藏搜索框
            SearchBox(text = viewModel.getSearchText(),
                onValueChange = { viewModel.setSearchText(it) },
                hint = stringResource(R.string.mod_page_search_hit),
                onClose = { viewModel.setSearchBoxVisible(false) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBox(
    text: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = text,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // 设置透明度
                )
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .height(50.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        // 使用动态取色
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    )
}