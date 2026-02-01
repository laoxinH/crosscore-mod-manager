package top.laoxin.modmanager.ui.view.components.mod

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.io.File
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.service.EnableStep
import top.laoxin.modmanager.ui.state.EnableProgressState
import top.laoxin.modmanager.ui.state.EnableResultState
import top.laoxin.modmanager.ui.theme.ExpressiveOutlinedButton
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton

/** MOD 开启进度覆盖层 */
@Composable
fun EnableProgressOverlay(
        modifier: Modifier = Modifier,
        progressState: EnableProgressState?,
        onCancel: () -> Unit,
        onDismiss: () -> Unit,
        onGoSettings: () -> Unit = {},
        onGrantPermission: () -> Unit = {},
        onDisableMod: (ModBean) -> Unit = {},
        onRemoveFromSelection: (ModBean) -> Unit = {},
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
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                                    .clickable(
                                            indication = null,
                                            interactionSource =
                                                    remember { MutableInteractionSource() }
                                    ) { /* 消费点击事件 */},
                    contentAlignment = Alignment.Center
            ) {
                Card(
                        modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    when {
                        state.error != null -> {
                            ErrorContent(
                                    error = state.error,
                                    onDismiss = onDismiss,
                                    onGoSettings = onGoSettings,
                                    onGrantPermission = onGrantPermission
                            )
                        }
                        state.result != null -> {
                            ResultContent(
                                    result = state.result,
                                    onDismiss = onDismiss,
                                    onDisableMod = onDisableMod,
                                    onRemoveFromSelection = onRemoveFromSelection
                            )
                        }
                        else -> {
                            ProgressContent(state = state, onCancel = onCancel)
                        }
                    }
                }
            }
        }
    }
}

/** 进度内容 */
@Composable
private fun ProgressContent(state: EnableProgressState, onCancel: () -> Unit) {
    val animatedProgress by
            animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 300),
                    label = "progress"
            )

    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 标题
        Text(
                text = "⚡ " + stringResource(R.string.enable_progress_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 当前 MOD 名称
        if (state.modName.isNotEmpty()) {
            Text(
                    text = state.modName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 进度条
        /*if (state.subProgress < 0) {
            LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {*/
        LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        // }

        // 百分比
        /*if (state.subProgress >= 0) {*/
        Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
        )
        /*     } else {
            Text(
                    text = stringResource(R.string.enable_progress_processing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
            )
        }*/

        Spacer(modifier = Modifier.height(16.dp))

        // 步骤详情区域
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 步骤描述
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

            // 子进度
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

        // 进度统计
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
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text =
                            stringResource(
                                    R.string.enable_progress_count,
                                    state.current,
                                    state.total
                            ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 取消按钮
        ExpressiveOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                    if (state.step == EnableStep.CANCELING)
                            stringResource(R.string.enable_progress_canceling)
                    else stringResource(R.string.enable_progress_cancel)
            )
        }
    }
}

