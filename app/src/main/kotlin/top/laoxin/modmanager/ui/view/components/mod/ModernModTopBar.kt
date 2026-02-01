package top.laoxin.modmanager.ui.view.components.mod

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Start
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.viewmodel.ModernModBrowserViewModel
import top.laoxin.modmanager.ui.viewmodel.ModernModListViewModel
import top.laoxin.modmanager.ui.state.ModListFilter
import top.laoxin.modmanager.ui.view.components.common.fuzzyContains
import top.laoxin.modmanager.ui.theme.ExpressiveTextField
import top.laoxin.modmanager.ui.viewmodel.ModOperationViewModel
import top.laoxin.modmanager.ui.viewmodel.ModScanViewModel
import top.laoxin.modmanager.ui.viewmodel.ModSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/*@OptIn(ExperimentalMaterial3Api::class)
@Composable*/
fun ModernModTopBar(
    modListViewModel: ModernModListViewModel,
    modBrowserViewModel: ModernModBrowserViewModel,
    modOperationViewModel: ModOperationViewModel,
    modSearchViewModel: ModSearchViewModel,
    modScanViewModel: ModScanViewModel,
    modifier: Modifier = Modifier,
    configuration: Int
) {
    val modListUiState by modListViewModel.uiState.collectAsState()
    val isBrowser = modListUiState.isBrowser
    val isMultiSelect = modListUiState.isMultiSelect
    
    val modSearchUiState by modSearchViewModel.uiState.collectAsState()
    val modOperationUiState by modOperationViewModel.uiState.collectAsState()
    val modBrowserUiState by modBrowserViewModel.uiState.collectAsState()

    val modList = modListUiState.modList
    val enableModList = modListUiState.enableModList
    val disableModList = modListUiState.disableModList
    val currentBrowserMods = modBrowserUiState.currentMods
    val modsSelected = modListUiState.modsSelected
    val filter = modListUiState.filter
    
    val searchBoxVisible = modSearchUiState.searchBoxVisible
    val searchQuery = modSearchUiState.searchContent
    
    // Shared Data Preparation
    val currentMods = remember(isBrowser, filter, searchQuery, currentBrowserMods,modList) {
        Log.d("ModernModTopBar", "currentMods: $isBrowser, ${currentBrowserMods.size}")
        if (isBrowser) {
            currentBrowserMods
        } else {
             when (filter) {
                ModListFilter.ALL -> modList
                ModListFilter.ENABLE -> enableModList
                ModListFilter.DISABLE -> disableModList
            }
        }
    }

    val currentModList = remember(currentMods, searchQuery) {
        currentMods.filter { searchQuery.isBlank() || it.name.fuzzyContains(searchQuery) }
    }
    
    // Determine Title Resource
    val titleRes = if (isBrowser) {
        R.string.mod_page_title_mods_browser
    } else {
        when (filter) {
            ModListFilter.ALL -> R.string.mod_page_title_all_mods
            ModListFilter.ENABLE -> R.string.mod_page_title_enable_mods
            ModListFilter.DISABLE -> R.string.mod_page_title_disable_mods
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (configuration == Configuration.ORIENTATION_LANDSCAPE)
                        MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceContainer,
            ),
            navigationIcon = {
                // Determine Navigation Icon: Back (Browser) vs None/Close (MultiSelect)
                // If MultiSelect, usually we show "Close" or "X" as nav icon? 
                // Previous design put "Close" in ACTIONS for MultiSelect.
                // General Mode: "Back" if isBrowser.
                // Let's stick to General Mode logic for Nav Icon, as user said "left side fixed".
                // If isMultiSelect, we probably shouldn't show "Back" even if isBrowser?
                // Or maybe we treat MultiSelect as an overlay state?
                
                /* if (isBrowser && !isMultiSelect) {
                    ExpressiveButton(
                        onClick = { modListViewModel.onBackClick() },
                        modifier = Modifier.size(35.dp).padding(start = 6.dp).offset(y = 8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.ArrowBackIosNew, "back", modifier = Modifier.size(16.dp))
                        }
                    }
                }*/
            },
            title = {
                 Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = if (isBrowser) Modifier.padding(start = 6.dp) else Modifier
                ) {
                    Text(
                        stringResource(id = titleRes),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box {
                        Row(modifier = Modifier.padding(top = 40.dp)) {
                            val total = if (modsSelected.isNotEmpty())
                                    "${modsSelected.size}/${currentModList.size}"
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
                // Animate Action Icons
                AnimatedContent(
                    targetState = isMultiSelect,
                    transitionSpec = {
                        if (targetState) {
                             // MultiSelect enters (Slide from right, Scale up)
                            (slideInHorizontally { it } + 
                             scaleIn(initialScale = 0.5f, transformOrigin = TransformOrigin(1f, 0.5f)) + 
                             fadeIn()) togetherWith
                            // General exits (Scale down to right)
                            (scaleOut(targetScale = 0.5f, transformOrigin = TransformOrigin(1f, 0.5f)) + 
                             fadeOut())
                        } else {
                            // General enters (Scale up from right)
                             (scaleIn(initialScale = 0.5f, transformOrigin = TransformOrigin(1f, 0.5f)) + 
                             fadeIn()) togetherWith
                            // MultiSelect exits (Slide out to right, Scale down)
                            (slideOutHorizontally { it } + 
                             scaleOut(targetScale = 0.5f, transformOrigin = TransformOrigin(1f, 0.5f)) + 
                             fadeOut())
                        }
                    },
                    label = "TopBarActionsTransition"
                ) { targetIsMultiSelect ->
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        if (targetIsMultiSelect) {
                            // Multi-Select Actions
                            IconButton(onClick = { modListViewModel.allSelect(currentModList) }) {
                                Icon(Icons.Default.SelectAll, "Select All")
                            }
                            IconButton(onClick = { modListViewModel.deselect() }) {
                                Icon(Icons.Default.Deselect, "Deselect")
                            }
                            IconButton(onClick = {
                                val selectedMods = modListViewModel.getSelectableModsForSwitch(true)
                                modOperationViewModel.switchSelectMods(selectedMods, true)
                            }) { Icon(Icons.Default.FlashOn, "Enable") }
                            IconButton(onClick = {
                                val selectedMods = modListViewModel.getSelectableModsForSwitch(false)
                                modOperationViewModel.switchSelectMods(selectedMods, false)
                            }) { Icon(Icons.Filled.FlashOff, "Disable") }
                            IconButton(onClick = {
                                modOperationViewModel.checkAndDeleteMods(
                                    modsSelected.map { id -> modList.find { it.id == id } }.mapNotNull { it }
                                )
                            }) { Icon(Icons.Filled.Delete, "Delete") }
                            IconButton(onClick = { modListViewModel.exitSelect() }) {
                                Icon(Icons.Filled.Close, "Close")
                            }
                        } else {
                            // General Actions
                            IconButton(onClick = { modSearchViewModel.setSearchBoxVisible(true) }) {
                                Icon(Icons.Filled.Search, "Search")
                            }
                            IconButton(onClick = { modScanViewModel.flashMods(
                                isLoading = true,
                                forceScan = false
                            ) }) {
                                Icon(Icons.Filled.Refresh, "Refresh")
                            }
                            IconButton(onClick = { modScanViewModel.setShowForceScanDialog(true) }) {
                                Icon(Icons.Filled.RestartAlt, "ForceRefresh")
                            }
                            IconButton(onClick = { modBrowserViewModel.toggleDisplayMode() }) {
                                Icon(
                                    imageVector = if (modBrowserUiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                    contentDescription = if (modBrowserUiState.isGridView) "List View" else "Grid View"
                                )
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.Menu, "Menu")
                            }
                        }
                    }
                }
                
                // Dropdown Menu (General Actions Only, strictly speaking, but hoisted out of AnimatedContent to avoid clipping issues if possible? 
                // Typically Menu is anchored to button. If button animates out, menu might close. 
                // Since this is only for General mode, we can keep it inside the if-else block OR
                // keep it here if `showMenu` is controlled by general mode button.
                // Wait, DropdownMenu needs to be in composition tree.
                // If I put it inside `AnimatedContent`, it will disappear when exiting general mode.
                // That is correct.
                
                AnimatedVisibility(visible = showMenu && !isMultiSelect, modifier = Modifier.offset(y = 20.dp)) {
                     DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                         DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_enable_mods)) },
                            onClick = {
                                if (isBrowser) modListViewModel.onNavigateToList()
                                modListViewModel.setFilter(ModListFilter.ENABLE)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_disable_mods)) },
                            onClick = {
                                if (isBrowser) modListViewModel.onNavigateToList()
                                modListViewModel.setFilter(ModListFilter.DISABLE)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_show_all_mods)) },
                            onClick = {
                                if (isBrowser) modListViewModel.onNavigateToList()
                                modListViewModel.setFilter(ModListFilter.ALL)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mod_page_dropdownMenu_mods_browser)) },
                            onClick = {
                                if (!isBrowser) modListViewModel.onNavigateToBrowser()
                                showMenu = false
                            }
                        )
                     }
                }
            }
        )

        // Search Box (General Actions related, but can be overlay)
        AnimatedVisibility(visible = searchBoxVisible) {
            SearchBox(
                text = modSearchUiState.searchContent,
                onValueChange = { modSearchViewModel.setSearchText(it) },
                hint = stringResource(R.string.mod_page_search_hit),
                visible = searchBoxVisible,
                onClose = {
                    modSearchViewModel.setSearchBoxVisible(false)
                    modSearchViewModel.setSearchText("")
                }
            )
        }

        // Path Header (Only in Browser Mode)
        AnimatedVisibility(visible = isBrowser) {
             val currentPath = modBrowserUiState.currentBrowsingPath ?: modBrowserUiState.currentGameModPath
             val rootPath = modBrowserUiState.currentGameModPath
             PathHeader(
                 currentPath = currentPath, 
                 rootPath = rootPath,
                 onPathClick = { path -> modListViewModel.onNavigateToBrowser(path) },
                 onBackClick = { modListViewModel.onBackClick() }
             )
        }
    }
}

