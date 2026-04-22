package kiwi.lorenz.moocam.ui.camera

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

const val TAG = "CameraScreen"

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindCamera(lifecycleOwner)
    }

    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val selectedDelay by viewModel.selectedDelay.collectAsStateWithLifecycle()

    CameraScreenContent(
        onCaptureButtonTap = {
            viewModel.buttonTapped({ Log.i(TAG, "Photo taken!") }) },
        cameraState = cameraState,
        delays = viewModel.delays,
        selectedDelay = selectedDelay, // is passing the Int directly best practice here?
        onDelayChange = { newDelay -> viewModel.setDelay(newDelay)}
    )
}

@Composable
private fun CameraScreenContent(
    onCaptureButtonTap: () -> Unit,
    cameraState: CameraScreenState,
    delays: Array<Int>,
    selectedDelay: Int,
    onDelayChange: (Int) -> Unit
) {
    val isPreview = LocalInspectionMode.current
    Column(modifier = Modifier.fillMaxSize()) {
        // TODO Maybe Status bar padding at the top? It's camera so maybe we don't want it...
        if (isPreview) {
            Box(modifier = Modifier
                .weight(1f)
                .background(Color.Yellow))
        } else {
            when (cameraState) {
                is CameraScreenState.Ready -> {
                    CameraPreview(
                        surfaceRequest = cameraState.surfaceRequest,
                        modifier = Modifier.weight(1f)
                    )
                }
                is CameraScreenState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is CameraScreenState.Error -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Could not start camera: ${cameraState.message}",
                            color = Color.White
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(R.color.dark_charcoal)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            SegmentedControl(
                modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.medium),
                items = delays.toList(), // TODO arrays? list? think about this
                selectedItem = selectedDelay,
                onItemSelected = { i -> onDelayChange(i) },
            )
            Spacer(Modifier.height(spacing.large))
            CaptureButton(
                onClick = onCaptureButtonTap
            )

            Spacer(Modifier.height(spacing.large))
            // TODO Add navigation bar padding at the bottom
        }
    }
}

@Composable
fun DelayChip(delay: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 20),
        onClick = onClick,
        content = { Text(delay.toString()) } // like this or is there a nicer way?
    )
}

@Composable
fun CaptureButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier.size(80.dp),
        shape = RoundedCornerShape(percent = 50),
        onClick = onClick
    ) {}
}


@Preview
@Composable
private fun PreviewCameraScreen() {
    MooCamTheme() {
        CameraScreenContent(
            onCaptureButtonTap = {},
            cameraState = CameraScreenState.Loading,
            delays = arrayOf(5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
        )
    }
}