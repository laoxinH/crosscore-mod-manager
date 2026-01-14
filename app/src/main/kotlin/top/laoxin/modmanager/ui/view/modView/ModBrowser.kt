package top.laoxin.modmanager.ui.view.modView

import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.flow.distinctUntilChanged
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.ui.view.common.fuzzyContains
import top.laoxin.modmanager.ui.viewmodel.ModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModDetailViewModel
import top.laoxin.modmanager.ui.viewmodel.ModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel

@Composable
fun ModsBrowser(
    modBrowserViewModel: ModBrowserViewModel,
    modListViewModel: ModListViewModel,
    modDetailViewModel: ModDetailViewModel,
    modOperationViewModel: ModOperationViewModel,
    modSearchViewModel: ModSearchViewModel
) {
    // 收集状态
    val modBrowserUiState = modBrowserViewModel.uiState.collectAsState()
    val currentGameModPath = modBrowserUiState.value.currentGameModPath
    val currentFiles = modBrowserUiState.value.currentFiles
    val currentPath = modBrowserUiState.value.currentPath
    val isBackPathExist = modBrowserUiState.value.isBackPathExist
    val doBackFunction = modBrowserUiState.value.doBackFunction
    val currentMods = modBrowserUiState.value.currentMods

    val modListUiState = modListViewModel.uiState.collectAsState()
    val modListState = modListUiState.value.modList
    val isMultiSelect = modListUiState.value.isMultiSelect
    val modsSelected = modListUiState.value.modsSelected
    // 收集操作状态
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modSwitchEnable = modOperationUiState.modSwitchEnable
    // 收集搜索状态
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val searchQuery = modSearchUiState.searchContent
    val searchBoxVisible = modSearchUiState.searchBoxVisible
    val searchModList = modSearchUiState.searchModList

    var currentPathState by remember(currentGameModPath) { mutableStateOf(currentGameModPath) }

    val files =
        remember(
            currentFiles, /*searchQuery*/
        ) {
            /* if (searchQuery.isNotEmpty()) currentFiles.filter { it.path.contains(searchQuery) } else*/ currentFiles
        }
    val modList =
        remember(modListState, searchQuery) {
            modListState.filter { searchQuery.isBlank() || it.name.fuzzyContains(searchQuery) }
        }
    val listState = rememberLazyListState()
    val scrollPositions = remember { mutableMapOf<String, Int>() }
    val scrollOffsets = remember { mutableMapOf<String, Int>() }

    val isAtRootPath by
    remember(currentPathState, currentGameModPath) {
        Log.d(
            "ModBrowser",
            "当前路径: $currentPathState, 外部路径: ${Environment.getExternalStorageDirectory().path}"
        )
        derivedStateOf {
            currentPathState == currentGameModPath ||
                    currentPathState == Environment.getExternalStorageDirectory().path ||
                    "$currentPathState/" == currentGameModPath
        }
    }

    LaunchedEffect(currentGameModPath) {
        //  modBrowserViewModel.setBackIconVisible(!isAtRootPath)
    }

    val doBack = remember {
        {
            if (!isAtRootPath) {
                scrollPositions[currentPathState] = listState.firstVisibleItemIndex
                scrollOffsets[currentPathState] = listState.firstVisibleItemScrollOffset
                currentPathState = File(currentPathState).parent ?: currentPathState

                val nextIsRoot =
                    File(currentPathState).path == currentGameModPath ||
                            File(currentPathState).path ==
                            Environment.getExternalStorageDirectory().path
                // modBrowserViewModel.setBackIconVisible(!nextIsRoot)
            }
        }
    }

    LaunchedEffect(doBackFunction) {
        if (doBackFunction) {
            doBack()
            modBrowserViewModel.setDoBackFunction(false)
            modBrowserViewModel.setBackIconVisible(false)
        }
    }

    LaunchedEffect(currentPathState, modList) {
        snapshotFlow { currentPathState }.distinctUntilChanged().collect { newPath ->
            val isRoot =
                ("$newPath/").equals(currentGameModPath, true) ||
                        newPath.equals(currentGameModPath, true) ||
                        newPath == Environment.getExternalStorageDirectory().path

            modBrowserViewModel.setBackIconVisible(!isRoot)
            // modBrowserViewModel.setDoBackFunction(!isRoot)
            modBrowserViewModel.updateFiles(newPath)
        }
    }

    LaunchedEffect(files) {
        if (files.isNotEmpty()) {
            listState.scrollToItem(
                scrollPositions[currentPathState] ?: 0,
                scrollOffsets[currentPathState] ?: 0
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHandler(enabled = !isAtRootPath) {
            // modBrowserViewModel.setDoBackFunction(true)
        }

        AnimatedContent(
            targetState = currentPathState,
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            label = "PathAnimation"
        ) { path ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val mods =
                    remember(path, files, modList) {
                        files.flatMap { file ->
                            modBrowserViewModel.getModsByPathStrict(file.path, modList) +
                                    modBrowserViewModel.getModsByVirtualPathsStrict(
                                        file.path,
                                        modList
                                    )
                        }
                    }

                LaunchedEffect(mods) { modBrowserViewModel.setCurrentMods(mods) }

                LaunchedEffect(currentPathState, files.isEmpty()) {
                    if (!isAtRootPath && files.isEmpty()) {
                        // modBrowserViewModel.setBackIconVisible(true)
                    }
                }

                if (files.isEmpty() /*&& currentMods.isEmpty() ||*/ ||
                    currentPathState == Environment.getExternalStorageDirectory().path
                ) {
                    // Log.d(ModBrowserViewModel.TAG, "No mods found in $path---$files",)
                    NoModInDir()
                } else {
                    //  Log.d(ModBrowserViewModel.TAG, "Mods found in $path---$files",)
                    FileListContent(
                        files = files,
                        modList = modList,
                        listState = listState,
                        isBackPathExist = isBackPathExist,
                        isAtRootPath = isAtRootPath,
                        isMultiSelect = isMultiSelect,
                        modsSelected = modsSelected,
                        modSwitchEnable = modSwitchEnable,
                        isGridView = modBrowserUiState.value.isGridView,
                        modBrowserViewModel = modBrowserViewModel,
                        modListViewModel = modListViewModel,
                        modDetailViewModel = modDetailViewModel,
                        modOperationViewModel = modOperationViewModel,
                        onFileClick = { file ->
                            if (file.isDirectory || !file.exists()) {
                                scrollPositions[currentPathState] =
                                    listState.firstVisibleItemIndex
                                scrollOffsets[currentPathState] =
                                    listState.firstVisibleItemScrollOffset
                                currentPathState = file.path
                            }
                        },
                        onFileClickByZip = { file ->
                            scrollPositions[currentPathState] = listState.firstVisibleItemIndex
                            scrollOffsets[currentPathState] =
                                listState.firstVisibleItemScrollOffset
                            currentPathState = file.path
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListContent(
    files: List<File>,
    modList: List<ModBean>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isBackPathExist: Boolean,
    isAtRootPath: Boolean,
    isMultiSelect: Boolean,
    modsSelected: Set<Int>,
    modSwitchEnable: Boolean,
    isGridView: Boolean = false,
    modBrowserViewModel: ModBrowserViewModel,
    modListViewModel: ModListViewModel,
    modDetailViewModel: ModDetailViewModel,
    modOperationViewModel: ModOperationViewModel,
    onFileClick: (File) -> Unit,
    onFileClickByZip: (File) -> Unit
) {
    if (isGridView) {
        // 网格视图
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = files, key = { file -> file.path }, contentType = { "gridItem" }) { file
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

                // 根据文件类型显示不同的卡片
                when {
                    // 单个MOD：显示大图卡片
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

                    // 多个MOD的压缩包
                    modsByPath.size > 1 || modsByVirtualPaths.size > 1 -> {
                        FolderGridItem(
                            name = file.name,
                            modCount = modCount,
                            modEnableCount = modEnableCount,
                            iconId = R.drawable.zip_mod_icon,
                            onClick = { onFileClickByZip(file) }
                        )
                    }

                    // 文件夹或不存在的路径（可能是虚拟路径）
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

                    // 其他文件（未知类型）- 显示为普通文件卡片
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
                if (!isBackPathExist && !isAtRootPath) {
                    LaunchedEffect(Unit) {
                        // modBrowserViewModel.setBackIconVisible(true)
                    }
                }

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
            modifier
                .combinedClickable(
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
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .sizeIn(minHeight = 30.dp, maxHeight = 80.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
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
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                // 背景渐变
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)) {
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

fun findStringDifferences(str1: String, str2: String): List<Difference> {
    val differences = mutableListOf<Difference>()
    val maxLength = maxOf(str1.length, str2.length)

    for (i in 0 until maxLength) {
        val char1 = str1.getOrNull(i)
        val char2 = str2.getOrNull(i)

        if (char1 != char2) {
            differences.add(Difference(i, char1, char2))
        }
    }

    return differences
}

data class Difference(val position: Int, val char1: Char?, val char2: Char?)
