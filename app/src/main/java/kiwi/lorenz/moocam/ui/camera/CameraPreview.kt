package kiwi.lorenz.moocam.ui.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CameraPreview(
    surfaceRequest: SurfaceRequest,
    modifier: Modifier = Modifier
) {
    CameraXViewfinder(
        surfaceRequest = surfaceRequest,
        modifier = modifier
    )
}