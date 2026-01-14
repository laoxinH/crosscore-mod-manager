package top.laoxin.modmanager.ui.view.modView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.ScanStep
import top.laoxin.modmanager.ui.state.ScanProgressState
import top.laoxin.modmanager.ui.state.ScanResultState
import top.laoxin.modmanager.ui.theme.ExpressiveOutlinedButton
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton


/** 扫描进度覆盖层 显示扫描过程中的实时进度和最终结果 */
@Composable
fun ScanProgressOverlay(
        modifier: Modifier = Modifier,
        progressState: ScanProgressState?,
        onCancel: () -> Unit,
        onDismiss: () -> Unit,
        onGoSettings: () -> Unit = {},
        onGrantPermission: () -> Unit = {},
        onDisableMod: (ModBean) -> Unit = {},
        onDisableAllMods: (List<ModBean>) -> Unit = {},
) {
        AnimatedVisibility(
                visible = progressState != null,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
        ) {
                progressState?.let { state ->
                        Box(
                                modifier =
                                        modifier.fillMaxSize()
                                                .background(
                                                        MaterialTheme.colorScheme.scrim.copy(
                                                                alpha = 0.5f
                                                        )
                                                )
                                                .clickable(
                                                        indication = null,
                                                        interactionSource =
                                                                remember {
                                                                        MutableInteractionSource()
                                                                }
                                                ) { /* 消费点击事件，阻止穿透 */},
                                contentAlignment = Alignment.Center
                        ) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                        when {
                                                state.error != null -> {
                                                        // 显示错误状态
                                                        ErrorContent(
                                                                error = state.error,
                                                                onDismiss = onDismiss,
                                                                onGoSettings = onGoSettings,
                                                                onGrantPermission =
                                                                        onGrantPermission
                                                        )
                                                }
                                                state.result != null -> {
                                                        // 显示完成结果
                                                        ResultContent(
                                                                result = state.result,
                                                                onDismiss = onDismiss,
                                                                onDisableMod = onDisableMod,
                                                                onDisableAllMods = onDisableAllMods
                                                        )
                                                }
                                                else -> {
                                                        // 显示扫描进度
                                                        ProgressContent(
                                                                state = state,
                                                                onCancel = onCancel
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

/** 扫描进度内容 */
@Composable
private fun ProgressContent(state: ScanProgressState, onCancel: () -> Unit) {
        val animatedProgress by
                animateFloatAsState(
                        targetValue = state.progress,
                        animationSpec = tween(durationMillis = 300),
                        label = "progress"
                )

        // Log.d("ScanProgressOverlay", "当前的progress: ${state}")
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // 标题
                Text(
                        text = "🔍 " + stringResource(R.string.scan_progress_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 当前扫描的源文件（压缩包/文件夹名）
                if (state.sourceName.isNotEmpty()) {
                        Text(
                                text = state.sourceName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 进度条：subProgress < 0 时显示滚动动画，否则显示正常进度
                /*if (state.subProgress < 0) {*/
                // 不确定进度（滚动动画）
                LinearProgressIndicator(
                        modifier =
                                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                /*        } else {
                    // 确定进度
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }*/

                // 百分比（只在有确定进度时显示）
                if (state.subProgress >= 0) {
                        Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                        )
                } else {
                        // 显示处理中提示
                        Text(
                                text = stringResource(R.string.scan_progress_processing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                        )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 子进度区域：显示当前步骤详情
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                ),
                                                RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // 步骤描述（使用 i18n）
                        Text(
                                text = getStepText(state.step, state.current, state.total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                        )

                        // 当前文件名
                        if (state.currentFile.isNotEmpty()) {

                                Text(
                                        text = "📄 ${state.currentFile}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                )
                        }

                        // 子进度百分比
                        if (state.subProgress >= 0) {
                                Text(
                                        text = "${(state.subProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                )
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 发现的MOD数量
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.3f
                                                ),
                                                RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text =
                                        stringResource(
                                                R.string.scan_progress_found_mods,
                                                state.foundModsCount
                                        ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                        )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 取消按钮
                ExpressiveOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.scan_progress_cancel))
                }
        }
}

/** 扫描结果内容 */
@Composable
private fun ResultContent(
        result: ScanResultState,
        onDismiss: () -> Unit,
        onDisableMod: (ModBean) -> Unit = {},
        onDisableAllMods: (List<ModBean>) -> Unit = {}
) {
    var isStatsExpanded by remember { mutableStateOf(result.deletedEnabledMods.isEmpty()) }
    // 追踪已禁用的MOD ID
    var disabledModIds by remember { mutableStateOf(setOf<Int>()) }
    
    // 过滤掉已禁用的MOD
    val remainingMods = remember(result.deletedEnabledMods, disabledModIds) {
        result.deletedEnabledMods.filter { it.id !in disabledModIds }
    }
    
    // 当所有MOD都被禁用后自动关闭
    LaunchedEffect(remainingMods) {
        if (result.deletedEnabledMods.isNotEmpty() && remainingMods.isEmpty()) {
            onDismiss()
        }
    }
    
    Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 成功图标
        Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
                text = stringResource(R.string.scan_result_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 可折叠统计信息
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                        )
                        .clickable { isStatsExpanded = !isStatsExpanded }
                        .padding(16.dp)
        ) {
            // 折叠标题行
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = if (isStatsExpanded) 
                            stringResource(R.string.scan_result_hide_stats) 
                        else 
                            stringResource(R.string.scan_result_show_stats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                )
                Icon(
                        imageVector = if (isStatsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                )
            }
            
            // 展开的统计详情
            AnimatedVisibility(visible = isStatsExpanded) {
                Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow(
                            icon = Icons.Outlined.Inventory2,
                            label = stringResource(R.string.scan_result_total),
                            value = result.scannedCount.toString()
                    )
                    StatRow(
                            icon = Icons.Outlined.Folder,
                            label = stringResource(R.string.scan_result_added),
                            value = "+${result.addedCount}"
                    )
                    StatRow(
                            icon = Icons.Outlined.Folder,
                            label = stringResource(R.string.scan_result_updated),
                            value = "↻${result.updatedCount}"
                    )
                    StatRow(
                            icon = Icons.Outlined.Folder,
                            label = stringResource(R.string.scan_result_deleted),
                            value = "-${result.deletedCount}"
                    )
                    if (result.transferredCount > 0) {
                        StatRow(
                                icon = Icons.Outlined.Folder,
                                label = stringResource(R.string.scan_result_transferred),
                                value = result.transferredCount.toString()
                        )
                    }
                    if (result.skippedCount > 0) {
                        StatRow(
                                icon = Icons.Outlined.Folder,
                                label = stringResource(R.string.scan_result_skipped),
                                value = result.skippedCount.toString()
                        )
                    }
                    if (remainingMods.isNotEmpty()) {
                        StatRow(
                                icon = Icons.Outlined.Error,
                                label = stringResource(R.string.scan_result_deleted_enabled_count),
                                value = remainingMods.size.toString()
                        )
                    }
                }
            }
        }

        // 已删除但启用的MOD警告区域
        if (remainingMods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                    modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
            ) {
                // 警告标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = stringResource(R.string.scan_result_deleted_enabled_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 提示文字
                Text(
                        text = stringResource(R.string.scan_result_deleted_enabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 可滚动的MOD列表
                LazyColumn(
                        modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                            items = remainingMods,
                            key = { it.id }
                    ) { mod ->
                        DeletedEnabledModItem(
                                mod = mod,
                                onDisable = { 
                                    onDisableMod(mod)
                                    disabledModIds = disabledModIds + mod.id
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部按钮区域
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 保留启用按钮
                ExpressiveOutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.scan_result_keep_enabled))
                }
                
                // 关闭所有按钮
                ExpressiveTextButton(
                        onClick = { onDisableAllMods(remainingMods) },
                        modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.scan_result_disable_all))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))

            // 没有已删除启用的MOD时，只显示确定按钮
            ExpressiveTextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_button_confirm))
            }
        }
    }
}

/** 已删除但启用的MOD列表项 */
@Composable
private fun DeletedEnabledModItem(mod: ModBean, onDisable: () -> Unit) {
    // 加载MOD图标
    val iconPath = remember(mod.icon, mod.updateAt) {
        mod.icon?.takeIf { java.io.File(it).exists() }
    }
    val imageBitmap by rememberImageBitmap(path = iconPath, reqWidth = 48, reqHeight = 48, key = mod.updateAt)

    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // MOD图标
        Box(
                modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            imageBitmap?.let {
                Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )
            } ?: run {
                // 默认图标
                Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        // MOD名称
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = mod.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Text(
                    text = mod.path.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 关闭按钮
        ExpressiveOutlinedButton(onClick = onDisable) {
            Text(
                    text = stringResource(R.string.scan_result_disable_mod),
                    style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** 错误内容 */
@Composable
private fun ErrorContent(
        error: AppError,
        onDismiss: () -> Unit,
        onGoSettings: () -> Unit,
        onGrantPermission: () -> Unit
) {
        // 根据错误类型获取消息和操作按钮
        val (errorMessage, actionButtonText, onAction) =
                when (error) {
                        is AppError.GameError.GameNotSelected ->
                                Triple(
                                        stringResource(R.string.error_game_not_selected),
                                        stringResource(R.string.error_action_go_settings),
                                        onGoSettings
                                )
                        is AppError.GameError.GameNotInstalled ->
                                Triple(
                                        stringResource(
                                                R.string.error_game_not_installed,
                                                error.gameName
                                        ),
                                        null,
                                        null
                                )
                        is AppError.PermissionError ->
                                Triple(
                                        stringResource(R.string.error_permission_denied),
                                        stringResource(R.string.error_action_grant_permission),
                                        onGrantPermission
                                )
                        else -> Triple(error.toString(), null, null)
                }

        Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Icon(
                        imageVector = Icons.Outlined.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = stringResource(R.string.scan_error_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 如果有操作按钮，显示两个按钮
                if (actionButtonText != null && onAction != null) {
                        ExpressiveOutlinedButton(
                                onClick = onAction,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text(actionButtonText) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ExpressiveTextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.dialog_button_confirm)) }
                } else {
                        // 只显示确认按钮
                        ExpressiveOutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.dialog_button_confirm)) }
                }
        }
}

/** 统计行 */
@Composable
private fun StatRow(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        value: String
) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
                Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                )
        }
}

/** 根据 ScanStep 枚举获取本地化的步骤描述 */
@Composable
private fun getStepText(step: ScanStep, current: Int, total: Int): String {
        return when (step) {
                ScanStep.CHECKING_ENCRYPTION ->
                        stringResource(R.string.scan_step_checking_encryption)
                ScanStep.LISTING_FILES -> stringResource(R.string.scan_step_listing_files)
                ScanStep.ANALYZING_FILES -> {
                        if (current > 0 && total > 0) {
                                stringResource(R.string.scan_step_analyzing_files, current, total)
                        } else {
                                stringResource(R.string.scan_step_scanning)
                        }
                }
                ScanStep.IDENTIFYING_MODS -> stringResource(R.string.scan_step_identifying_mods)
                ScanStep.CHECKING_FOLDER -> stringResource(R.string.scan_step_checking_folder)
                ScanStep.TRANSFERRING -> stringResource(R.string.scan_step_transferring)
                ScanStep.SCANNING_DIRECTORY -> stringResource(R.string.scan_step_scanning_directory)
                ScanStep.SYNCING_DATABASE -> stringResource(R.string.scan_step_syncing_database)
                ScanStep.COMPLETE -> stringResource(R.string.scan_step_complete)
        }
}
