package top.laoxin.modmanager.ui.view.modview

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.ModBean

@Composable
fun ModList(
    modifier: Modifier = Modifier,
    mods: List<ModBean>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modSwitchEnable : Boolean,
    showDialog: (ModBean, Boolean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit
) {
    LazyColumn(
        // state = state,
        //columns = GridCells.Adaptive(minSize = 256.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Log.d("ModList", "ModList: ${mods}")
        itemsIndexed(mods) { index, mod ->

            ModListItem(
                mod = mod,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                modSwitchEnable = modSwitchEnable,
                showDialog = showDialog,
                enableMod = enableMod
            )
        }
    }
}

@Composable
fun ModListItem(
    mod: ModBean,
    modifier: Modifier = Modifier,
    modSwitchEnable : Boolean,
    showDialog: (ModBean, Boolean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,

) {
    val coroutineScope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val path = mod.icon
    LaunchedEffect(path) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(path)
                val composeImageBitmap = bitmap?.asImageBitmap()
                withContext(Dispatchers.Main) {
                    imageBitmap = composeImageBitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { showDialog(mod, true) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .sizeIn(minHeight = 30.dp)
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
                        contentScale = ContentScale.FillWidth
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
            val description = mod.description
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
                if (description != null) {
                    Text(
                        text = (/*if (description.length > 10) description.substring(0, 10) + "..." else*/ description),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            //Spacer(Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)

            ) {
                Switch(checked = mod.isEnable, onCheckedChange = { enableMod(mod, it) }, enabled = modSwitchEnable)
            }
        }
    }
}


@Composable
fun ModDetailDialog(
    showDialog: Boolean,
    mod: ModBean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss, // 点击对话框外的区域时关闭对话框
            title = { Text(text = mod.name ?: "", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    // 文本标签
                    AssistChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.mod_page_mod_detail_dialog_detali_title)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Localized description",
                                Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                    Card {
                        Column(Modifier.padding(16.dp)) {


                            Text(
                                text = stringResource(
                                    R.string.mod_page_mod_detail_dialog_detali_descript,
                                    mod.description ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(
                                    R.string.mod_page_mod_detail_dialog_detali_version,
                                    mod.version ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(
                                    R.string.mod_page_mod_detail_dialog_detali_path,
                                    mod.path ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(
                                    R.string.mod_page_mod_detail_dialog_detali_modType,
                                    mod.modType ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(
                                    R.string.mod_page_mod_detail_dialog_detali_author,
                                    mod.author ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    // 垂直分割
                    Spacer(modifier = Modifier.height(8.dp))
                    // 预览图
                    if ((!mod.images.isNullOrEmpty() || !mod.isEncrypted) || (mod.password != null && !mod.images.isNullOrEmpty())) {
                        AssistChip(
                            onClick = { },
                            label = { Text(stringResource(R.string.mod_page_mod_detail_dialog_detali_perview)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = "Localized description",
                                    Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )

                        Column(Modifier.padding(16.dp)) {
                            val imageBitmaps = mod.images?.mapNotNull { path ->
                                createImageBitmapFromPath(path)
                            }
                            ImagePager(images = imageBitmaps!!)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(text = stringResource(R.string.mod_page_mod_detail_dialog_close))
                }
            }
        )
    }
}

// 输入密码的弹窗
@Composable
fun PasswordInputDialog(
    showDialog: Boolean,
    mod : ModBean,
    onDismiss: () -> Unit,
    onPasswordSubmit: (String) -> Unit
) {
    if (showDialog) {
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.password_dialog_title),
                style = MaterialTheme.typography.titleLarge)
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
                    Text(text = stringResource(id =R.string.dialog_button_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id =R.string.dialog_button_request_close))
                }
            }
        )
    }
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePager(images: List<ImageBitmap>) {
    val pagerState = rememberPagerState(pageCount = { images.size })
    HorizontalPager(state = pagerState) { page ->
        Image(
            bitmap = images[page],
            contentDescription = "预览图",
            modifier = Modifier.clip(RoundedCornerShape(8.dp)) // 设置图片的圆角
        )
    }


}