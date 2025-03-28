package top.laoxin.modmanager.ui.view.modView

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import java.io.File

@Composable
fun ModList(
    mods: List<ModBean>,
    modsSelected: List<Int>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modSwitchEnable: Boolean,
    isMultiSelect: Boolean,
    showDialog: (ModBean, Boolean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,
    // 长按
    onLongClick: (ModBean) -> Unit,  // 长按
    // 多选点击
    onMultiSelectClick: (ModBean) -> Unit, // 多选状态下的点击
    modViewModel: ModViewModel
) {
    LazyColumn(
        // state = state,
        //columns = GridCells.Adaptive(minSize = 256.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Log.d("ModList", "ModList: $mods")
        itemsIndexed(mods) { index, mod ->

            ModListItem(
                mod = mod,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                modSwitchEnable = modSwitchEnable,
                openModDetail = showDialog,
                enableMod = enableMod,
                isSelected = modsSelected.contains(mod.id),
                onLongClick = onLongClick,
                onMultiSelectClick = onMultiSelectClick,
                isMultiSelect = isMultiSelect,
                modViewModel = modViewModel
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModListItem(
    modifier: Modifier = Modifier,
    mod: ModBean,
    isSelected: Boolean = false,
    // 长按回调
    onLongClick: (ModBean) -> Unit,  // 长按
    onMultiSelectClick: (ModBean) -> Unit, // 多选状态下的点击
    isMultiSelect: Boolean = false, // 是否多选状态
    modSwitchEnable: Boolean,
    openModDetail: (ModBean, Boolean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,
    modViewModel: ModViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(mod.icon) {
        coroutineScope.launch(Dispatchers.IO) {
            mod.icon?.let {
                if (File(mod.icon).exists()) {
                    imageBitmap = loadImageBitmapFromPath(context, mod.icon, 256, 256)
                } else {
                    Log.e("ModList", "ICON文件不存在: ${mod.icon}")
                    logRecord("ICON文件不存在: ${mod.icon}")
                    if (mod.icon.substringAfterLast('/') == "null") {
                        Log.e("ModDetail", "ICON信息异常: ${mod.icon}")
                        logRecord("ICON信息异常: ${mod.icon}")
                        modViewModel.setModDetail(mod.copy(icon = null))
                        modViewModel.updateMod(mod.copy(icon = null))
                    } else {
                        modViewModel.flashModImage(mod)
                        imageBitmap = loadImageBitmapFromPath(context, mod.icon, 256, 256)

                        Log.i("ModList", "已重新解压ICON: ${mod.icon}")
                        logRecord("已重新解压ICON: ${mod.icon}")
                    }
                }
            }
        }
    }

    Card(
        elevation = if (isSelected) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(
            0.dp
        ),
        modifier = modifier.combinedClickable(
            onClick = {
                if (isMultiSelect) {
                    onMultiSelectClick(mod)
                } else {
                    openModDetail(mod, true)
                }
            },
            onLongClick = {
                onLongClick(mod)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (!isSelected) CardDefaults.cardColors().containerColor else MaterialTheme.colorScheme.secondaryContainer,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .sizeIn(minHeight = 30.dp, /*maxHeight = 80.dp*/)


        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterVertically)


            ) {
                if (imageBitmap != null) {
                    Image(
                        imageBitmap!!, // 从路径获取图片
                        contentDescription = null,
                        alignment = Alignment.TopCenter,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painterResource(id = R.drawable.app_icon),
                        contentDescription = null,
                        alignment = Alignment.TopCenter,
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            val name = mod.name
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {

                if (name != null) {
                    Text(
                        text = /*if (name.length > 10) name.substring(0, 10) + "..." else*/ name,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(Modifier.height(8.dp))
                if (mod.description != null) {
                    Text(
                        text = (stringResource(R.string.mod_modlist_item_desrcipt)),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = (stringResource(R.string.mod_bean_no_readme)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            //Spacer(Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)

            ) {
                Switch(
                    checked = mod.isEnable,
                    onCheckedChange = { enableMod(mod, it) },
                    enabled = modSwitchEnable
                )
            }
        }
    }
}

// 输入密码的弹窗
@Composable
fun PasswordInputDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPasswordSubmit: (String) -> Unit
) {
    if (showDialog) {
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.password_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },

            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_dialog_label)) },
                    //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onPasswordSubmit(password)
                    onDismiss()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.dialog_button_request_close))
                }
            }
        )
    }
}
