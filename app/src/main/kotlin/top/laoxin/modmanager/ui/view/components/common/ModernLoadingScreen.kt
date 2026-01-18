package top.laoxin.modmanager.ui.view.components.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun ModernLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Core Animation: Pulse
        val infiniteTransition = rememberInfiniteTransition(label = "LoadingInfinite")

        // 1. Central Core Breathing
        val coreScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "CoreScale"
        )
        val coreAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "CoreAlpha"
        )

        // 2. Outer Ring Rotation
        val ringRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RingRotation"
        )

        // 3. Inner Ring Rotation (Counter-clockwise, faster)
        val innerRingRotation by infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "InnerRingRotation"
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = Modifier.size(120.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2

            // Draw Central Core
            drawCircle(
                color = primaryColor.copy(alpha = coreAlpha),
                radius = (baseRadius * 0.3f) * coreScale,
                center = center
            )

            // Draw Outer Ring (Arc)
            rotate(ringRotation, center) {
                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.9f, center.y - baseRadius * 0.9f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.8f, baseRadius * 1.8f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw Inner Ring (Arc)
            rotate(innerRingRotation, center) {
                drawArc(
                    color = tertiaryColor,
                    startAngle = 0f,
                    sweepAngle = 180f, // Half circle
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.6f, center.y - baseRadius * 0.6f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.2f, baseRadius * 1.2f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}
