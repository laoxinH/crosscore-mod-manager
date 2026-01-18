package top.laoxin.modmanager.ui.view.components.mod

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import java.io.File
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.viewmodel.ModernModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModernModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ModernModsBrowser(
    initialPath: String? = null,
    modBrowserViewModel: ModernModBrowserViewModel,
    modListViewModel: ModernModListViewModel,
    modDetailViewModel: ModDetailViewModel,
    modOperationViewModel: ModOperationViewModel,
    modSearchViewModel: ModSearchViewModel,
    onNavigateToPath: (String) -> Unit
) {
    // 收集状态
    val modBrowserUiState = modBrowserViewModel.uiState.collectAsState()
    val currentGameModPath = modBrowserUiState.value.currentGameModPath

    // Determine the path to display. If initialPath is null or empty, use the game's mod path.
    val currentPath = if (initialPath.isNullOrEmpty()) currentGameModPath else initialPath

    // Load files specifically for this path using the ViewModel's Flow
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val searchQuery = modSearchUiState.searchContent
    val modListUiState = modListViewModel.uiState.collectAsState()
    val modListState = modListUiState.value.modList
    val files by
    produceState<List<File>>(
        initialValue = emptyList(),
        key1 = currentPath,
        key2 = searchQuery,
        key3 = modListState
    ) { modBrowserViewModel.getFilesFlow(currentPath, searchQuery).collect { value = it } }

    // Other Shared States

    val isMultiSelect = modListUiState.value.isMultiSelect
    val modsSelected = modListUiState.value.modsSelected
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modSwitchEnable = modOperationUiState.modSwitchEnable

    // 拦截返回
    /*  BackHandler(enabled = isMultiSelect) {
          modListViewModel.onBackClick()
      }*/

    // Scroll State Persistence
    val (listInitIndex, listInitOffset) = remember(currentPath) {
        modBrowserViewModel.getScrollState("${currentPath}_LIST")
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = listInitIndex,
        initialFirstVisibleItemScrollOffset = listInitOffset
    )

    val (gridInitIndex, gridInitOffset) = remember(currentPath) {
        modBrowserViewModel.getScrollState("${currentPath}_GRID")
    }
    // Need to remember grid state as well
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
        initialFirstVisibleItemIndex = gridInitIndex,
        initialFirstVisibleItemScrollOffset = gridInitOffset
    )

    // Save scroll state when path changes or composable is disposed (e.g. Tab Switch)
    androidx.compose.runtime.DisposableEffect(currentPath) {
        onDispose {
            modBrowserViewModel.saveScrollState(
                "${currentPath}_LIST",
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
            modBrowserViewModel.saveScrollState(
                "${currentPath}_GRID",
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            // Calculate mods for the current directory (visual feedback)
            /*  val mods =
                      remember(currentPath, files, modListState) {
                          files.flatMap { file ->
                              modBrowserViewModel.getModsByPathStrict(file.path, modListState) +
                                      modBrowserViewModel.getModsByVirtualPathsStrict(
                                              file.path,
                                              modListState
                                      )
                          }
                      }


              LaunchedEffect(mods) { modBrowserViewModel.setCurrentMods(mods) }*/
            // Removed automatic setting of 'currentMods' in global state because it's view-specific
            // now.
            // If other components need it, we might need a different mechanism, but for listing,
            // it's local.

            if (files.isEmpty()) {
                NoModInDir()
            } else {
                ModernFileListContent(
                    files = files,
                    modList = modListState,
                    listState = listState,
                    isMultiSelect = isMultiSelect,
                    modsSelected = modsSelected,
                    modSwitchEnable = modSwitchEnable,
                    isGridView = modBrowserUiState.value.isGridView,
                    modBrowserViewModel = modBrowserViewModel,
                    modListViewModel = modListViewModel,
                    modDetailViewModel = modDetailViewModel,
                    modOperationViewModel = modOperationViewModel,
                    onFileClick = { file ->
                        // Navigate if directory
                        if (file.isDirectory || !file.exists()
                        ) { // !exists often means virtual/zip in this app context
                            onNavigateToPath(file.path)
                        }
                    },
                    onFileClickByZip = { file -> onNavigateToPath(file.path) },
                    gridState = gridState
                )
            }
        }
    }
}

@Composable
private fun ModernFileListContent(
    files: List<File>,
    modList: List<ModBean>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState, // Added
    isMultiSelect: Boolean,
    modsSelected: Set<Int>,
    modSwitchEnable: Boolean,
    isGridView: Boolean = false,
    modBrowserViewModel: ModernModBrowserViewModel,
    modListViewModel: ModernModListViewModel,
    modDetailViewModel: ModDetailViewModel,
    modOperationViewModel: ModOperationViewModel,
    onFileClick: (File) -> Unit,
    onFileClickByZip: (File) -> Unit
) {
    AnimatedContent(
        targetState = isGridView,
        transitionSpec = {
            (fadeIn(tween(300)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(300)))
                .togetherWith(
                    fadeOut(tween(300)) +
                            scaleOut(targetScale = 0.95f, animationSpec = tween(300))
                )
        },
        label = "BrowserViewModeTransition"
    ) { isGrid ->
        if (isGrid) {
            // 网格视图
            LazyVerticalGrid(
                state = gridState, // Use state
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = files,
                    key = { file -> file.path },
                    contentType = { "gridItem" }) { file
                    ->
                    val filePath = file.path
                    val (modsByPath, modsByVirtualPaths) =
                        remember(filePath, modList) {
                            Pair(
                                modBrowserViewModel.getModsByPathStrict(filePath, modList),
                                modBrowserViewModel.getModsByVirtualPathsStrict(
                                    filePath,
                                    modList
                                )
                            )
                        }

                    val modsByFullPath =
                        remember(filePath, modList) {
                            modBrowserViewModel.getModsByPath(filePath, modList).ifEmpty {
                                modBrowserViewModel.getModsByVirtualPaths(filePath, modList)
                            }
                        }

                    val modCount = remember(modsByFullPath) { modsByFullPath.size }
                    val modEnableCount =
                        remember(modsByFullPath) { modsByFullPath.count { it.isEnable } }
                    val onClick = remember(filePath) { { onFileClick(file) } }

                    when {
                        modsByPath.size == 1 || modsByVirtualPaths.size == 1 -> {
                            val mod = modsByPath.firstOrNull() ?: modsByVirtualPaths.firstOrNull()!!
                            val onModLongClick: (ModBean) -> Unit =
                                remember(mod.id) { { modListViewModel.modLongClick(mod) } }
                            val onModMultiSelectClick: (ModBean) -> Unit =
                                remember(mod.id) { { modListViewModel.modMultiSelectClick(mod) } }

                            ModGridItem(
                                mod = mod,
                                isSelected = modsSelected.contains(mod.id),
                                onLongClick = onModLongClick,
                                onMultiSelectClick = onModMultiSelectClick,
                                isMultiSelect = isMultiSelect,
                                modSwitchEnable = modSwitchEnable,
                                openModDetail = { modDetailViewModel.openModDetail(it, true) },
                                enableMod = { modBean, enable ->
                                    modOperationViewModel.switchMod(modBean, enable)
                                },
                                modDetailViewModel = modDetailViewModel
                            )
                        }

                        modsByPath.size > 1 || modsByVirtualPaths.size > 1 -> {
                            FolderGridItem(
                                name = file.name,
                                modCount = modCount,
                                modEnableCount = modEnableCount,
                                iconId = R.drawable.zip_mod_icon,
                                onClick = { onFileClickByZip(file) }
                            )
                        }

                        file.isDirectory || !file.exists() -> {
                            FolderGridItem(
                                name = file.name,
                                modCount = modCount,
                                modEnableCount = modEnableCount,
                                iconId =
                                    if (modCount > 0) R.drawable.folder_mod_icon
                                    else R.drawable.folder_icon,
                                onClick = onClick
                            )
                        }

                        else -> {
                            FolderGridItem(
                                name = file.name,
                                modCount = 0,
                                modEnableCount = 0,
                                iconId = R.drawable.folder_icon,
                                onClick = onClick
                            )
                        }
                    }
                }
            }
        } else {
            // 列表视图
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(
                    items = files,
                    key = { file -> file.path },
                    contentType = { file ->
                        when {
                            file.isDirectory -> "directory"
                            !file.exists() -> "archive"
                            else -> "file"
                        }
                    }
                ) { file ->
                    val filePath = file.path
                    val (modsByPath, modsByVirtualPaths) =
                        remember(filePath, modList) {
                            Pair(
                                modBrowserViewModel.getModsByPathStrict(filePath, modList),
                                modBrowserViewModel.getModsByVirtualPathsStrict(
                                    filePath,
                                    modList
                                )
                            )
                        }

                    val modsByFullPath =
                        remember(filePath, modList) {
                            modBrowserViewModel.getModsByPath(filePath, modList).ifEmpty {
                                modBrowserViewModel.getModsByVirtualPaths(filePath, modList)
                            }
                        }


                    val modCount = remember(modsByFullPath) { modsByFullPath.size }
                    val modEnableCount =
                        remember(modsByFullPath) { modsByFullPath.count { it.isEnable } }
                    val onClick = remember(filePath) { { onFileClick(file) } }

                    if (modsByPath.isEmpty() &&
                        modsByVirtualPaths.isEmpty() &&
                        (file.isDirectory || !file.exists())
                    ) {
                        FileListItem(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            name = file.name,
                            isSelected = false,
                            onLongClick = {},
                            onClick = onClick,
                            onMultiSelectClick = {},
                            isMultiSelect = isMultiSelect,
                            description =
                                stringResource(
                                    R.string.mod_browser_file_description,
                                    modCount,
                                    modEnableCount
                                ),
                            iconId =
                                if (modCount > 0) R.drawable.folder_mod_icon
                                else R.drawable.folder_icon
                        )
                    }

                    if (modsByPath.size == 1 || modsByVirtualPaths.size == 1) {
                        val mod = modsByPath.firstOrNull() ?: modsByVirtualPaths.firstOrNull()!!
                        val onModLongClick: (ModBean) -> Unit =
                            remember(mod.id) { { modListViewModel.modLongClick(mod) } }
                        val onModMultiSelectClick: (ModBean) -> Unit =
                            remember(mod.id) { { modListViewModel.modMultiSelectClick(mod) } }

                        ModListItem(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            mod = mod,
                            isSelected = modsSelected.contains(mod.id),
                            onLongClick = onModLongClick,
                            onMultiSelectClick = onModMultiSelectClick,
                            isMultiSelect = isMultiSelect,
                            modSwitchEnable = modSwitchEnable,
                            openModDetail = { modDetailViewModel.openModDetail(it, true) },
                            enableMod = { modBean, enable ->
                                modOperationViewModel.switchMod(modBean, enable)
                            },
                            modDetailViewModel = modDetailViewModel
                        )
                    }

                    if (modsByPath.size > 1 || modsByVirtualPaths.size > 1) {
                        FileListItem(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            name = file.name,
                            isSelected = false,
                            onLongClick = {},
                            onClick = { onFileClickByZip(file) },
                            onMultiSelectClick = {},
                            isMultiSelect = isMultiSelect,
                            description =
                                stringResource(
                                    R.string.mod_browser_file_description,
                                    modCount,
                                    modEnableCount
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
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onMultiSelectClick: () -> Unit,
    isMultiSelect: Boolean = false,
    @DrawableRes iconId: Int = R.drawable.folder_icon,
    description: String? = null,
) {
    Card(
        elevation =
            if (isSelected) CardDefaults.cardElevation(2.dp)
            else CardDefaults.cardElevation(0.dp),
        modifier =
            modifier.combinedClickable(
                onClick = {
                    if (isMultiSelect) onMultiSelectClick() else onClick()
                },
                onLongClick = onLongClick
            )
                .animateContentSize(),
        colors =
            if (!isSelected) CardDefaults.cardColors()
            else
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(16.dp)
                    .sizeIn(minHeight = 30.dp, maxHeight = 80.dp)
        ) {
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleSmall)

                Spacer(Modifier.height(8.dp))
                if (description != null) {
                    Text(text = (description), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun NoModInDir() {
    Log.d("ModBrowserViewModel", "当前路径为空")
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = R.string.mod_page_no_mod_dir),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** 文件夹/压缩包的网格卡片 - 与ModGridItem风格一致 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(
    modifier: Modifier = Modifier,
    name: String,
    modCount: Int = 0,
    modEnableCount: Int = 0,
    @DrawableRes iconId: Int = R.drawable.folder_icon,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val cardColors = CardDefaults.cardColors()
    val elevation = CardDefaults.cardElevation(1.dp)

    Card(
        elevation = elevation,
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = cardColors,
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            // 图标区域 - 1:1比例
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                // 背景渐变
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                )
                // 图标
                Image(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 底部信息区域
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                // 文件夹/压缩包名称
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // MOD数量信息
                if (modCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text =
                            stringResource(
                                R.string.mod_browser_file_description,
                                modCount,
                                modEnableCount
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
