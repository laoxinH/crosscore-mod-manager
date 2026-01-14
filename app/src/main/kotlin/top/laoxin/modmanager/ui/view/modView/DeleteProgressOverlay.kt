package top.laoxin.modmanager.ui.view.modView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.usercase.mod.DeleteStep
import top.laoxin.modmanager.domain.usercase.mod.IntegratedPackageInfo
import top.laoxin.modmanager.ui.state.DeleteCheckState
import top.laoxin.modmanager.ui.state.DeleteProgressState
import top.laoxin.modmanager.ui.state.DeleteResultState
import top.laoxin.modmanager.ui.theme.ExpressiveButton
import top.laoxin.modmanager.ui.theme.ExpressiveOutlinedButton
import top.laoxin.modmanager.ui.theme.ExpressiveTextButton

/** MOD 删除进度覆盖层 */
@Composable
fun DeleteProgressOverlay(
    modifier: Modifier = Modifier,
    progressState: DeleteProgressState?,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
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
                    modifier
                        .fillMaxSize()
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
                        ) { /* 消费点击事件 */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
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
                            ErrorContent(
                                error = state.error,
                                onDismiss = onDismiss
                            )
                        }

                        state.result != null -> {
                            ResultContent(
                                result = state.result,
                                onDismiss = onDismiss,
                                onDisableMod = onDisableMod,
                                onDisableAllMods = onDisableAllMods
                            )
                        }

                        else -> {
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

/** 进度内容 */
@Composable
private fun ProgressContent(state: DeleteProgressState, onCancel: () -> Unit) {
    val animatedProgress by
    animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "🗑️ " + stringResource(R.string.delete_progress_title),
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
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 进度文字
        Text(
            text =
                stringResource(
                    R.string.delete_progress_current,
                    state.current,
                    state.total
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 步骤描述
        Text(
            text =
                when (state.step) {
                    DeleteStep.AUTHENTICATING ->
                        stringResource(R.string.delete_step_authenticating)

                    DeleteStep.COLLECTING_PATHS ->
                        stringResource(R.string.delete_step_collecting)

                    DeleteStep.FILTERING ->
                        stringResource(R.string.delete_step_filtering)

                    DeleteStep.DELETING ->
                        stringResource(R.string.delete_step_deleting)

                    DeleteStep.COMPLETED ->
                        stringResource(R.string.delete_step_completed)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 取消按钮
        ExpressiveOutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.delete_button_cancel))
        }
    }
}

/** 错误内容 */
@Composable
private fun ErrorContent(
    error: top.laoxin.modmanager.domain.model.AppError,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.delete_error_delete_failed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        ExpressiveOutlinedButton(onClick = onDismiss) {
            Text(stringResource(R.string.delete_button_dismiss))
        }
    }
}

