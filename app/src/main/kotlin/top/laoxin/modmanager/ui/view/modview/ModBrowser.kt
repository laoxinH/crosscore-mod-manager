package top.laoxin.modmanager.ui.view.modview

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.laoxin.modmanager.R
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import java.io.File
const val TAG = "ModsBrowser"
@Composable
fun ModsBrowser(viewModel: ModViewModel, uiState: ModUiState) {
    var currentPath by remember { mutableStateOf(uiState.currentPath) }
    val files = uiState.currentFiles
    var previousPath by remember { mutableStateOf(currentPath) }
    val listState = rememberLazyListState()
    val scrollPositions = remember { mutableMapOf<String, Int>() }
    val scrollOffsets = remember { mutableMapOf<String, Int>() }
    if (currentPath == ModTools.MOD_PATH) {
        return NoMod()
    }

    // 返回上级目录是否显示
    LaunchedEffect(currentPath) {
        viewModel.updateFiles(currentPath)
    }

    // 监听列表加载状态并在加载完成后滚动到指定位置
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .collect { itemCount ->
                if (itemCount > 0) {
                    listState.scrollToItem(scrollPositions[currentPath] ?: 0, scrollOffsets[currentPath] ?: 0)
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 监听返回按键事件
        if (currentPath != uiState.currentGameModPath) {
            BackHandler {
                if (currentPath != Environment.getExternalStorageDirectory().path) {
                    // 记录当前路径的滚动位置
                    scrollPositions[currentPath] = listState.firstVisibleItemIndex
                    scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
                    previousPath = currentPath
                    currentPath = File(currentPath).parent ?: currentPath
                }
            }
        }

        AnimatedContent(
            targetState = currentPath, transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            }, label = ""
        ) { path ->
            val mods = files.flatMap { file ->
                viewModel.getModsByPathStrict(file.path) + viewModel.getModsByVirtualPathsStrict(
                    file.path
                )
            }
            viewModel.setCurrentMods(mods)
            // 返回上级目录按钮
            if (currentPath != uiState.currentGameModPath && files.isEmpty()) {
                FileListItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    name = stringResource(R.string.mod_browser_file_list_back),
                    isSelected = false,
                    onLongClick = {
                        // 长按事件
                    },
                    onClick = {
                        if (currentPath != uiState.currentGameModPath) {
                            // 记录当前路径的滚动位置
                            scrollPositions[currentPath] = listState.firstVisibleItemIndex
                            scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
                            previousPath = currentPath
                            currentPath = File(currentPath).parent ?: currentPath
                        }
                    },
                    onMultiSelectClick = {
                        // 多选状态下的点击事件
                    },
                    isMultiSelect = uiState.isMultiSelect,
                    description = currentPath.replace(uiState.currentGameModPath, ""),
                    iconId = R.drawable.back_icon
                )
            }
            LazyColumn(state = listState) {
                items(files) { file: File ->
                    if (currentPath != uiState.currentGameModPath && files.indexOf(file) == 0) {
                        FileListItem(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            name = stringResource(R.string.mod_browser_file_list_back),
                            isSelected = false,
                            onLongClick = {
                                // 长按事件
                            },
                            onClick = {
                                if (currentPath != uiState.currentGameModPath) {
                                    // 记录当前路径的滚动位置
                                    scrollPositions[currentPath] = listState.firstVisibleItemIndex
                                    scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
                                    previousPath = currentPath
                                    currentPath = File(currentPath).parent ?: currentPath
                                }
                            },
                            onMultiSelectClick = {
                                // 多选状态下的点击事件
                            },
                            isMultiSelect = uiState.isMultiSelect,
                            description = currentPath.replace(uiState.currentGameModPath, ""),
                            iconId = R.drawable.back_icon
                        )
                    }

                    // 通过filepath获取mods
                    val modsByPath = viewModel.getModsByPathStrict(file.path)
                    // 通过虚拟路径获取mods
                    val modsByVirtualPaths = viewModel.getModsByVirtualPathsStrict(file.path)

                    // 设置当前页面的mods

                    val modCount = if (viewModel.getModsByPath(file.path)
                            .isNotEmpty()
                    ) viewModel.getModsByPath(file.path).size else viewModel.getModsByVirtualPaths(
                        file.path
                    ).size

                    val modEnableCount = if (viewModel.getModsByPath(file.path)
                            .isNotEmpty()
                    ) viewModel.getModsByPath(file.path).filter { it.isEnable }.size else viewModel.getModsByVirtualPaths(
                        file.path
                    ).filter { it.isEnable }.size
                    if (modsByPath.isEmpty() && modsByVirtualPaths.isEmpty() && (file.isDirectory || !file.exists())) {
                        FileListItem(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            name = file.name,
                            isSelected = false,
                            onLongClick = {},
                            onClick = {
                                if (file.isDirectory || !file.exists()) {
                                    // 记录当前路径的滚动位置
                                    scrollPositions[currentPath] = listState.firstVisibleItemIndex
                                    scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
                                    previousPath = currentPath
                                    currentPath = file.path
                                } else {
                                    // 处理文件点击事件
                                }
                            },
                            onMultiSelectClick = {},
                            isMultiSelect = uiState.isMultiSelect,
                            description = stringResource(
                                R.string.mod_browser_file_description, modCount, modEnableCount
                            ),
                            iconId = if (modCount > 0) R.drawable.folder_mod_icon else R.drawable.folder_icon
                        )
                    }
                    if (modsByPath.size == 1 || modsByVirtualPaths.size == 1) {
                        ModListItem(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            mod = modsByPath.firstOrNull() ?: modsByVirtualPaths.firstOrNull()!!,
                            isSelected = uiState.modsSelected.contains(
                                modsByPath.firstOrNull()?.id
                                    ?: modsByVirtualPaths.firstOrNull()?.id!!
                            ),
                            onLongClick = {
                                viewModel.modLongClick(
                                    modsByPath.firstOrNull() ?: modsByVirtualPaths.firstOrNull()!!
                                )
                            },
                            onMultiSelectClick = {
                                viewModel.modMultiSelectClick(
                                    modsByPath.firstOrNull() ?: modsByVirtualPaths.firstOrNull()!!
                                )
                            },
                            isMultiSelect = uiState.isMultiSelect,
                            modSwitchEnable = uiState.modSwitchEnable,
                            openModDetail = { mod, _ ->
                                viewModel.openModDetail(mod, true)
                            },
                            enableMod = { mod, isEnable ->
                                viewModel.switchMod(mod, isEnable)
                            })
                    }
                    if (modsByPath.size > 1 || modsByVirtualPaths.size > 1) {
                        FileListItem(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            name = file.name,
                            isSelected = false,
                            onLongClick = {},
                            onClick = {
                                // 记录当前路径的滚动位置
                                scrollPositions[currentPath] = listState.firstVisibleItemIndex
                                scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
                                previousPath = currentPath
                                currentPath = file.path
                            },
                            onMultiSelectClick = {},
                            isMultiSelect = uiState.isMultiSelect,
                            description = stringResource(
                                R.string.mod_browser_file_description,
                                viewModel.getModsByPath(file.path).size,
                                viewModel.getModsByPath(file.path).filter { it.isEnable }.size
                            ),
                            iconId = R.drawable.zip_mod_icon
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    modifier: Modifier = Modifier,
    name: String,

    isSelected: Boolean = false,
    // 长按回调
    onLongClick: () -> Unit,  // 长按
    onClick: () -> Unit,
    onMultiSelectClick: () -> Unit, // 多选状态下的点击
    isMultiSelect: Boolean = false, // 是否多选状态
    // 图标id
    @DrawableRes iconId: Int = R.drawable.folder_icon,
    // 描述
    description: String? = null,
) {
    Card(
        elevation = if (isSelected) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(
            0.dp
        ), modifier = modifier
            .combinedClickable(onClick = {
                if (isMultiSelect) {
                    onMultiSelectClick()
                } else {
                    onClick()
                }
            }, onLongClick = {
                onLongClick()
            })
            .animateContentSize(), colors = CardDefaults.cardColors(
            containerColor = if (!isSelected) CardDefaults.cardColors().containerColor else MaterialTheme.colorScheme.secondaryContainer,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .sizeIn(minHeight = 30.dp, maxHeight = 80.dp)


        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterVertically)


            ) {
                Image(
                    painterResource(id = iconId),
                    contentDescription = null,
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.FillWidth
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = /*if (name.length > 10) name.substring(0, 10) + "..." else*/ name,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(8.dp))
                if (description != null) {
                    Text(
                        text = (/*if (description.length > 10) description.substring(0, 10) + "..." else*/ description),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            //Spacer(Modifier.width(16.dp))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewFileBrowser() {
    // FileBrowser(viewModel, uiState)
}