/** 结果内容 */
@Composable
private fun ResultContent(
        result: EnableResultState,
        onDismiss: () -> Unit,
        onDisableMod: (ModBean) -> Unit = {},
        onRemoveFromSelection: (ModBean) -> Unit = {}
) {
    // 是否展开冲突详情
    var isConflictExpanded by remember { mutableStateOf(false) }
    
    // 是否展开失败详情
    var isFailedExpanded by remember { mutableStateOf(false) }

    // 追踪已移除的MOD ID（用于从列表中移除已处理的项目）
    var removedMutualConflictIds by remember { mutableStateOf(setOf<Int>()) }
    var removedEnabledConflictIds by remember { mutableStateOf(setOf<Int>()) }

    // 过滤掉已移除的MOD
    val remainingMutualConflictMods =
            remember(result.mutualConflictMods, removedMutualConflictIds) {
                result.mutualConflictMods.filter { it.id !in removedMutualConflictIds }
            }
    val remainingEnabledConflictMods =
            remember(result.enabledConflictMods, removedEnabledConflictIds) {
                result.enabledConflictMods.filter { it.id !in removedEnabledConflictIds }
            }

    val hasConflicts =
            remainingMutualConflictMods.isNotEmpty() || remainingEnabledConflictMods.isNotEmpty()
    
    // 检查是否有失败的MOD
    val hasFailedMods = result.backupFailedMods.isNotEmpty() ||
            result.enableFailedMods.isNotEmpty() ||
            result.restoreFailedMods.isNotEmpty()

    Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                text = stringResource(R.string.enable_result_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 统计信息
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatRow(
                    icon = Icons.Outlined.CheckCircle,
                    label = stringResource(R.string.enable_result_success),
                    value = result.enabledCount.toString()
            )
            if (result.needPasswordMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Lock,
                        label = stringResource(R.string.enable_result_need_password),
                        value = result.needPasswordMods.size.toString()
                )
            }
            if (result.fileMissingMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Folder,
                        label = stringResource(R.string.enable_result_file_missing),
                        value = result.fileMissingMods.size.toString()
                )
            }
            if (result.backupFailedMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Error,
                        label = stringResource(R.string.enable_result_backup_failed),
                        value = result.backupFailedMods.size.toString()
                )
            }
            if (result.enableFailedMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Block,
                        label = stringResource(R.string.enable_result_enable_failed),
                        value = result.enableFailedMods.size.toString()
                )
            }
            if (result.restoreFailedMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Error,
                        label = stringResource(R.string.enable_result_restore_failed),
                        value = result.restoreFailedMods.size.toString()
                )
            }
            if (result.mutualConflictMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Block,
                        label = stringResource(R.string.enable_result_mutual_conflict),
                        value = result.mutualConflictMods.size.toString()
                )
            }
            if (result.enabledConflictMods.isNotEmpty()) {
                StatRow(
                        icon = Icons.Outlined.Block,
                        label = stringResource(R.string.enable_result_enabled_conflict),
                        value = result.enabledConflictMods.size.toString()
                )
            }
        }

        // 失败详情展开区域
        if (hasFailedMods) {
            Spacer(modifier = Modifier.height(12.dp))

            // 展开/折叠按钮
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { isFailedExpanded = !isFailedExpanded }
                                    .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text =
                                if (isFailedExpanded)
                                        stringResource(R.string.enable_result_hide_failed_details)
                                else stringResource(R.string.enable_result_show_failed_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                        imageVector =
                                if (isFailedExpanded) Icons.Outlined.ExpandLess
                                else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isFailedExpanded) {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 备份失败列表
                    if (result.backupFailedMods.isNotEmpty()) {
                        FailedModSection(
                                title = stringResource(R.string.enable_result_backup_failed),
                                failedMods = result.backupFailedMods
                        )
                    }

                    // 开启失败列表
                    if (result.enableFailedMods.isNotEmpty()) {
                        FailedModSection(
                                title = stringResource(R.string.enable_result_enable_failed),
                                failedMods = result.enableFailedMods
                        )
                    }

                    // 还原失败列表
                    if (result.restoreFailedMods.isNotEmpty()) {
                        FailedModSection(
                                title = stringResource(R.string.enable_result_restore_failed),
                                failedMods = result.restoreFailedMods
                        )
                    }
                }
            }
        }

        // 冲突详情展开区域
        if (hasConflicts) {
            Spacer(modifier = Modifier.height(12.dp))

            // 展开/折叠按钮
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { isConflictExpanded = !isConflictExpanded }
                                    .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text =
                                if (isConflictExpanded)
                                        stringResource(R.string.enable_result_hide_conflict_details)
                                else stringResource(R.string.enable_result_show_conflict_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                        imageVector =
                                if (isConflictExpanded) Icons.Outlined.ExpandLess
                                else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isConflictExpanded) {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 与已开启MOD冲突列表
                    if (remainingEnabledConflictMods.isNotEmpty()) {
                        ConflictSection(
                                title =
                                        stringResource(
                                                R.string.enable_result_enabled_conflict_title
                                        ),
                                hint = stringResource(R.string.enable_result_enabled_conflict_hint),
                                mods = remainingEnabledConflictMods,
                                actionLabel = stringResource(R.string.scan_result_disable_mod),
                                onAction = { mod ->
                                    onDisableMod(mod)
                                    removedEnabledConflictIds = removedEnabledConflictIds + mod.id
                                }
                        )
                    }

                    // 内部冲突列表
                    if (remainingMutualConflictMods.isNotEmpty()) {
                        ConflictSection(
                                title =
                                        stringResource(
                                                R.string.enable_result_mutual_conflict_title
                                        ),
                                hint = stringResource(R.string.enable_result_mutual_conflict_hint),
                                mods = remainingMutualConflictMods,
                                actionLabel =
                                        stringResource(R.string.enable_result_remove_selection),
                                onAction = { mod ->
                                    onRemoveFromSelection(mod)
                                    removedMutualConflictIds = removedMutualConflictIds + mod.id
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ExpressiveTextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dialog_button_confirm))
        }
    }
}

/** 冲突区域 */
@Composable
private fun ConflictSection(
        title: String,
        hint: String,
        mods: List<ModBean>,
        actionLabel: String,
        onAction: (ModBean) -> Unit
) {
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
    ) {
        // 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 提示
        Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // MOD列表
        LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = mods, key = { it.id }) { mod ->
                ConflictModItem(mod = mod, actionLabel = actionLabel, onAction = { onAction(mod) })
            }
        }
    }
}

