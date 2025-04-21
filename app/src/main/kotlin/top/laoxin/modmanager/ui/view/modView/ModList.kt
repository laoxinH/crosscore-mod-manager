package top.laoxin.modmanager.ui.view.modView

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.bean.ModBean
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
    onLongClick: (ModBean) -> Unit,
    onMultiSelectClick: (ModBean) -> Unit,
    modViewModel: ModViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        itemsIndexed(mods, key = { _, mod -> mod.id }) { _, mod -> // 添加唯一 key
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
    onLongClick: (ModBean) -> Unit,
    onMultiSelectClick: (ModBean) -> Unit,
    isMultiSelect: Boolean = false,
    modSwitchEnable: Boolean,
    openModDetail: (ModBean, Boolean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,
    modViewModel: ModViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 使用remember记住图像状态
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

    // 使用derivedStateOf计算图标路径是否有效
    val iconPath by remember(mod.icon) {
        derivedStateOf {
            mod.icon?.takeIf { File(it).exists() }
        }
    }

    // 只在初始组合或图标路径变化时加载图像
    LaunchedEffect(iconPath) {
        coroutineScope.launch(Dispatchers.IO) {
            iconPath?.let {
                // 仅当图像为空或路径变化时加载
                imageBitmap.value = loadImageBitmapFromPath(context, it, 256, 256)
            } ?: run {
                // 如果图标路径无效，请求刷新图标
                if (mod.icon != null) {
                    modViewModel.flashModImage(mod)
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
            onLongClick = { onLongClick(mod) }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (!isSelected) CardDefaults.cardColors().containerColor else MaterialTheme.colorScheme.secondaryContainer,
        )
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
                // 使用稳定的图像引用
                val currentImage = imageBitmap.value
                if (currentImage != null) {
                    Image(
                        bitmap = currentImage,
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
            Column(modifier = Modifier.weight(1f)) {
                mod.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mod.description ?: stringResource(R.string.mod_bean_no_readme),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.align(Alignment.CenterVertically)
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
