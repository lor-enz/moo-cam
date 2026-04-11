package kiwi.lorenz.moocam.ui.camera

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindCamera(lifecycleOwner)
    }

    surfaceRequest?.let { request ->
        CameraPreview(
            surfaceRequest = request,
            modifier = Modifier.fillMaxSize()
        )
    }
}