package kiwi.lorenz.moocam.ui.camera

import android.view.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

// How far past the middle between two rotations the device must turn to switch. Without it,
// a phone held right at that middle would keep flipping between them.
private const val SWITCH_MARGIN = 15

// The Surface.ROTATION_* value for a sensor reading in degrees clockwise from upright. Stays
// at the current rotation until the reading is clearly in another one.
fun deviceRotationFor(orientation: Int, current: Int): Int {
    val currentOrientation = (360 - current * 90) % 360
    val distance = abs((orientation - currentOrientation + 540) % 360 - 180)
    if (distance <= 45 + SWITCH_MARGIN) return current
    return when (orientation) {
        in 45 until 135 -> Surface.ROTATION_270
        in 135 until 225 -> Surface.ROTATION_180
        in 225 until 315 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }
}

// The smallest turn between two angles, so going from 270° to 0° doesn't spin backwards
fun shortestTurn(from: Float, to: Float): Float = (to - from + 180f).mod(360f) - 180f

// Degrees clockwise that signage has to turn to look upright
val LocalUprightAngle = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

@Composable
fun rememberUprightAngle(deviceRotation: Int): State<Float> {
    val angle = remember { Animatable(deviceRotation * 90f) }
    LaunchedEffect(deviceRotation) {
        angle.animateTo(angle.value + shortestTurn(angle.value, deviceRotation * 90f), tween(300))
    }
    return angle.asState()
}

// The layout stays in portrait, so only square elements or ones with room around them can
// use this. In the layer, so that animating the angle doesn't recompose them.
@Composable
fun Modifier.upright(): Modifier {
    val angle = LocalUprightAngle.current
    return graphicsLayer { rotationZ = angle.value }
}
