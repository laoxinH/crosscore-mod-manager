package top.lings.start

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Handler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.laoxin.modmanager.MainActivity
import top.laoxin.modmanager.R

@SuppressLint("AutoboxingStateValueProperty")
@Composable
fun UserAgreement() {
    val context = LocalContext.current

    val isConfirmed = remember { mutableStateOf(false) }
    val hasScrolledToBottom = remember { mutableStateOf(false) }
    val timeSpent = remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    // 使用 Handler 来模拟倒计时
    val handler = remember { Handler(context.mainLooper) }

    // 更新 timeSpent，从启动时开始计数
    DisposableEffect(key1 = Unit) {
        val runnable = object : Runnable {
            override fun run() {
                if (timeSpent.intValue < 20) {
                    timeSpent.value += 1
                    handler.postDelayed(this, 1000L)
                }
            }
        }

        // 启动倒计时
        handler.post(runnable)

        // 在 composable 被移除时清理 Handler
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    // 监听是否滚动到底部
    LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } }) {
        if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1) {
            hasScrolledToBottom.value = true
        }
    }

    // 倒计时和状态更新
    LaunchedEffect(timeSpent.intValue, hasScrolledToBottom.value) {
        if (timeSpent.intValue >= 20 && hasScrolledToBottom.value) {
            isConfirmed.value = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(0.dp)
    ) {
        // 内部的 LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp), state = listState
        ) {
            // 标题
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
            // 重要内容
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
            // 消息内容
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

        // 如果倒计时还没结束，显示剩余时间
        if (!isConfirmed.value) {
            Button(
                onClick = {
                    null
                }, modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            ) {
                Text(
                    text = "${20 - timeSpent.intValue}", style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Button(
                onClick = {
                    // 用户点击确认按钮，跳转到 MainActivity 并记录状态
                    val editor = context.getSharedPreferences("AppLaunch", MODE_PRIVATE).edit()
                    editor.putBoolean("isConfirm", true)
                    editor.apply()

                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    (context as Activity).finish()
                }, modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_button_info_permission),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}