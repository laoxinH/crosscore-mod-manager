package top.laoxin.modmanager.ui.view.modView

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.theme.ExpressiveSwitch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import java.io.File

@Composable
fun ModList(
    mods: List<ModBean>,
    modsSelected: Set<Int>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modSwitchEnable: Boolean,
    isMultiSelect: Boolean,
    isGridView: Boolean = false,
    showDialog: (ModBean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,
    onLongClick: (ModBean) -> Unit,
    onMultiSelectClick: (ModBean) -> Unit,
    modListViewModel: ModListViewModel,
    modOperationViewModel: ModOperationViewModel,
    modDetailViewModel: ModDetailViewModel
) {
    if (isGridView) {
        // 大图网格视图
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = mods,
                key = { mod -> mod.id },
                contentType = { "modGridItem" }
            ) { mod ->
                ModGridItem(
                    mod = mod,
                    isSelected = modsSelected.contains(mod.id),
                    onLongClick = onLongClick,
                    onMultiSelectClick = onMultiSelectClick,
                    isMultiSelect = isMultiSelect,
                    modSwitchEnable = modSwitchEnable,
                    openModDetail = { showDialog(mod) },
                    enableMod = enableMod,
                    modDetailViewModel = modDetailViewModel
                )
            }
        }
    } else {
        // 列表视图
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
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    mod = mod,
                    isSelected = modsSelected.contains(mod.id),
                    onLongClick = onLongClick,
                    onMultiSelectClick = onMultiSelectClick,
                    isMultiSelect = isMultiSelect,
                    modSwitchEnable = modSwitchEnable,
                    openModDetail = { showDialog(mod) },
                    enableMod = enableMod,
                    modDetailViewModel = modDetailViewModel
                )
            }
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
    openModDetail: (ModBean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,
    modDetailViewModel: ModDetailViewModel
) {
    // 使用 updateAt 作为 key，刷新后 updateAt 变化会触发重新检测和加载
    val iconPath = remember(mod.icon, mod.updateAt) {
        mod.icon?.takeIf { File(it).exists() }
    }

    // 使用新的图片加载器，自动处理缓存，updateAt 变化时重新加载
    val imageBitmap by rememberImageBitmap(path = iconPath, key = mod.updateAt)

    //var localIsEnable by remember(mod.id) { mutableStateOf<Boolean?>(null) }
    //val displayIsEnable = localIsEnable ?: mod.isEnable

    //Log.d("ModList", "触发重组当前开启状态: ${mod.isEnable}")

    /*LaunchedEffect(mod.isEnable) {
        if (localIsEnable != null && localIsEnable == mod.isEnable) {
            localIsEnable = null
        }
    }*/

    val onClick = remember(mod, isMultiSelect) {
        { if (isMultiSelect) onMultiSelectClick(mod) else openModDetail(mod) }
    }
    val onCheckedChange = remember(mod) {
        { isChecked: Boolean ->
           // localIsEnable = isChecked
            enableMod(mod, isChecked)
        }
    }

    // 如果图标路径不存在但 mod.icon 不为空，触发重新解压
    // 使用 key 参数确保只在 icon 变化时触发
    LaunchedEffect(key1 = mod.icon, key2 = iconPath) {
        if (mod.icon != null && iconPath == null) {
            modDetailViewModel.refreshModDetail(mod)
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
                mod.name.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mod.description.ifEmpty { stringResource(R.string.mod_bean_no_readme) },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                ExpressiveSwitch(
                    checked = mod.isEnable,
                    onCheckedChange = onCheckedChange,
                    enabled = modSwitchEnable
                )
            }
        }
    }
}

/** 大图网格视图MOD卡片 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModGridItem(
    modifier: Modifier = Modifier,
    mod: ModBean,
    isSelected: Boolean = false,
    onLongClick: (ModBean) -> Unit,
    onMultiSelectClick: (ModBean) -> Unit,
    isMultiSelect: Boolean = false,
    modSwitchEnable: Boolean,
    openModDetail: (ModBean) -> Unit,
    enableMod: (ModBean, Boolean) -> Unit,
    modDetailViewModel: ModDetailViewModel
) {
    // 使用 updateAt 作为 key，刷新后 updateAt 变化会触发重新检测和加载
    val iconPath = remember(mod.icon, mod.updateAt) {
        mod.icon?.takeIf { File(it).exists() }
    }

    // 使用新的图片加载器，自动处理缓存，updateAt 变化时重新加载
    val imageBitmap by rememberImageBitmap(path = iconPath, reqWidth = 300, reqHeight = 400, key = mod.updateAt)

    val onClick = remember(mod, isMultiSelect) {
        { if (isMultiSelect) onMultiSelectClick(mod) else openModDetail(mod) }
    }
    val onCheckedChange = remember(mod) {
        { isChecked: Boolean ->
            enableMod(mod, isChecked)
        }
    }

    // 如果图标路径不存在但 mod.icon 不为空，触发重新解压
    LaunchedEffect(key1 = mod.icon, key2 = iconPath) {
        if (mod.icon != null && iconPath == null) {
            modDetailViewModel.refreshModDetail(mod)
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
        CardDefaults.cardElevation(4.dp)
    } else {
        CardDefaults.cardElevation(1.dp)
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
        Column {
            // 大预览图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)  // 1:1 比例
                    .clip(MaterialTheme.shapes.large)
            ) {
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // 底部信息区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MOD名称
                Text(
                    text = mod.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 标签式开关
                Box(
                    modifier = Modifier
                        .background(
                            color = if (mod.isEnable) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable(enabled = modSwitchEnable) { onCheckedChange(!mod.isEnable) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (mod.isEnable) "ON" else "OFF",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (mod.isEnable) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
