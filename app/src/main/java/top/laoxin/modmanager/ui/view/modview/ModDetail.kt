package top.laoxin.modmanager.ui.view.modview

import android.icu.text.SimpleDateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.ui.view.commen.DialogCommon
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModDetailPartialBottomSheet(
    showDialog: Boolean,
    mod: ModBean,
    viewModel: ModViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    DialogCommon(title = stringResource(id = R.string.dialog_del_mod_title),
        content = stringResource(id = R.string.dialog_del_mod_content),
        onConfirm = { viewModel.deleteMod()},
        onCancel = { viewModel.setShowDelModDialog(false) },
        showDialog = uiState.showDelModDialog)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    if (showDialog) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            sheetState = sheetState,
            onDismissRequest = onDismiss
        ) {

            val scrollState = rememberScrollState()
            var showButton = remember { true }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Your existing code...

                Text(
                    text = mod.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // 预览图
                if ((!mod.images.isNullOrEmpty() && !mod.isEncrypted) || (mod.password != null && !mod.images.isNullOrEmpty())) {
                    showButton =false
                    LabelAndIconButtonGroup(
                        label = R.string.mod_page_mod_detail_dialog_detali_perview,
                        icon = Icons.Filled.Image,
                        showButton = true,
                        viewModel = viewModel
                    )
                    val imageBitmaps = mod.images.mapNotNull { path ->
                        createImageBitmapFromPath(path)
                    }
                    ImageCarouselWithIndicator(imageBitmaps)

                }

                LabelAndIconButtonGroup(label = R.string.mod_page_mod_detail_dialog_detali_title, icon = Icons.Filled.Settings, showButton = showButton, viewModel = viewModel)
                // 文本标签

                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_modType,
                                mod.modType ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_modNums,
                                mod.modFiles?.size ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_imgs,
                                mod.images?.size ?: "0"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_version,
                                mod.version ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_author,
                                mod.author ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_create_time,
                                formatTimestamp(mod.date)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_descript,
                                mod.description ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = stringResource(
                                R.string.mod_page_mod_detail_dialog_detali_path,
                                mod.path ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

        }


    }

}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarouselWithIndicator(images: List<ImageBitmap>) {
    if (images.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { images.size })

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) { page ->
            Image(
                bitmap = images[page],
                contentDescription = "Image $page",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            images.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.secondary else CardDefaults.cardColors().containerColor
                        )
                )
            }
        }
    }
}

// 标签和图标按钮组
@Composable
fun LabelAndIconButtonGroup(
    @StringRes label: Int,
    icon: ImageVector,
    showButton: Boolean,
    viewModel: ModViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AssistChip(
            onClick = { },
            label = { Text(stringResource(id = label)) },

            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = "Localized description",
                    Modifier.size(AssistChipDefaults.IconSize)
                )

            }
        )
        // Icon button group
        if (showButton) {
            Row(
                modifier = Modifier
                //.padding(40.dp),
                //horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
               /* IconButton(onClick = { *//* Handle click *//* }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }*/
                IconButton(onClick = { viewModel.deleteMod() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)

                }
                // Add more icons as needed...
            }
        }


    }
}


