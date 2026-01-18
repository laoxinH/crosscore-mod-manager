package top.laoxin.modmanager.ui.view.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.laoxin.modmanager.R

/**
 * 随机启动图片组件
 * 在系统 Splash Screen 结束后显示随机图片
 */
@Composable
fun RandomSplashScreen(
    durationMillis: Long = 1000L,
    onSplashFinished: () -> Unit,
    content: @Composable () -> Unit
) {
    // 随机选择一张启动图片
    val splashImages = remember {
        listOf(
            R.drawable.start_1,
            R.drawable.start_2,
            // 在此添加更多图片资源，例如：
            // R.drawable.start_3,
        )
    }

    val randomImage = remember { splashImages.random() }
    var showSplash by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }

    // 动画状态
    val appInfoAlpha = remember { Animatable(0f) }
    val appInfoOffsetY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        // 启动 App 信息的动画（延迟 200ms 后开始）
        launch {
            delay(200)
            launch { appInfoAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing)) }
            launch { appInfoOffsetY.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing)) }
        }

        delay(durationMillis)
        showSplash = false
        showContent = true
        onSplashFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容（在启动屏后面预加载）
        if (showContent) {
            content()
        }

        // 随机启动图片覆盖层
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 随机启动图片
                    Image(
                        painter = painterResource(id = randomImage),
                        contentDescription = "Splash Image",
                        modifier = Modifier.fillMaxHeight(0.5f),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // App 图标和名称（并排显示，带动画）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .alpha(appInfoAlpha.value)
                            .offset { IntOffset(0, appInfoOffsetY.value.toInt()) }
                    ) {
                        // App 图标
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = "App Icon",
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // App 名称
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

