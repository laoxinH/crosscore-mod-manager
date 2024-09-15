package top.laoxin.modmanager.ui.view.modview

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


import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.ui.theme.ModManagerTheme
import top.laoxin.modmanager.ui.view.commen.DialogCommon
import top.laoxin.modmanager.ui.view.commen.SelectPermissionDialog
import top.laoxin.modmanager.ui.viewmodel.ModViewModel

//lateinit var viewModel: ModViewModel
enum class NavigationIndex(
    @StringRes val title: Int,
    val index: Int,
) {
    ENABLE_MODS(R.string.mod_page_title_enable_mods, 1),
    DISABLE_MODS(R.string.mod_page_title_disable_mods, 2),
    SEARCH_MODS(R.string.mod_page_title_search_mods, 3),
    ALL_MODS(R.string.mod_page_title_all_mods, 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModPage(viewModel: ModViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box {
        UserTipsDialog(
            showDialog = uiState.showUserTipsDialog,
            setUserTipsDialog = viewModel::setUserTipsDialog
        )
        DisEnableModsDialog(
            showDialog = uiState.showDisEnableModsDialog,
            mods = uiState.delEnableModsList,
            switchMod = { mod, enable -> viewModel.switchMod(mod, enable, true) },
            onConfirmRequest = {
                viewModel.delMods()
            }
        )
        DialogCommon(
            title = stringResource(R.string.open_mod_failed_dialog_title),
            content = stringResource(
                R.string.open_mod_failed_dialog_desc,
                uiState.openFailedMods.size
            ),
            onConfirm = {
                viewModel.setShowOpenFailedDialog(false)
                viewModel.disableMod(uiState.openFailedMods, false)
            },
            onCancel = { viewModel.setShowOpenFailedDialog(false) },
            showDialog = uiState.showOpenFailedDialog
        )

        if (viewModel.requestPermissionPath.isNotEmpty()) {
            SelectPermissionDialog(
                path = viewModel.requestPermissionPath,
                onDismissRequest = { viewModel.setOpenPermissionRequestDialog(false) },
                showDialog = uiState.openPermissionRequestDialog
            )
        }
        if (uiState.isLoading) {
            Loading(uiState.loadingPath)
        } else {
            uiState.modDetail?.let {
                ModDetailPartialBottomSheet(
                    showDialog = uiState.showModDetail,
                    mod = it,
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowModDetail(false) }
                )
                PasswordInputDialog(
                    showDialog = uiState.showPasswordDialog,
                    mod = it,
                    onDismiss = { viewModel.showPasswordDialog(false) },
                    onPasswordSubmit = viewModel::checkPassword
                )
            }
            AllModPage(viewModel, uiState)
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
    mods: List<ModBean>,
    switchMod: (ModBean, Boolean) -> Unit,
    onConfirmRequest: () -> Unit,
) {
    if (showDialog) {
        if (mods.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        stringResource(id = R.string.dialog_dis_enable_mods_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                text = {
                    LazyColumn {
                        itemsIndexed(mods) { _, mod ->
                            ModListItem(
                                mod = mod,
                                modSwitchEnable = true,
                                openModDetail = { _, _ -> },
                                enableMod = switchMod,
                                isMultiSelect = false,
                                onLongClick = { },
                                onMultiSelectClick = { }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
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


// 使用 Glide 加载 Bitmap，同时保持图片比例
suspend fun loadImageBitmapFromPath(
    context: Context,
    path: String,
    reqWidth: Int,
    reqHeight: Int
): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val bitmap: Bitmap = Glide.with(context)
                .asBitmap()
                .load(path)
                .apply(RequestOptions().override(reqWidth, reqHeight))  // 指定最大宽度和高度，保持图片比例
                .submit()
                .get()
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
                if (!it.isFocused && hasFocus) {
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
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        // 点击就清空text
                        modifier = Modifier.clickable {
                            onValueChange.invoke("")
                        })
                }
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primaryContainer,

                    modifier = Modifier
                        .clickable {
                            close()
                        }
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


@Preview("Heroes List")
@Composable
fun HeroesPreview() {
    ModManagerTheme {
        Surface {
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
