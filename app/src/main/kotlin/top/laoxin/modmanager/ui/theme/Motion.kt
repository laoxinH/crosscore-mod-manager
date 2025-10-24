package top.laoxin.modmanager.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MotionDuration {
    const val Short = 200
    const val Medium = 300
    const val Long = 400
    const val ExtraLong = 500
}

object MotionEasing {
    val Standard = FastOutSlowInEasing
    val Emphasized = LinearOutSlowInEasing
}

object MotionSpec {
    val standard = tween<Float>(
        durationMillis = MotionDuration.Medium,
        easing = MotionEasing.Standard
    )
    
    val emphasized = tween<Float>(
        durationMillis = MotionDuration.Long,
        easing = MotionEasing.Emphasized
    )
    
    val quick = tween<Float>(
        durationMillis = MotionDuration.Short,
        easing = MotionEasing.Standard
    )
    
    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

