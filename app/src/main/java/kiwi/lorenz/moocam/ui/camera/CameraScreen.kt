package kiwi.lorenz.moocam.ui.camera

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

const val TAG = "CameraScreen"

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindCamera(lifecycleOwner)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPreview) { Box(modifier = Modifier.fillMaxSize().background(Color.Yellow))
        } else {
            surfaceRequest?.let { request ->
                CameraPreview(
                    surfaceRequest = request,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }


        // TODO SystemBarsPadding
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom

        ) {
            CaptureButton(
                onClick = {
                    viewModel.takePhoto {
                        Log.i(TAG, "Photo taken!")
                    }
                }
            )

            Spacer(modifier = Modifier.size(100.dp))
        }
    }
}

@Composable
fun CaptureButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(percent = 50),
        onClick = onClick
    ) {}
}


@Preview
@Composable
private fun PreviewCameraScreen() {
    MooCamTheme() {
        CameraScreen()
    }
}