@Composable
private fun PathHeader(
    currentPath: String, 
    rootPath: String,
    onPathClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val relativePath = if (currentPath.startsWith(rootPath)) {
        currentPath.removePrefix(rootPath).trimStart('/').trimStart('\\')
    } else {
        currentPath
    }

    val isRoot = currentPath == rootPath || currentPath.trimEnd('/') == rootPath.trimEnd('/')
    val rootName = stringResource(R.string.mod_page_title_mods_browser)

    val breadcrumbs = remember(currentPath, rootPath, rootName) {
        val list = mutableListOf<Pair<String, String>>()
        list.add(rootName to rootPath)
        if (!isRoot) {
            val parts = relativePath.split('/', '\\').filter { it.isNotEmpty() }
            var accPath = rootPath
            parts.forEach { part ->
                accPath = if (accPath.endsWith(File.separator)) accPath + part else accPath + File.separator + part
                list.add(part to accPath)
            }
        }
        list
    }
    
    // Auto-scroll to end
    val listState = rememberLazyListState()
    LaunchedEffect(breadcrumbs) {
        if (breadcrumbs.isNotEmpty()) {
            listState.animateScrollToItem(breadcrumbs.size * 2) 
        }
    }

    Surface(
        modifier = Modifier,
        color = MaterialTheme.colorScheme.surfaceContainer, // Premium feel
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon with Smart Back Overlay
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = !isRoot) { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                    painter = painterResource(id = R.drawable.folder_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                // Overlay "Return" Arrow if not root
                if (!isRoot) {
                     Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply, // U-shaped arrow
                        contentDescription = "Up",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(14.dp) // Slightly larger for center visibility
                            .align(Alignment.Center) // Center alignment
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Start
            ) {
                itemsIndexed(breadcrumbs) { index, (name, path) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { 
                                        android.util.Log.d("PathHeader", "Click on breadcrumb: name=$name, path=$path")
                                        onPathClick(path) 
                                    }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        
                        // Separator if not last
                        if (index < breadcrumbs.size - 1) {
                            Text(
                                text = " > ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                    }
                }
            }
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
    val placeholderStyle =
        MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

    val shape = MaterialTheme.shapes.medium
    val colors =
        TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            unfocusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

    val keyboardActions = remember { KeyboardActions(onDone = { keyboardController?.hide() }) }

    ExpressiveTextField(
        value = text,
        onValueChange = onValueChangeCallback,
        placeholder = { Text(text = hint, style = placeholderStyle) },
        modifier =
            modifier.fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 10.dp)
                .height(50.dp)
                .focusRequester(focusRequester),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onCloseCallback) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "关闭搜索")
            }
        },
        colors = colors
    )
}
