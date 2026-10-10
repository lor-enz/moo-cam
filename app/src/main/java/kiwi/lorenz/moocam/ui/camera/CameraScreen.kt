package kiwi.lorenz.moocam.ui.camera

import android.util.Log
import androidx.compose.foundation.background
import androidx.camera.core.AspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.ui.SegmentedControl
import kiwi.lorenz.moocam.ui.theme.AppTheme
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
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.cancelCountdown()
    }

    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val selectedDelay by viewModel.selectedDelay.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()



    CameraScreenContent(
        onCaptureButtonTap = {
            viewModel.buttonTapped({ Log.i(TAG, "Photo taken!") })
        },
        cameraState = cameraState,
        timerState = timerState,
        delays = viewModel.delays,
        selectedDelay = selectedDelay, // is passing the Int directly best practice here?
        onDelayChange = { newDelay -> viewModel.setDelay(newDelay)}
    )
}

@Composable
private fun CameraScreenContent(
    onCaptureButtonTap: () -> Unit,
    cameraState: CameraScreenState,
    timerState: TimerState,
    delays: Array<Int>,
    selectedDelay: Int,
    onDelayChange: (Int) -> Unit,
    // A real viewfinder can't render in previews; set false to preview the Loading/Error layouts
    showFakeViewfinder: Boolean = LocalInspectionMode.current
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .statusBarsPadding()) {
        if (showFakeViewfinder) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(Color.Yellow))
        } else {
            when (cameraState) {
                is CameraScreenState.Ready -> {
                    val previewRatio = if (cameraState.aspectRatio == AspectRatio.RATIO_4_3) 3f / 4f else 9f / 16f
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CameraPreview(
                            surfaceRequest = cameraState.surfaceRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(previewRatio)
                        )
                    }
                }
                // Loading and Error fill the whole screen on purpose: this pushes the
                // controls below out of view, so no timer or capture button is shown
                // while there is no usable camera.
                is CameraScreenState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is CameraScreenState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.camera_start_failed, cameraState.message),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.dark_charcoal))
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SegmentedControl(
                modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.medium),
                items = delays.toList(), // TODO arrays? list? think about this
                selectedItem = selectedDelay,
                onItemSelected = { i -> onDelayChange(i) },
            )
            Spacer(Modifier.height(spacing.large))
            CaptureButton(
                onClick = onCaptureButtonTap,
                timerState = timerState,
            )

            Spacer(Modifier.height(spacing.large))
        }
    }
}

@Composable
fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    timerState: TimerState
) {

    val buttonText by remember(timerState) {
        derivedStateOf {
            when (val state = timerState) {
                is TimerState.Counting -> state.secondsLeft.toString()
                else -> ""
            }
        }
    }

    Button(
        modifier = modifier.size(80.dp),
        shape = CircleShape,
        onClick = onClick,
        colors = when (timerState) {
            is TimerState.Capturing -> {
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.camera_red),
                    contentColor = colorResource(R.color.off_white)
                )
            }

            is TimerState.Counting -> {
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.kiwi_brown),
                    contentColor = colorResource(R.color.dark_charcoal)
                )
            }

            is TimerState.Idle -> {
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.kiwi_green),
                    contentColor = colorResource(R.color.dark_charcoal)
                )
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = buttonText,
                textAlign = TextAlign.Center,
                style = AppTheme.typography.headlineMedium,
                maxLines = 1
            )
        }
    }
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
            timerState = TimerState.Counting(12),
        )
    }
}

@Preview
@Composable
private fun PreviewCameraScreenLoading() {
    MooCamTheme() {
        CameraScreenContent(
            onCaptureButtonTap = {},
            cameraState = CameraScreenState.Loading,
            delays = arrayOf(5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            showFakeViewfinder = false,
        )
    }
}

@Preview
@Composable
private fun PreviewCameraScreenError() {
    MooCamTheme() {
        CameraScreenContent(
            onCaptureButtonTap = {},
            cameraState = CameraScreenState.Error("No back camera found"),
            delays = arrayOf(5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            showFakeViewfinder = false,
        )
    }
}