/** 结果内容 */
@Composable
private fun ResultContent(
    result: DeleteResultState,
    onDismiss: () -> Unit,
    onDisableMod: (ModBean) -> Unit,
    onDisableAllMods: (List<ModBean>) -> Unit
) {
    var showDeleted by remember { mutableStateOf(false) }
    var showDeletedEnabled by remember { mutableStateOf(false) }
    var showSkippedIntegrated by remember { mutableStateOf(false) }
    var showFailed by remember { mutableStateOf(false) }
    // 跟踪全局关闭按钮状态
    val showAllDisableButtonState = remember { mutableStateOf(true) }
    // 跟踪已关闭的MOD数量
    var disabledCount by remember { mutableStateOf(0) }
    val totalEnabledMods = result.deletedEnabledMods.size

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 成功图标
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 删除成功数量
        Text(
            text = stringResource(R.string.delete_result_success, result.deletedCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 已删除的MOD列表（普通MOD）
        if (result.deletedMods.isNotEmpty()) {
            ExpandableSection(
                title = stringResource(
                    R.string.delete_result_deleted_mods,
                    result.deletedMods.size
                ),
                icon = Icons.Outlined.DeleteOutline,
                iconColor = MaterialTheme.colorScheme.primary,
                expanded = showDeleted,
                onToggle = { showDeleted = !showDeleted }
            ) {
                ModListSection(
                    mods = result.deletedMods,
                    hint = stringResource(R.string.delete_result_deleted_mods_hint)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 已删除的已启用 MOD（物理文件已删除，数据库保留）
        if (result.deletedEnabledMods.isNotEmpty()) {
            ExpandableModListWithDisable(
                title = stringResource(
                    R.string.delete_result_deleted_enabled,
                    result.deletedEnabledMods.size
                ),
                mods = result.deletedEnabledMods,
                hint = stringResource(R.string.delete_result_deleted_enabled_hint),
                expanded = showDeletedEnabled,
                onToggle = { showDeletedEnabled = !showDeletedEnabled },
                iconColor = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Outlined.Lock,
                onDisableMod = { mod ->
                    onDisableMod(mod)
                    disabledCount++
                    // 当所有MOD都被关闭后，自动关闭窗口
                    if (disabledCount >= totalEnabledMods) {
                        onDismiss()
                    }
                },
                showAllDisableButtonState = showAllDisableButtonState
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 跳过整合包 MOD
        if (result.skippedIntegratedMods.isNotEmpty()) {
            ExpandableSection(
                title =
                    stringResource(
                        R.string.delete_result_skipped_integrated,
                        result.skippedIntegratedMods.size
                    ),
                icon = Icons.Outlined.Warning,
                iconColor = MaterialTheme.colorScheme.secondary,
                expanded = showSkippedIntegrated,
                onToggle = { showSkippedIntegrated = !showSkippedIntegrated }
            ) {
                ModListSection(
                    mods = result.skippedIntegratedMods,
                    hint =
                        stringResource(
                            R.string
                                .delete_result_skipped_integrated_hint
                        )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 删除失败的 MOD
        if (result.failedMods.isNotEmpty()) {
            ExpandableSection(
                title =
                    stringResource(
                        R.string.delete_result_failed,
                        result.failedMods.size
                    ),
                icon = Icons.Outlined.Error,
                iconColor = MaterialTheme.colorScheme.error,
                expanded = showFailed,
                onToggle = { showFailed = !showFailed }
            ) {
                ModListSection(
                    mods = result.failedMods.map { it.first },
                    hint = stringResource(R.string.delete_result_failed_hint)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮区域：根据是否有已删除的已启用MOD显示不同按钮
        if (result.deletedEnabledMods.isNotEmpty() && showAllDisableButtonState.value) {
            // 有已删除的已启用MOD：显示两个按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 保留开启按钮
                ExpressiveOutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.delete_result_keep_enabled))
                }
                // 关闭所有按钮
                ExpressiveButton(
                    onClick = {
                        showAllDisableButtonState.value = false
                        onDisableAllMods(result.deletedEnabledMods)
                        onDismiss() // 点击关闭所有后同时关闭结果窗口
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.delete_result_disable_all))
                }
            }
        } else {
            // 没有已删除的已启用MOD或全部已关闭：显示确认按钮
            ExpressiveOutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_button_dismiss))
            }
        }
    }
}

/** 可展开区域 */
@Composable
private fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector =
                    if (expanded) Icons.Outlined.ExpandLess
                    else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) { content() }
    }
}

/** MOD 列表区域 */
@Composable
private fun ModListSection(mods: List<ModBean>, hint: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(mods) { mod ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mod.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** 可展开的MOD列表（带关闭按钮）- 用于删除结果页面 */
@Composable
private fun ExpandableModListWithDisable(
    title: String,
    mods: List<ModBean>,
    hint: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDisableMod: (ModBean) -> Unit,
    showAllDisableButtonState: MutableState<Boolean>
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector =
                    if (expanded) Icons.Outlined.ExpandLess
                    else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(mods) { mod ->
                        var showDisableButton by remember { mutableStateOf(true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = mod.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // 关闭按钮
                            if (showDisableButton && showAllDisableButtonState.value) {
                                Text(
                                    text = stringResource(R.string.delete_button_disable_mod),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            showDisableButton = false
                                            onDisableMod(mod)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 删除前确认对话框 - 显示整合包警告 */
@Composable
fun DeleteCheckConfirmDialog(
    checkState: DeleteCheckState?,
    onCancel: () -> Unit,
    onSkipIntegrated: () -> Unit,
    onDeleteAll: () -> Unit,
    onDisableMod: (ModBean) -> Unit,
    onDisableAllMods: (List<ModBean>) -> Unit,
) {
    AnimatedVisibility(
        visible = checkState != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        val showAllDisableButtonState = remember { mutableStateOf(true) }

        checkState?.let { state ->
            val checkResult = state.checkResult
                // 跟踪已关闭的MOD数量
            var disabledCount by remember { mutableStateOf(0) }
            val totalEnabledMods = checkResult.allEnabledMods.size
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
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
                        ) { /* 消费点击事件 */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        ///.fillMaxHeight(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        ),
                    elevation =
                        CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 标题
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text =
                                stringResource(
                                    R.string.delete_check_title
                                ),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 整合包数量提示
                        Text(
                            text =
                                stringResource(
                                    R.string
                                        .delete_check_integrated_count,
                                    checkResult
                                        .integratedPackages
                                        .size
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 可滚动的内容区域
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()

                        ) {
                            // 待删除的 MOD列表
                            if (checkResult.singleMods.isNotEmpty() || checkResult.singleEnabledMods.isNotEmpty()) {
                                item {
                                    DeletionPending(
                                        checkResult.singleMods,
                                        checkResult.singleEnabledMods,
                                        {
                                            disabledCount ++
                                            onDisableMod(it)
                                        },
                                        showAllDisableButtonState
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // 整合包列表
                            checkResult.integratedPackages.forEach { packageInfo ->
                                item {
                                    IntegratedPackageCard(
                                        packageInfo =
                                            packageInfo,
                                        onDisableMod =
                                            {
                                                disabledCount ++
                                                onDisableMod(it)
                                            },
                                        showAllDisableButtonState
                                    )
                                    Spacer(
                                        modifier =
                                            Modifier.height(
                                                8.dp
                                            )
                                    )
                                }
                            }

                            // 已启用MOD警告
                            if (checkResult.hasEnabledMods && disabledCount < totalEnabledMods && showAllDisableButtonState.value) {
                                item {
                                    EnabledModsWarning(
                                        enabledMods =
                                            checkResult
                                                .allEnabledMods,
                                        onDisableAll = {
                                            showAllDisableButtonState.value = false
                                            onDisableAllMods(
                                                checkResult
                                                    .allEnabledMods
                                            )
                                        },
                                        showAllDisableButtonState
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(24.dp))

                                // 按钮区域
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    // 删除所有按钮
                                    ExpressiveButton(
                                        onClick = onDeleteAll,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string
                                                    .delete_button_delete_all
                                            )
                                        )
                                    }
                                    if (checkResult.hasIntegratedPackages) {
                                        // 跳过整合包按钮
                                        ExpressiveOutlinedButton(
                                            onClick = onSkipIntegrated,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                stringResource(
                                                    R.string
                                                        .delete_button_skip_integrated
                                                )
                                            )
                                        }
                                    }


                                    // 取消按钮
                                    ExpressiveTextButton(
                                        onClick = onCancel,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string
                                                    .delete_button_cancel
                                            )
                                        )
                                    }
                                }
                            }
                        }


                    }
                }
            }
        }
    }
}

/** 整合包卡片 */
@Composable
private fun IntegratedPackageCard(
    packageInfo: IntegratedPackageInfo,
    onDisableMod: (ModBean) -> Unit,
    showAllDisableButtonState: MutableState<Boolean>
) {
    var expandedSelected by remember { mutableStateOf(false) }
    var expandedOther by remember { mutableStateOf(false) }
    var expandedEnabled by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                .padding(12.dp)
    ) {
        // 整合包名称
        Text(
            text =
                stringResource(
                    R.string.delete_check_package_name,
                    packageInfo.packageName
                ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text =
                stringResource(
                    R.string.delete_check_package_mods_count,
                    packageInfo.totalCount
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 选中的MOD
        if (packageInfo.selectedMods.isNotEmpty()) {
            ExpandableModList(
                title =
                    stringResource(
                        R.string.delete_check_selected_mods,
                        packageInfo.selectedMods.size
                    ),
                mods = packageInfo.selectedMods,
                expanded = expandedSelected,
                onToggle = { expandedSelected = !expandedSelected },
                iconColor = MaterialTheme.colorScheme.primary,
                showDisableButton = false,
                onDisableMod = onDisableMod,
                showAllDisableButtonState = showAllDisableButtonState
            )
        }

        // 其他MOD（会被一起删除）
        if (packageInfo.otherMods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            ExpandableModList(
                title =
                    stringResource(
                        R.string.delete_check_other_mods,
                        packageInfo.otherMods.size
                    ),
                mods = packageInfo.otherMods,
                expanded = expandedOther,
                onToggle = { expandedOther = !expandedOther },
                iconColor = MaterialTheme.colorScheme.tertiary,
                showDisableButton = false,
                onDisableMod = onDisableMod,
                showAllDisableButtonState = showAllDisableButtonState
            )
        }

        // 已启用的MOD
        if (packageInfo.enabledMods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            ExpandableModList(
                title =
                    stringResource(
                        R.string.delete_check_enabled_mods,
                        packageInfo.enabledMods.size
                    ),
                mods = packageInfo.enabledMods,
                expanded = expandedEnabled,
                onToggle = { expandedEnabled = !expandedEnabled },
                iconColor = MaterialTheme.colorScheme.error,
                showDisableButton = true,
                onDisableMod = onDisableMod,
                showAllDisableButtonState = showAllDisableButtonState
            )
        }
    }
}

/** 可展开的MOD列表 */
@Composable
private fun ExpandableModList(
    title: String,
    mods: List<ModBean>,
    expanded: Boolean,
    onToggle: () -> Unit,
    iconColor: Color,
    showDisableButton: Boolean,
    onDisableMod: (ModBean) -> Unit,
    showAllDisableButtonState: MutableState<Boolean>
) {

    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector =
                    if (expanded) Icons.Outlined.ExpandLess
                    else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = iconColor,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 20.dp)) {
                mods.forEach { mod ->
                    var showDisableButtonState by remember { mutableStateOf(showDisableButton) }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• ${mod.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (showDisableButtonState && mod.isEnable && showAllDisableButtonState.value) {
                            Text(
                                text =
                                    stringResource(
                                        R.string
                                            .delete_button_disable_mod
                                    ),
                                style =
                                    MaterialTheme.typography
                                        .labelSmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .primary,
                                modifier =
                                    Modifier
                                        .clickable {
                                            showDisableButtonState = false
                                            onDisableMod(
                                                mod
                                            )
                                        }
                                        .padding(
                                            horizontal =
                                                4.dp,
                                            vertical =
                                                2.dp
                                        )
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 待删除的MOD确认区域 */
@Composable
private fun DeletionPending(
    delMods: List<ModBean>,
    delEnableMods: List<ModBean>,
    onDisableMod: (ModBean) -> Unit,
    showAllDisableButtonState: MutableState<Boolean>
) {
    var expandedSelected by remember { mutableStateOf(false) }
    var expandedEnabled by remember { mutableStateOf(false) }


    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                .padding(12.dp)
    ) {
        // 整合包名称
        Text(
            text =
                stringResource(
                    R.string.delete_check_deletion_Pending,
                    delMods.size
                ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )



        Spacer(modifier = Modifier.height(8.dp))

        // 选中的MOD
        if (delMods.isNotEmpty()) {
            ExpandableModList(
                title =
                    stringResource(
                        R.string.delete_check_selected_mods,
                        delMods.size
                    ),
                mods = delMods,
                expanded = expandedSelected,
                onToggle = { expandedSelected = !expandedSelected },
                iconColor = MaterialTheme.colorScheme.primary,
                showDisableButton = false,
                onDisableMod = {},
                showAllDisableButtonState
            )
        }

        // 已启用的MOD
        if (delEnableMods.isNotEmpty()) {

            Spacer(modifier = Modifier.height(4.dp))
            ExpandableModList(
                title =
                    stringResource(
                        R.string.delete_check_enabled_mods,
                        delEnableMods.size
                    ),
                mods = delEnableMods,
                expanded = expandedEnabled,
                onToggle = { expandedEnabled = !expandedEnabled },
                iconColor = MaterialTheme.colorScheme.error,

                showDisableButton = true,
                onDisableMod = onDisableMod,
                showAllDisableButtonState
            )
        }


    }
}

/** 已启用MOD警告区域 */
@Composable
private fun EnabledModsWarning(
    enabledMods: List<ModBean>,
    onDisableAll: () -> Unit,
    showAllDisableButtonState: MutableState<Boolean>
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
                .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text =
                    stringResource(
                        R.string.delete_check_enabled_warning,
                        enabledMods.size
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            // 全部关闭按钮
            if (showAllDisableButtonState.value) {
                Text(
                    text = stringResource(R.string.delete_button_disable_all),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clickable { onDisableAll() }
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.delete_check_enabled_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
