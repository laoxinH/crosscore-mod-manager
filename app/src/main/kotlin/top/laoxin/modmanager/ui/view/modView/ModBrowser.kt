package top.laoxin.modmanager.ui.view.modView

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.state.ModUiState
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import java.io.File

@Composable
fun ModsBrowser(viewModel: ModViewModel, uiState: ModUiState) {
    var currentPath by remember { mutableStateOf(uiState.currentPath) }
    val files = remember(uiState.currentFiles) { uiState.currentFiles }
    var previousPath by remember { mutableStateOf(currentPath) }
    val listState = rememberLazyListState()
    val scrollPositions = remember { mutableMapOf<String, Int>() }
    val scrollOffsets = remember { mutableMapOf<String, Int>() }
    val doBackFunction by rememberUpdatedState(uiState.doBackFunction)

    val isAtRootPath by remember(currentPath, uiState.currentGameModPath) {
        derivedStateOf {
            currentPath == uiState.currentGameModPath || currentPath == Environment.getExternalStorageDirectory().path
        }
    }

    fun doBack() {
        if (!isAtRootPath) {
            scrollPositions[currentPath] = listState.firstVisibleItemIndex
            scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
            previousPath = currentPath
            currentPath = File(currentPath).parent ?: currentPath
        }
    }

    LaunchedEffect(doBackFunction) {
        if (doBackFunction) {
            doBack()
            viewModel.setDoBackFunction(false)
            viewModel.setBackIconVisible(false)
        }
    }

    LaunchedEffect(currentPath) {
        snapshotFlow { currentPath }
            .distinctUntilChanged()
            .collect { newPath ->
                viewModel.updateFiles(newPath)
            }
    }

    LaunchedEffect(files) {
        if (files.isNotEmpty()) {
            listState.scrollToItem(
                scrollPositions[currentPath] ?: 0,
                scrollOffsets[currentPath] ?: 0
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isAtRootPath) {
            BackHandler {
                viewModel.setDoBackFunction(true)
            }
        }

        AnimatedContent(
            targetState = currentPath,
            transitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            }
        ) { path ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val mods by remember {
                    derivedStateOf {
                        files.flatMap { file ->
                            viewModel.getModsByPathStrict(file.path) +
                                    viewModel.getModsByVirtualPathsStrict(file.path)
                        }
                    }
                }

                LaunchedEffect(mods) {
                    viewModel.setCurrentMods(mods)
                }

                LaunchedEffect(currentPath, files.isEmpty()) {
                    if (!isAtRootPath && files.isEmpty()) {
                        viewModel.setBackIconVisible(true)
                    }
                }

                if (files.isEmpty() && uiState.currentMods.isEmpty() && !isAtRootPath) {
                    NoModInDir()
                } else {
                    FileListContent(
                        files = files,
                        uiState = uiState,
                        listState = listState,
                        isBackPathExist = uiState.isBackPathExist,
                        isAtRootPath = isAtRootPath,
                        viewModel = viewModel,
                        onFileClick = { file ->
                            if (file.isDirectory || !file.exists()) {
                                scrollPositions[currentPath] = listState.firstVisibleItemIndex
                                scrollOffsets[currentPath] = listState.firstVisibleItemScrollOffset
                                previousPath = currentPath
                                currentPath = file.path
                            }
                        },
                        onFileClickByZip = { file ->
                            scrollPositions[currentPath] = listState.firstVisibleItemIndex
                            scrollOffsets[currentPath] =
                                listState.firstVisibleItemScrollOffset
                            previousPath = currentPath
                            currentPath = file.path
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
    uiState: ModUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isBackPathExist: Boolean,
    isAtRootPath: Boolean,
    viewModel: ModViewModel,
    onFileClick: (File) -> Unit,
    onFileClickByZip: (File) -> Unit
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(
            items = files,
            key = { file -> file.path }
        ) { file ->
            if (!isBackPathExist && !isAtRootPath) {
                LaunchedEffect(Unit) {
                    viewModel.setBackIconVisible(true)
                }
            }

            val modsByPath = viewModel.getModsByPathStrict(file.path)
            val modsByVirtualPaths = viewModel.getModsByVirtualPathsStrict(file.path)

            val modCount =
                if (modsByPath.isNotEmpty()) modsByPath.size else viewModel.getModsByVirtualPaths(
                    file.path
                ).size

            val modEnableCount = if (modsByPath.isNotEmpty())
                modsByPath.count { it.isEnable }
            else
                viewModel.getModsByVirtualPaths(file.path).count { it.isEnable }

            if (modsByPath.isEmpty() && modsByVirtualPaths.isEmpty() && (file.isDirectory || !file.exists())) {
                FileListItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    name = file.name,
                    isSelected = false,
                    onLongClick = {},
                    onClick = { onFileClick(file) },
                    onMultiSelectClick = {},
                    isMultiSelect = uiState.isMultiSelect,
                    description = stringResource(
                        R.string.mod_browser_file_description,
                        modCount,
                        modEnableCount
                    ),
                    iconId = if (modCount > 0) R.drawable.folder_mod_icon else R.drawable.folder_icon
                )
            }
            if (modsByPath.size == 1 || modsByVirtualPaths.size == 1) {
                val mod = modsByPath.firstOrNull() ?: modsByVirtualPaths.firstOrNull()!!
                ModListItem(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    mod = mod,
                    isSelected = uiState.modsSelected.contains(mod.id),
                    onLongClick = { viewModel.modLongClick(mod) },
                    onMultiSelectClick = { viewModel.modMultiSelectClick(mod) },
                    isMultiSelect = uiState.isMultiSelect,
                    modSwitchEnable = uiState.modSwitchEnable,
                    openModDetail = { selectedMod, _ ->
                        viewModel.openModDetail(
                            selectedMod,
                            true
                        )
                    },
                    enableMod = { selectedMod, isEnable ->
                        viewModel.switchMod(
                            selectedMod,
                            isEnable
                        )
                    },
                    modViewModel = viewModel
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
                    isMultiSelect = uiState.isMultiSelect,
                    description = stringResource(
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
        elevation = if (isSelected) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(
            0.dp
        ),
        modifier = modifier
            .combinedClickable(
                onClick = { if (isMultiSelect) onMultiSelectClick() else onClick() },
                onLongClick = onLongClick
            )
            .animateContentSize(),
        colors = if (!isSelected) CardDefaults.cardColors() else CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    text = name,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(8.dp))
                if (description != null) {
                    Text(
                        text = (description),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun NoModInDir() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.mod_page_no_mod_dir),
            style = MaterialTheme.typography.titleMedium
        )
    }
}