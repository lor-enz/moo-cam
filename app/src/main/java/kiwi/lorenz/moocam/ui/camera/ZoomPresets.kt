package kiwi.lorenz.moocam.ui.camera

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

data class ZoomUiState(
    val presets: List<Float>,
    val zoomRatio: Float
)

// One zoom ratio per lens, plus 1×. Most phones combine their back lenses into one camera
// and switch lenses by zoom ratio, so picking a lens means picking its ratio. Phones that
// don't report their lenses still get the ultra-wide (if they can zoom below 1×) and 2×.
fun zoomPresets(cameraInfo: CameraInfo, minZoom: Float, maxZoom: Float): List<Float> {
    val presets = mutableSetOf(1f)

    // Zooming out as far as possible shows the ultra-wide's full view
    if (minZoom < 0.95f) presets += minZoom

    // Telephoto lenses, rounded like camera apps show them (e.g. 4.8 → 5×)
    val teleRatios = lensZoomRatios(cameraInfo)
        .filter { it >= 1.5f }
        .map { it.roundToInt().toFloat().coerceAtMost(maxZoom) }
    presets += teleRatios
    if (teleRatios.isEmpty() && maxZoom >= 2f) presets += 2f

    return presets.sorted()
}

// Zoom ratio of each lens behind the camera, compared to the camera's own 1× view.
// CameraX has intrinsicZoomRatio for this, but it throws for physical cameras (seen on a
// Pixel 9 Pro), so compare the fields of view: focal length relative to sensor size.
fun lensZoomRatios(cameraInfo: CameraInfo): List<Float> {
    val base = lensOptics(cameraInfo)?.fieldOfViewScale ?: return emptyList()
    return cameraInfo.physicalCameraInfos
        .mapNotNull { lensOptics(it) }
        // Some phones also list crops of a sensor as cameras (the Pixel 9 Pro has 2× and 9×
        // crops). They share the lens's focal length, so keep only the full sensor per lens.
        .groupBy { it.focalLength }
        .map { (_, crops) -> crops.maxBy { it.sensorDiagonal }.fieldOfViewScale / base }
}

private data class LensOptics(val focalLength: Float, val sensorDiagonal: Float) {
    // Bigger means a narrower view, i.e. more zoomed in
    val fieldOfViewScale get() = focalLength / sensorDiagonal
}

@OptIn(ExperimentalCamera2Interop::class)
private fun lensOptics(cameraInfo: CameraInfo): LensOptics? = runCatching {
    val characteristics = Camera2CameraInfo.from(cameraInfo)
    val focalLength = characteristics
        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.firstOrNull() ?: return null
    val sensorSize = characteristics
        .getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
    LensOptics(focalLength, hypot(sensorSize.width, sensorSize.height))
}.getOrNull()

// One decimal at most, e.g. the Pixel 9 Pro's 0.508 shows as 0.5×
private val zoomFormat = DecimalFormat("0.#")

fun formatZoom(ratio: Float): String = zoomFormat.format(ratio) + "×"

// Hidden when there is nothing to choose from
@Composable
fun ZoomPresetRow(
    zoom: ZoomUiState,
    onPresetTap: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (zoom.presets.size < 2) return

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
    ) {
        zoom.presets.forEach { preset ->
            val isSelected = abs(zoom.zoomRatio - preset) < 0.05f
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colorResource(R.color.kiwi_green) else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onPresetTap(preset) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatZoom(preset),
                    style = AppTheme.typography.bodyMedium,
                    color = colorResource(R.color.off_white)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewZoomPresetRow() {
    MooCamTheme {
        ZoomPresetRow(
            zoom = ZoomUiState(presets = listOf(0.5f, 1f, 5f), zoomRatio = 1f),
            onPresetTap = {}
        )
    }
}
