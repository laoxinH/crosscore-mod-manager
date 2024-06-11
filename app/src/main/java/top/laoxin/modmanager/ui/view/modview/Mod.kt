package top.laoxin.modmanager.ui.view.modview

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import androidx.compose.animation.AnimatedVisibility

/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ImagesearchRoller
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.tools.PermissionTools
import top.laoxin.modmanager.ui.view.SettingItem
import top.laoxin.modmanager.ui.view.commen.SelectPermissionDialog
import top.laoxin.modmanager.ui.viewmodel.ModViewModel

enum class NavigationIndex(
    @StringRes val title: Int,
    val icon: ImageVector
) {
    ALL_MODS(R.string.mod_page_title_all_mods, Icons.Filled.Dashboard),
    ENABLE_MODS(R.string.mod_page_title_enable_mods, Icons.Filled.ImagesearchRoller),
    DISABLE_MODS(R.string.mod_page_title_disable_mods, Icons.Filled.Settings),
    SEARCH_MODS(R.string.mod_page_title_search_mods, Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModPage() {
    // 导航栏
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    // 当前导航索引
    val currentScreen = NavigationIndex.valueOf(
        currentEntry?.destination?.route ?: NavigationIndex.ALL_MODS.name
    )

    val viewModel: ModViewModel = viewModel(
        factory = ModViewModel.Factory
    )
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                title = {
                    Text(
                        stringResource(id = currentScreen.title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = {
                        // 在这里处理图标按钮的点击事件
                        viewModel.setUserTipsDialog(true)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info, // 使用信息图标
                            contentDescription = "Info", // 为辅助功能提供描述
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    IconButton(onClick = {
                        viewModel.setSearchBoxVisible(true)
                        navController.navigate(NavigationIndex.SEARCH_MODS.name) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }

                        // 请求焦点
                    }, modifier = Modifier) {
                        Icon(
                            imageVector = Icons.Filled.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    IconButton(onClick = {
                        viewModel.flashMods(false)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh, // 使用刷新图标
                            contentDescription = "Refresh", // 为辅助功能提供描述
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    IconButton(onClick = { showMenu = true }, modifier = Modifier) {
                        Icon(
                            imageVector = Icons.Filled.Menu, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_enable_mods)) },
                            onClick = {
                                navController.navigate(NavigationIndex.ENABLE_MODS.name) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }

                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_disable_mods)) },
                            onClick = {
                                navController.navigate(NavigationIndex.DISABLE_MODS.name) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_all_mods)) },
                            onClick = {
                                navController.navigate(NavigationIndex.ALL_MODS.name) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                        // 添加更多的菜单项
                    }
                }
            )
            AnimatedVisibility(visible = uiState.searchBoxVisible) {

                // 根据 MutableState 显示或隐藏搜索框

                CustomEdit(
                    text = viewModel.getSearchText(),
                    onValueChange = {
                        viewModel.setSearchText(it)
                    },
                    hint = stringResource(R.string.mod_page_search_hit),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 10.dp)
                        .height(50.dp)
                        .background(Color(0xBCE9E9E9), shape = MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp),
                    textStyle = typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    close = {
                        viewModel.setSearchBoxVisible(false)
                    }

                )
            }
        },
    ) { innerPadding ->
        Box(/*modifier = if (uiState.showTips) Modifier.padding(innerPadding) else Modifier.fillMaxSize()*/) {

            UserTipsDialog(
                showDialog = uiState.showUserTipsDialog,
                setUserTipsDialog = viewModel::setUserTipsDialog
            )
            DisEnableModsDialog(
                showDialog = uiState.showDisEnableModsDialog,
                mods = uiState.delEnableModsList,
                switchMod = { mod, enable -> viewModel.switchMod(mod, enable,true) },
                onConfirmRequest = {
                    viewModel.delMods()
                }
            )

            if (viewModel.requestPermissionPath.isNotEmpty()) {
                SelectPermissionDialog(path = viewModel.requestPermissionPath, onDismissRequest = { viewModel.setOpenPermissionRequestDialog(false) }, showDialog = uiState.openPermissionRequestDialog)
            }
            if (uiState.isLoading) {
                Loading(uiState.loadingPath)
            } else {
                uiState.modDetail?.let {
                    ModDetailDialog(
                        showDialog = uiState.showModDetail,
                        mod = it,
                        onDismiss = { viewModel.setShowModDetail(false) }
                    )
                    PasswordInputDialog(
                        showDialog = uiState.showPasswordDialog,
                        mod = it,
                        onDismiss = { viewModel.showPasswordDialog(false) },
                        onPasswordSubmit = viewModel::checkPassword
                    )
                }
                NavHost(
                    navController = navController,
                    startDestination = NavigationIndex.ALL_MODS.name,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(route = NavigationIndex.ALL_MODS.name) {
                        Box {
                            AllModPage(viewModel, uiState)
                            Tips(
                                text = uiState.tipsText,
                                showTips = uiState.showTips,
                                onDismiss = { viewModel.setShowTips(false) },
                                )
                        }

                    }

                    composable(route = NavigationIndex.ENABLE_MODS.name) {
                        Box {
                            EnableModPage(viewModel, uiState)
                            Tips(
                                text = uiState.tipsText,
                                showTips = uiState.showTips,
                                onDismiss = { viewModel.setShowTips(false) },

                                )
                        }

                    }
                    composable(route = NavigationIndex.DISABLE_MODS.name) {
                        Box {
                            DisableModPage(viewModel, uiState)
                            Tips(
                                text = uiState.tipsText,
                                showTips = uiState.showTips,
                                onDismiss = { viewModel.setShowTips(false) },

                                )
                        }

                    }
                    composable(route = NavigationIndex.SEARCH_MODS.name) {
                        Box {
                            SearchModPage(viewModel, uiState)
                            Tips(
                                text = uiState.tipsText,
                                showTips = uiState.showTips,
                                onDismiss = { viewModel.setShowTips(false) },

                                )
                        }
                    }
                }


            }
        }
    }


    /*    StoragePermissionDialog(
            showDialog = uiState.openPermissionRequestDialog,
            viewModel = viewModel
        ) { viewModel.setOpenPermissionRequestDialog(false) }*/


}


