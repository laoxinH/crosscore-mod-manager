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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.ui.theme.ExpressiveSwitch
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
        items(
            items = mods,
            key = { mod -> mod.id },
            contentType = { "modItem" }
        ) { mod ->
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
    val iconPath = remember(mod.icon) {
        mod.icon?.takeIf { File(it).exists() }
    }

    // 使用新的图片加载器，自动处理缓存
    val imageBitmap by rememberImageBitmap(path = iconPath)

    var localIsEnable by remember(mod.id) { mutableStateOf<Boolean?>(null) }
    val displayIsEnable = localIsEnable ?: mod.isEnable

    LaunchedEffect(mod.isEnable) {
        if (localIsEnable != null && localIsEnable == mod.isEnable) {
            localIsEnable = null
        }
    }

    val onClick = remember(mod.id, isMultiSelect) {
        { if (isMultiSelect) onMultiSelectClick(mod) else openModDetail(mod, true) }
    }
    val onCheckedChange = remember(mod.id) {
        { isChecked: Boolean ->
            localIsEnable = isChecked
            enableMod(mod, isChecked)
        }
    }

    // 如果图标路径不存在但 mod.icon 不为空，触发重新解压
    // 使用 key 参数确保只在 icon 变化时触发
    LaunchedEffect(key1 = mod.icon, key2 = iconPath) {
        if (mod.icon != null && iconPath == null) {
            modViewModel.flashModImage(mod)
        }
    }

    val cardColors = if (!isSelected) {
        CardDefaults.cardColors()
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }

    val elevation = if (isSelected) {
        CardDefaults.cardElevation(2.dp)
    } else {
        CardDefaults.cardElevation(0.dp)
    }

    val onLongClickCallback = remember(mod.id) {
        { onLongClick(mod) }
    }

    Card(
        elevation = elevation,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClickCallback
        ),
        colors = cardColors,
        shape = MaterialTheme.shapes.large
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
                    .clip(MaterialTheme.shapes.extraSmall)
                    .align(Alignment.CenterVertically)
            ) {
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        alignment = Alignment.TopCenter,
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
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
                ExpressiveSwitch(
                    checked = displayIsEnable,
                    onCheckedChange = onCheckedChange,
                    enabled = modSwitchEnable
                )
            }
        }
    }
}
