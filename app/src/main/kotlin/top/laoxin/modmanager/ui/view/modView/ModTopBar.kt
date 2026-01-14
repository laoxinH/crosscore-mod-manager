package top.laoxin.modmanager.ui.view.modView

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.laoxin.modmanager.R

import top.laoxin.modmanager.ui.theme.ExpressiveButton
import top.laoxin.modmanager.ui.theme.ExpressiveTextField

import top.laoxin.modmanager.ui.view.common.fuzzyContains
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModListViewModel
import top.laoxin.modmanager.ui.viewmodel.ModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModTopBar(
    modListViewModel: ModListViewModel,
    modBrowserViewModel: ModBrowserViewModel,
    modOperationViewModel: ModOperationViewModel,
    modSearchViewModel: ModSearchViewModel,
    modScanViewModel: ModScanViewModel,
    modifier: Modifier = Modifier,
    configuration: Int
) {
    // 在这里获取其他需要的ViewModel
//    val modListViewModel: ModListViewModel = hiltViewModel()
//    val modBrowserViewModel: ModBrowserViewModel = hiltViewModel()
//    val modOperationViewModel: ModOperationViewModel = hiltViewModel()
    val modListUiState by modListViewModel.uiState.collectAsState()
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val modBrowserUiState by modBrowserViewModel.uiState.collectAsState()
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    // 收集状态
    val isMultiSelect = modListUiState.isMultiSelect
    val modsView = modBrowserUiState.modsView
    val showDelSelectModsDialog = modOperationUiState.showDelSelectModsDialog

    if (isMultiSelect) {

        MultiSelectTopBar(
            modListViewModel = modListViewModel,
            modSearchViewModel = modSearchViewModel,
            modBrowserViewModel = modBrowserViewModel,
            modOperationViewModel = modOperationViewModel,
            modifier = modifier,
            configuration = configuration
        )
    } else {
        GeneralTopBar(
            modSearchViewModel = modSearchViewModel,
            modScanViewModel = modScanViewModel,
            modListViewModel = modListViewModel,
            modBrowserViewModel = modBrowserViewModel,
            modifier = modifier,
            configuration = configuration
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    modListViewModel: ModListViewModel,
    modSearchViewModel: ModSearchViewModel,
    modBrowserViewModel: ModBrowserViewModel,
    modOperationViewModel: ModOperationViewModel,
    modifier: Modifier,
    configuration: Int
) {
    val modListUiState by modListViewModel.uiState.collectAsState()
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val modBrowserUiState by modBrowserViewModel.uiState.collectAsState()
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    // 收集状态
    val modList = modListUiState.modList
    val enableModList = modListUiState.enableModList
    val disableModList = modListUiState.disableModList
    val currentBrowserMods = modBrowserUiState.currentMods
    val modsSelected = modListUiState.modsSelected
    val modsView = modBrowserUiState.modsView
    val searchBoxVisible = modSearchUiState.searchBoxVisible
    val searchQuery = modSearchUiState.searchContent
    val currentMods = remember(modsView, searchQuery) {
        when (modsView) {
            NavigationIndex.ALL_MODS -> modList
            NavigationIndex.ENABLE_MODS -> enableModList
            NavigationIndex.DISABLE_MODS -> disableModList
            NavigationIndex.SEARCH_MODS -> modList
            NavigationIndex.MODS_BROWSER -> currentBrowserMods
        }
    }

    val currentModList = remember(currentMods, searchQuery) {
        currentMods.filter { searchQuery.isBlank() || it.name.fuzzyContains(searchQuery) }
    }
    Column {
        TopAppBar(
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (configuration == Configuration.ORIENTATION_LANDSCAPE) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
            ),
            title = {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = if (modsView == NavigationIndex.MODS_BROWSER)
                        Modifier.padding(start = 6.dp)
                    else
                        Modifier
                ) {
                    Text(
                        stringResource(id = modsView.title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        Row(
                            modifier = Modifier.padding(top = 40.dp),
                        ) {
                            val total =
                                if (modsSelected.isNotEmpty()) "${modsSelected.size}/${currentModList.size}"
                                else "${currentModList.size}"

                            Text(
                                text = stringResource(R.string.mod_top_bar_count, total),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            },
            actions = {
                // 全选
                IconButton(onClick = {
                    modListViewModel.allSelect(currentModList)
                }) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select All"
                    )
                }
                // 取消选择
                IconButton(onClick = {
                    modListViewModel.deselect()
                }) {
                    Icon(
                        imageVector = Icons.Default.Deselect,
                        contentDescription = "Deselect"
                    )
                }
                // 启用选中的Mod
                IconButton(onClick = {
                    val selectedMods = modListViewModel.getSelectableModsForSwitch(true)
                    modOperationViewModel.switchSelectMods(selectedMods, true)
                }) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Enable"
                    )
                }
                // 禁用选中的Mod
                IconButton(onClick = {
                    val selectedMods = modListViewModel.getSelectableModsForSwitch(false)
                    modOperationViewModel.switchSelectMods(selectedMods, false)
                }) {
                    Icon(
                        imageVector = Icons.Filled.FlashOff,
                        contentDescription = "Disable"
                    )
                }
                // 删除选中的Mod
                IconButton(onClick = {
                    //modOperationViewModel.setShowDelSelectModsDialog(true)

                   modOperationViewModel.checkAndDeleteMods(modsSelected.map {id-> modList.find { it.id ==  id } }.mapNotNull{ it})
                }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete"
                    )
                }
                // 退出多选模式
                IconButton(onClick = {
                    modListViewModel.exitSelect()
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            })

        // 显示或隐藏搜索框
        AnimatedVisibility(visible = searchBoxVisible) {
            SearchBox(
                text = modSearchUiState.searchContent,
                onValueChange = { modSearchViewModel.setSearchText(it) },
                hint = stringResource(R.string.mod_page_search_hit),
                visible = searchBoxVisible,
                onClose = {
                    modSearchViewModel.setSearchBoxVisible(false)
                    when (modsView) {
                        NavigationIndex.MODS_BROWSER -> modBrowserViewModel.setModsView(
                            NavigationIndex.MODS_BROWSER
                        )

                        else -> modBrowserViewModel.setModsView(NavigationIndex.ALL_MODS)
                    }
                    modSearchViewModel.setSearchText("")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralTopBar(
    modSearchViewModel: ModSearchViewModel,
    modScanViewModel: ModScanViewModel,
    modListViewModel: ModListViewModel,
    modBrowserViewModel: ModBrowserViewModel,
    modifier: Modifier,
    configuration: Int
) {
    val modListUiState by modListViewModel.uiState.collectAsState()
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val modBrowserUiState by modBrowserViewModel.uiState.collectAsState()
    // 收集状态
    val modList = modListUiState.modList
    val enableModList = modListUiState.enableModList
    val disableModList = modListUiState.disableModList
    val searchQuery = modSearchUiState.searchContent
    val currentBrowserMods = modBrowserUiState.currentMods
    val modsSelected = modListUiState.modsSelected
    val modsView = modBrowserUiState.modsView
    val searchBoxVisible = modSearchUiState.searchBoxVisible
    val isBackPathExist = modBrowserUiState.isBackPathExist
    //val showForceScanDialog = modSearchUiState.showForceScanDialog.collectAsState()

    val currentMods = remember(modsView, searchQuery,currentBrowserMods) {
        when (modsView) {
            NavigationIndex.ALL_MODS -> modList
            NavigationIndex.ENABLE_MODS -> enableModList
            NavigationIndex.DISABLE_MODS -> disableModList
            NavigationIndex.SEARCH_MODS -> modList
            NavigationIndex.MODS_BROWSER -> currentBrowserMods
        }
    }

    val currentModList = remember(currentMods, searchQuery) {
        currentMods.filter { searchQuery.isBlank() || it.name.fuzzyContains(searchQuery) }
    }

    var showMenu by remember { mutableStateOf(false) }

    // 添加按钮透明度动画
    val backButtonAlpha by animateFloatAsState(
        targetValue = if (isBackPathExist) 1f else 0.5f,
        label = "backButtonAlpha"
    )

    Column {
        TopAppBar(
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (configuration == Configuration.ORIENTATION_LANDSCAPE) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
            ),
            navigationIcon = {
                if (modsView == NavigationIndex.MODS_BROWSER)
                    ExpressiveButton(
                        onClick = {
                            if (isBackPathExist) {
                                modBrowserViewModel.setDoBackFunction(true)
                            }
                        },
                        modifier = Modifier
                            .size(35.dp)
                            .padding(start = 6.dp)
                            .offset(y = 8.dp)
                            .alpha(backButtonAlpha),
                        contentPadding = PaddingValues(0.dp),
                        enabled = isBackPathExist,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBackIosNew,
                                contentDescription = "back",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                else null
            },
            title = {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = if (modsView == NavigationIndex.MODS_BROWSER)
                        Modifier.padding(start = 6.dp)
                    else
                        Modifier
                ) {
                    Text(
                        stringResource(id = modsView.title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box {
                        Row(
                            modifier = Modifier.padding(top = 40.dp),
                        ) {
                            val total =
                                if (modsSelected.isNotEmpty()) "${modsSelected.size}/${currentModList.size}"
                                else "${currentModList.size}"

                            Text(
                                text = stringResource(R.string.mod_top_bar_count, total),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = {
                    modSearchViewModel.setSearchBoxVisible(true)
                    when (modsView) {
                        NavigationIndex.MODS_BROWSER -> modBrowserViewModel.setModsView(
                            NavigationIndex.MODS_BROWSER
                        )

                        else -> modBrowserViewModel.setModsView(NavigationIndex.SEARCH_MODS)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = {
                    modScanViewModel.flashMods(true, false)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh"
                    )
                }
                IconButton(onClick = {
                    modScanViewModel.setShowForceScanDialog(true)
                }) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = "ForceRefresh"
                    )
                }
                // 列表/网格视图切换按钮
                IconButton(onClick = { modBrowserViewModel.toggleDisplayMode() }) {
                    Icon(
                        imageVector = if (modBrowserUiState.isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = if (modBrowserUiState.isGridView) "List View" else "Grid View"
                    )
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu"
                    )
                }
                AnimatedVisibility(visible = showMenu, modifier = Modifier.offset(y = 20.dp)) {
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_enable_mods)) },
                            onClick = {
                                modBrowserViewModel.setModsView(NavigationIndex.ENABLE_MODS)
                                showMenu = false
                            })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_disable_mods)) },
                            onClick = {
                                modBrowserViewModel.setModsView(NavigationIndex.DISABLE_MODS)
                                showMenu = false
                            })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_all_mods)) },
                            onClick = {
                                modBrowserViewModel.setModsView(NavigationIndex.ALL_MODS)
                                showMenu = false
                            })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_mods_browser)) },
                            onClick = {
                                modBrowserViewModel.setModsView(NavigationIndex.MODS_BROWSER)
                                showMenu = false
                            })
                    }
                }
            }
        )

        // 显示或隐藏搜索框
        AnimatedVisibility(visible = searchBoxVisible) {
            SearchBox(
                text = modSearchUiState.searchContent,
                onValueChange = { modSearchViewModel.setSearchText(it) },
                hint = stringResource(R.string.mod_page_search_hit),
                visible = searchBoxVisible,
                onClose = {
                    modSearchViewModel.setSearchBoxVisible(false)
                    when (modsView) {
                        NavigationIndex.MODS_BROWSER -> modBrowserViewModel.setModsView(
                            NavigationIndex.MODS_BROWSER
                        )

                        else -> modBrowserViewModel.setModsView(NavigationIndex.ALL_MODS)
                    }
                    modSearchViewModel.setSearchText("")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBox(
    text: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val onValueChangeCallback = remember(onValueChange) { onValueChange }
    val onCloseCallback = remember(onClose) { onClose }

    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    val textStyle = MaterialTheme.typography.bodyMedium
    val placeholderStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )


    val shape = MaterialTheme.shapes.medium
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )

    val keyboardActions = remember {
        KeyboardActions(onDone = { keyboardController?.hide() })
    }

    ExpressiveTextField(
        value = text,
        onValueChange = onValueChangeCallback,
        placeholder = { Text(text = hint, style = placeholderStyle) },
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 10.dp)
            .height(50.dp)
            .focusRequester(focusRequester),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onCloseCallback) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭搜索"
                )
            }
        },
        colors = colors
    )
}

