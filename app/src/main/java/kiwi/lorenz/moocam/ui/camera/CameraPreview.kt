package kiwi.lorenz.moocam.ui.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.MeteringPoint
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val focusCircleSize = 64.dp

// Faint circle that stays while the focus is locked
private const val lockedCircleAlpha = 0.4f

// Tapping the viewfinder focuses on that spot and shows a white circle there,
// like system camera apps do. The circle stays, faint, as long as the focus is locked.
@Composable
fun CameraPreview(
    surfaceRequest: SurfaceRequest,
    onTapToFocus: (MeteringPoint) -> Unit,
    isFocusLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val coordinateTransformer = remember { MutableCoordinateTransformer() }
    val currentOnTapToFocus by rememberUpdatedState(onTapToFocus)

    // Where the circle is drawn, in viewfinder coordinates; null until the first tap
    var focusPosition by remember { mutableStateOf<Offset?>(null) }
    // Counts taps, so tapping the same spot twice still restarts the animation
    var tapCount by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            coordinateTransformer = coordinateTransformer,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(surfaceRequest) {
                    detectTapGestures { tap ->
                        // The viewfinder may be scaled or cropped, so convert the tap
                        // to coordinates on the camera's preview surface
                        val surfacePosition = with(coordinateTransformer) { tap.transform() }
                        val point = SurfaceOrientedMeteringPointFactory(
                            surfaceRequest.resolution.width.toFloat(),
                            surfaceRequest.resolution.height.toFloat()
                        ).createPoint(surfacePosition.x, surfacePosition.y)
                        currentOnTapToFocus(point)
                        focusPosition = tap
                        tapCount++
                    }
                }
        )

        // Hidden as soon as the lock is gone, e.g. after a zoom change
        val position = focusPosition
        if (isFocusLocked && position != null) {
            FocusCircle(position = position, tapCount = tapCount)
        }
    }
}

// Shrinks into place, stays briefly, then fades to faint
@Composable
private fun FocusCircle(position: Offset, tapCount: Int) {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(tapCount) {
        alpha.snapTo(1f)
        scale.snapTo(1.4f)
        scale.animateTo(1f, tween(durationMillis = 200))
        delay(800)
        alpha.animateTo(lockedCircleAlpha, tween(durationMillis = 300))
    }

    Box(
        modifier = Modifier
            .offset {
                val radius = focusCircleSize.toPx() / 2
                IntOffset((position.x - radius).roundToInt(), (position.y - radius).roundToInt())
            }
            .size(focusCircleSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .border(2.dp, Color.White, CircleShape)
    )
}
