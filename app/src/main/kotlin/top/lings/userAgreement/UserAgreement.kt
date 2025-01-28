package top.lings.userAgreement

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import top.laoxin.modmanager.MainActivity
import top.laoxin.modmanager.R

@SuppressLint("AutoboxingStateValueProperty")
@Composable
fun UserAgreement() {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val state = remember { UserAgreementState(listState) }

    HandleScrollEvents(state)

    HandleCountdown(state)

    UserAgreementContent(state, context)
}

// 状态管理
class UserAgreementState(val listState: LazyListState) {
    var isConfirmed by mutableStateOf(false)
    var hasScrolledToBottom by mutableStateOf(false)
    var timeSpent by mutableIntStateOf(0)
}

// 处理滚动事件
@Composable
private fun HandleScrollEvents(state: UserAgreementState) {
    LaunchedEffect(state.listState) {
        snapshotFlow { state.listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastItem ->
                if (lastItem == state.listState.layoutInfo.totalItemsCount - 1) {
                    state.hasScrolledToBottom = true
                }
            }
    }
}

// 处理倒计时
@Composable
private fun HandleCountdown(state: UserAgreementState) {
    LaunchedEffect(Unit) {
        coroutineScope {
            while (state.timeSpent < 20) {
                delay(1000L)
                state.timeSpent++
            }
            if (state.timeSpent >= 20) {
                state.isConfirmed = true
            }
        }
    }
}

// UI 组件
@Composable
private fun UserAgreementContent(state: UserAgreementState, context: Context) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(0.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            state = state.listState
        ) {
            // 协议内容
            item {
                Text(
                    text = stringResource(id = R.string.dialog_info_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.dialog_info_important),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.dialog_info_message),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }
        }

        // 确认按钮
        if (!state.isConfirmed) {
            DisabledButton(
                timeLeft = 20 - state.timeSpent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            )
        } else {
            ConfirmedButton(
                onClick = {
                    saveUserAgreement(context)
                    navigateToMainActivity(context)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp),
                state = state
            )
        }
    }
}

// 禁用按钮
// 禁用按钮
@Composable
private fun DisabledButton(
    timeLeft: Int,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {},
        modifier = modifier,
        enabled = false
    ) {
        Text(
            text = timeLeft.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 确认按钮
@Composable
private fun ConfirmedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: UserAgreementState
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = state.hasScrolledToBottom
    ) {
        Text(
            text = stringResource(id = R.string.dialog_button_info_permission),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 存储用户协议状态
private fun saveUserAgreement(context: Context) {
    val editor = context.getSharedPreferences("AppLaunch", MODE_PRIVATE).edit()
    editor.putBoolean("isConfirm", true)
    editor.apply()
}

// 导航到主活动
private fun navigateToMainActivity(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    context.startActivity(intent)
    if (context is Activity) {
        context.finish()
    }
}