@Composable
fun UserTipsDialog(
    showDialog: Boolean,
    setUserTipsDialog: (Boolean) -> Unit
) {

    if (showDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = {

            }, // 空的 lambda 函数，表示点击对话框外的区域不会关闭对话框
            title = { Text(stringResource(id = R.string.dialog_info_title)) },
            text = { Text(stringResource(id = R.string.dialog_info_message)) },
            confirmButton = {
                TextButton(onClick = {
                    setUserTipsDialog(false)
                }) {
                    Text(stringResource(id = R.string.dialog_button_info_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (context is Activity) {
                        context.finish()
                    }
                }) {
                    Text(stringResource(id = R.string.dialog_button_request_close))
                }
            }
        )
    }
}

// 关闭mods提示框
@Composable
fun DisEnableModsDialog(
    showDialog: Boolean,
    mods : List<ModBean>,
    switchMod: (ModBean,Boolean) -> Unit,
    onConfirmRequest: () -> Unit,
) {
    if (showDialog) {
        if (mods.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(id = R.string.dialog_dis_enable_mods_title)) },
                text = {
                    LazyColumn {
                        itemsIndexed(mods) { _, mod ->
                            ModListItem(mod = mod, modSwitchEnable = true, showDialog = { _, _ ->}, enableMod = switchMod)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onConfirmRequest()
                    }) {
                        Text(stringResource(id = R.string.dialog_button_confirm))
                    }
                },
                dismissButton = {

                }
            )
        } else {
            onConfirmRequest()
        }
    }
}


// 将ByteArray转化为ImageBitmap
fun createImageBitmapFromPath(path: String): ImageBitmap? {
    val bitmap = BitmapFactory.decodeFile(path)
    return bitmap?.asImageBitmap()
}


fun onStoragePermissionResult(granted: Boolean, context: Context) {
    if (granted) {
        PermissionTools.checkShizukuPermission()
    } else {

    }
}

/**
 * @param hint: 空字符时的提示
 * @param startIcon: 左侧图标;  -1 则不显示
 * @param iconSpacing: 左侧图标与文字的距离; 相当于: drawablePadding
 */
@Composable
fun CustomEdit(
    text: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    hint: String = "请输入",
    @DrawableRes startIcon: Int = -1,
    iconSpacing: Dp = 6.dp,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    close: () -> Unit
) {
    // 焦点, 用于控制是否显示 右侧叉号
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                Log.d("CustomEdit", "CustomEdit:改变后焦点${it.isFocused}, 该斌前:$hasFocus ")
                if (!it.isFocused && hasFocus) {
                    Log.d("CustomEdit", "CustomEdit:失去焦点 ")
                    // 组件失去焦点
                    close()
                }
                hasFocus = it.isFocused
            },
        singleLine = true,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { close() }),
        visualTransformation = visualTransformation,
        cursorBrush = cursorBrush,
        decorationBox = @Composable { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -1 不显示 左侧Icon
                if (startIcon != -1) {
                    Image(painter = painterResource(id = startIcon), contentDescription = null)
                    Spacer(modifier = Modifier.width(iconSpacing))
                }

                Box(modifier = Modifier.weight(1f)) {
                    // 当空字符时, 显示hint
                    if (text.isEmpty())
                        Text(text = hint, color = Color.Gray, style = textStyle)

                    // 原本输入框的内容
                    innerTextField()
                }

                // 存在焦点 且 有输入内容时. 显示叉号
                if (hasFocus && text.isNotEmpty()) {
                    Log.d("CustomEdit", "CustomEdit:失去焦点 ")
                    Icon(imageVector = Icons.Filled.Clear, // 清除图标
                        contentDescription = null,
                        // 点击就清空text
                        modifier = Modifier.clickable { onValueChange.invoke("") })
                }
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 10.dp)
                )
            }
        }
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}


// 创建一个占据全屏的居中文本, 提示没有mod
@Composable
fun NoMod() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.mod_page_no_mod),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// 创建一个全屏的加载动画
@Composable
fun Loading(
    loadingPath: String = "loading"
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.mod_pag_loading, loadingPath),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
    Box {

    }
}

@Composable
fun Tips(

    @StringRes text: Int,
    showTips: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showTips) {
        Snackbar(
            action = {
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.tips_btn_close))
                }
            },
            containerColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(8.dp)
        ) { Text(text = stringResource(id = text)) }
    }
}

data class Hero(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val imageRes: Int
)

@Preview("Heroes List")
@Composable
fun HeroesPreview() {
    ModManagerTheme() {
        Surface(
            //color = MaterialTheme.colorScheme.background
        ) {
            Snackbar(
                action = {
                    Button(onClick = {}) {
                        Text("升级App")
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(8.dp)
            ) { Text(text = "检测到有新版本") }
        }
    }
}
