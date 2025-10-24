package top.laoxin.modmanager.ui.view.modView

import android.icu.text.SimpleDateFormat
import android.util.Log
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import top.laoxin.modmanager.R
import top.laoxin.modmanager.data.bean.ModBean
import top.laoxin.modmanager.tools.LogTools.logRecord
import top.laoxin.modmanager.ui.theme.ExpressiveAssistChip
import top.laoxin.modmanager.ui.view.commen.DialogCommon
import top.laoxin.modmanager.ui.viewmodel.ModViewModel
import java.io.File
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
    DialogCommon(
        title = stringResource(id = R.string.dialog_del_mod_title),
        content = stringResource(id = R.string.dialog_del_mod_content),
        onConfirm = { viewModel.deleteMod() },
        onCancel = { viewModel.setShowDelModDialog(false) },
        showDialog = uiState.showDelModDialog
    )

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
                    showButton = false
                    val context = LocalContext.current
                    LabelAndIconButtonGroup(
                        label = R.string.mod_page_mod_detail_dialog_detali_perview,
                        icon = Icons.Filled.Image,
                        showButton = true,
                        viewModel = viewModel
                    )

                    // 使用优化的图片加载
                    val imageBitmaps = remember { mutableStateListOf<ImageBitmap>() }
                    val loadedImages = remember { mutableStateListOf<String>() }

                    LaunchedEffect(mod.images) {
                        if (mod.images == loadedImages) return@LaunchedEffect
                        
                        imageBitmaps.clear()
                        loadedImages.clear()
                        
                        mod.images.forEach { path ->
                            val normalizedPath = path.replace("//", "/")
                            if (File(normalizedPath).exists()) {
                                loadImageBitmapWithCache(context, normalizedPath, 1024, 1024)?.let {
                                    imageBitmaps.add(it)
                                    loadedImages.add(path)
                                }
                            } else {
                                Log.e("ModDetail", "图片文件不存在: $path")
                                logRecord("图片文件不存在: $path")
                                
                                viewModel.flashModImage(mod)
                                
                                Log.e("ModDetail", "图片文件已重新解压: $path")
                                logRecord("图片文件已重新解压: $path")
                                
                                loadImageBitmapWithCache(context, normalizedPath, 1024, 1024)?.let {
                                    imageBitmaps.add(it)
                                    loadedImages.add(path)
                                }
                            }
                        }
                    }

                    // 显示图片轮播
                    if (imageBitmaps.isNotEmpty()) {
                        ImageCarouselWithIndicator(imageBitmaps)
                    }
                }



                LabelAndIconButtonGroup(
                    label = R.string.mod_page_mod_detail_dialog_detali_title,
                    icon = Icons.Filled.Settings,
                    showButton = showButton,
                    viewModel = viewModel
                )
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
                                R.string.mod_page_mod_detail_dialog_detali_create_time,
                                formatTimestamp(mod.date)
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

                        mod.password?.let {
                            Text(
                                text = stringResource(
                                    R.string.mod_page_mod_detail_dialog_detali_password,
                                    mod.password
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        mod.images?.size?.let {
                            if (it != 0)
                                Text(
                                    text = stringResource(
                                        R.string.mod_page_mod_detail_dialog_detali_imgs,
                                        it
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                        }

                        mod.images?.joinToString { it }?.let {
                            if (it.isNotBlank() && it.isNotEmpty())
                                Text(
                                    text = stringResource(
                                        R.string.mod_page_mod_detail_dialog_detali_path_images,
                                        it
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                        }

                    }
                }
                LabelAndIconButtonGroup(
                    label = R.string.mod_page_mod_detail_dialog_detali_readme,
                    icon = Icons.Filled.AttachFile,
                    showButton = false,
                    viewModel = viewModel
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    MarkdownText(
                        mod.description ?: stringResource(R.string.mod_bean_no_readme),
                        Modifier.padding(16.dp),
                        style = TextStyle(
                            fontSize = 14.sp,
                            //lineHeight = 10.sp,
                            textAlign = TextAlign.Justify,
                        ),
                    )
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
            .clip(MaterialTheme.shapes.medium)
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
        ExpressiveAssistChip(
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
            ) {
                IconButton(onClick = {
                    viewModel.refreshModDetail()
                }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    viewModel.deleteMod()
                }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "delete",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun Markdown(markdown: String) {
    MarkdownText(markdown)
}

// 创建实时预览
@Preview(showBackground = true)
@Composable
fun PreviewModDetailPartialBottomSheet() {
    // 显示一个文本输入框
    Markdown(
        "### 测试" +
                "" +
                "/n/n" +
                "你好"
    )
}