/** 冲突MOD项 */
@Composable
private fun ConflictModItem(mod: ModBean, actionLabel: String, onAction: () -> Unit) {
    val iconPath = remember(mod.icon, mod.updateAt) { mod.icon?.takeIf { File(it).exists() } }
    val imageBitmap by
            rememberImageBitmap(path = iconPath, reqWidth = 48, reqHeight = 48, key = mod.updateAt)

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // MOD图标
        Box(
                modifier =
                        Modifier.size(40.dp)
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
            }
                    ?: Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                    )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // MOD信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = mod.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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

        // 操作按钮
        ExpressiveOutlinedButton(onClick = onAction, modifier = Modifier.height(32.dp)) {
            Text(text = actionLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** 失败MOD区域 */
@Composable
private fun FailedModSection(
        title: String,
        failedMods: List<Pair<ModBean, AppError?>>
) {
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
    ) {
        // 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    imageVector = Icons.Outlined.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MOD列表
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            failedMods.forEach { (mod, error) ->
                FailedModItem(mod = mod, error = error)
            }
        }
    }
}

/** 失败MOD项 */
@Composable
private fun FailedModItem(mod: ModBean, error: AppError?) {
    val iconPath = remember(mod.icon, mod.updateAt) { mod.icon?.takeIf { File(it).exists() } }
    val imageBitmap by
            rememberImageBitmap(path = iconPath, reqWidth = 48, reqHeight = 48, key = mod.updateAt)

    // 是否展开错误详情
    var isExpanded by remember { mutableStateOf(false) }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // MOD图标
            Box(
                    modifier =
                            Modifier.size(40.dp)
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
                }
                        ?: Image(
                                painter = painterResource(id = R.drawable.app_icon),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // MOD信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = mod.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // 查看详情按钮
            if (error != null) {
                Text(
                        text = stringResource(R.string.enable_result_view_details),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                                Modifier.clickable { isExpanded = !isExpanded }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 错误详情展开区域
        AnimatedVisibility(visible = isExpanded && error != null) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(
                                            MaterialTheme.colorScheme.errorContainer.copy(
                                                    alpha = 0.5f
                                            ),
                                            RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp)
            ) {
                Text(
                        text = stringResource(R.string.enable_result_error_details),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = getErrorDescription(error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/** 获取错误描述 */
@Composable
private fun getErrorDescription(error: AppError?): String {
    return when (error) {
        is AppError.FileError.FileNotFound -> stringResource(R.string.error_file_not_found)
        is AppError.FileError.Unknown -> error.message
        is AppError.ModError.BackupFailed ->
                stringResource(R.string.error_backup_failed, error.toString())
        is AppError.ModError.EnableFailed ->
                stringResource(R.string.error_enable_failed, error.toString())
        is AppError.ModError.DisableFailed ->
                stringResource(R.string.error_disable_failed, error.toString())
        is AppError.ModError.RestoreFailed ->
                stringResource(R.string.error_restore_failed, error.toString())
        is AppError.ModError.FileMissing ->
                stringResource(R.string.error_mod_file_missing, error.toString())
        is AppError.ModError.WriteFailed -> stringResource(R.string.error_write_failed, error.toString())
        is AppError.ModError.ReadFailed -> stringResource(R.string.error_read_failed, error.toString())
        is AppError.ModError.CopyFailed -> stringResource(R.string.error_copy_failed, error.toString())
        is AppError.ModError.SpecialOperationFailed -> error.toString()
        is AppError.PermissionError -> stringResource(R.string.error_permission_denied)
        null -> stringResource(R.string.error_unknown)
        else -> error.toString()
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
                                stringResource(R.string.error_game_not_installed, error.gameName),
                                null,
                                null
                        )
                is AppError.PermissionError ->
                        Triple(
                                stringResource(R.string.error_permission_denied),
                                stringResource(R.string.error_action_grant_permission),
                                onGrantPermission
                        )
                is AppError.FileError.ShizukuDisconnected ->  Triple(
                    stringResource(R.string.error_shizuku_disconnected),
                    null,
                    null
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
                text = stringResource(R.string.enable_error_title),
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
            ExpressiveOutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionButtonText)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExpressiveTextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_button_confirm))
            }
        } else {
            // 只显示确认按钮
            ExpressiveOutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_button_confirm))
            }
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

/** 获取步骤文本 */
@Composable
private fun getStepText(step: EnableStep, current: Int, total: Int): String {
    return when (step) {
        EnableStep.VALIDATING -> stringResource(R.string.enable_step_validating)
        EnableStep.BACKING_UP -> stringResource(R.string.enable_step_backing_up)
        EnableStep.ENABLING -> stringResource(R.string.enable_step_enabling, current, total)
        EnableStep.DISABLING -> stringResource(R.string.enable_step_disabling, current, total)
        EnableStep.SPECIAL_PROCESS -> stringResource(R.string.enable_step_special_process)
        EnableStep.UPDATING_DB -> stringResource(R.string.enable_step_updating_db)
        EnableStep.COMPLETE -> stringResource(R.string.enable_step_complete)
        EnableStep.CANCELING -> stringResource(R.string.enable_step_canceling)
    }
}
