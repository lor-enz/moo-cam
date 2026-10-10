package kiwi.lorenz.moocam.ui.camera

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.camera.core.AspectRatio
import androidx.camera.core.MeteringPoint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.ui.SegmentedControl
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        viewModel.onCameraStopped()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshLastPhoto()
    }
    DeviceRotationEffect(onRotationChange = viewModel::setDeviceRotation)

    // The phone often waits on a shelf or tripod for the right moment; if the screen turned
    // off, the app would go to the background and stop the camera
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val isVolumeTooLow by viewModel.isVolumeTooLow.collectAsStateWithLifecycle()
    val isPrivateOutputConnected by viewModel.isPrivateOutputConnected.collectAsStateWithLifecycle()

    // A photo without the moo is pointless, so stop the countdown when the overlay shows
    LaunchedEffect(isVolumeTooLow) {
        if (isVolumeTooLow) viewModel.cancelCountdown()
    }

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CameraEvent.PhotoSaved -> Log.i(TAG, "Photo taken!")
                CameraEvent.CaptureFailed -> Toast.makeText(
                    context,
                    R.string.photo_capture_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val selectedDelay by viewModel.selectedDelay.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val lastPhotoUri by viewModel.lastPhotoUri.collectAsStateWithLifecycle()
    val selectedSound by viewModel.selectedSound.collectAsStateWithLifecycle()
    val zoom by viewModel.zoom.collectAsStateWithLifecycle()
    val isFocusLocked by viewModel.isFocusLocked.collectAsStateWithLifecycle()
    var showSoundPicker by rememberSaveable { mutableStateOf(false) }

    CameraScreenContent(
        onCaptureButtonTap = {
            viewModel.buttonTapped()
        },
        cameraState = cameraState,
        timerState = timerState,
        delays = viewModel.delays,
        selectedDelay = selectedDelay, // is passing the Int directly best practice here?
        onDelayChange = { newDelay -> viewModel.setDelay(newDelay)},
        isVolumeTooLow = isVolumeTooLow,
        isPrivateOutputConnected = isPrivateOutputConnected,
        lastPhotoUri = lastPhotoUri,
        onGalleryButtonTap = { uri -> openInGallery(context, uri) },
        selectedSound = selectedSound,
        onSoundLabelTap = { showSoundPicker = true },
        zoom = zoom,
        onZoomPresetTap = { ratio -> viewModel.setZoom(ratio) },
        onTapToFocus = { point -> viewModel.focusOn(point) },
        isFocusLocked = isFocusLocked
    )

    if (showSoundPicker) {
        SoundPickerDialog(
            sounds = viewModel.sounds,
            selectedSound = selectedSound,
            onSoundTap = { sound -> viewModel.previewSound(sound) },
            onConfirm = { sound ->
                viewModel.setSound(sound)
                showSoundPicker = false
            },
            onDismiss = { showSoundPicker = false }
        )
    }
}

// The activity is locked to portrait, so the display never rotates. Read how the device is
// held from the sensor instead, so landscape photos are saved upright. Only listens while
// the screen is started, to not use the sensor in the background.
@Composable
private fun DeviceRotationEffect(onRotationChange: (Int) -> Unit) {
    val context = LocalContext.current
    val currentOnRotationChange by rememberUpdatedState(onRotationChange)
    LifecycleStartEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // Unknown when the phone lies flat; keep the last rotation then
                if (orientation == ORIENTATION_UNKNOWN) return
                // Degrees clockwise from upright, mapped to the matching display rotation
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                currentOnRotationChange(rotation)
            }
        }
        listener.enable()
        onStopOrDispose { listener.disable() }
    }
}

@Composable
private fun CameraScreenContent(
    onCaptureButtonTap: () -> Unit,
    cameraState: CameraScreenState,
    timerState: TimerState,
    delays: Array<Int>,
    selectedDelay: Int,
    onDelayChange: (Int) -> Unit,
    isVolumeTooLow: Boolean,
    isPrivateOutputConnected: Boolean,
    lastPhotoUri: Uri?,
    onGalleryButtonTap: (Uri) -> Unit,
    selectedSound: AnimalSound,
    onSoundLabelTap: () -> Unit,
    zoom: ZoomUiState?,
    onZoomPresetTap: (Float) -> Unit,
    onTapToFocus: (MeteringPoint) -> Unit,
    isFocusLocked: Boolean = false,
    // A real viewfinder can't render in previews; set false to preview the Loading/Error layouts
    showFakeViewfinder: Boolean = LocalInspectionMode.current
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()) {
            // Drawn on top of the viewfinder
            val viewfinderOverlay: @Composable BoxScope.() -> Unit = {
                SoundLabel(
                    sound = selectedSound,
                    enabled = timerState is TimerState.Idle,
                    onClick = onSoundLabelTap,
                    modifier = Modifier.padding(spacing.medium)
                )
                zoom?.let {
                    ZoomPresetRow(
                        zoom = it,
                        onPresetTap = onZoomPresetTap,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(spacing.medium)
                    )
                }
            }
            if (showFakeViewfinder) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.Yellow)) {
                    viewfinderOverlay()
                }
            } else {
                when (cameraState) {
                    is CameraScreenState.Ready -> {
                        val previewRatio = if (cameraState.aspectRatio == AspectRatio.RATIO_4_3) 3f / 4f else 9f / 16f
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CameraPreview(
                                surfaceRequest = cameraState.surfaceRequest,
                                onTapToFocus = onTapToFocus,
                                isFocusLocked = isFocusLocked,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(previewRatio)
                            )
                            viewfinderOverlay()
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
                if (isPrivateOutputConnected) {
                    PrivateOutputBanner(
                        modifier = Modifier.padding(horizontal = spacing.large)
                    )
                    Spacer(Modifier.height(spacing.medium))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Equal weights on both sides keep the capture button centered
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        GalleryButton(
                            photoUri = lastPhotoUri,
                            enabled = timerState is TimerState.Idle,
                            onClick = onGalleryButtonTap
                        )
                    }
                    CaptureButton(
                        onClick = onCaptureButtonTap,
                        timerState = timerState,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(spacing.large))
            }
        }

        if (isVolumeTooLow) {
            VolumeTooLowOverlay()
        }
    }
}

// Covers the whole screen and can't be dismissed. It goes away by itself once the volume is turned up.
@Composable
private fun VolumeTooLowOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            // Swallow all touches so the camera can't be used without sound
            .clickable(interactionSource = null, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(spacing.large)
                .background(colorResource(R.color.off_white), RoundedCornerShape(24.dp))
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.volume_plus),
                contentDescription = null,
                tint = colorResource(R.color.dark_charcoal),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(spacing.medium))
            Text(
                text = stringResource(R.string.volume_too_low_title),
                style = AppTheme.typography.headlineMedium,
                color = colorResource(R.color.dark_charcoal),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(spacing.small))
            Text(
                text = stringResource(R.string.volume_too_low_text),
                style = AppTheme.typography.bodyLarge,
                color = colorResource(R.color.dark_charcoal),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrivateOutputBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(colorResource(R.color.light_brown), RoundedCornerShape(12.dp))
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(android.R.drawable.stat_sys_warning),
            contentDescription = null,
            tint = colorResource(R.color.dark_brown),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(spacing.small))
        Text(
            text = stringResource(R.string.private_output_warning),
            style = AppTheme.typography.bodyMedium,
            color = colorResource(R.color.dark_brown)
        )
    }
}

// Shows the last photo taken in this session, or a placeholder until there is one
@Composable
private fun GalleryButton(
    photoUri: Uri?,
    enabled: Boolean,
    onClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = photoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(uri, Size(256, 256), null).asImageBitmap()
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(2.dp, colorResource(R.color.off_white), CircleShape)
            .clickable(
                enabled = enabled && photoUri != null,
                onClickLabel = stringResource(R.string.open_last_photo),
                role = Role.Button,
                onClick = { photoUri?.let(onClick) }
            ),
        contentAlignment = Alignment.Center
    ) {
        val image = thumbnail
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.last_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_gallery),
                contentDescription = null,
                tint = colorResource(R.color.off_white),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Prefer the camera roll review intent (lets you swipe through photos in e.g. Google Photos),
// fall back to a plain image viewer
private fun openInGallery(context: Context, uri: Uri) {
    val intents = listOf(
        Intent("com.android.camera.action.REVIEW", uri),
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/jpeg")
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            return
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app for ${intent.action}", e)
        }
    }
    Toast.makeText(context, R.string.no_gallery_app, Toast.LENGTH_LONG).show()
}

// Don't use the volume buttons as a shutter button: people need them to turn the volume
// back up when the volume overlay shows, and the moo needs the volume anyway.
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
            isVolumeTooLow = false,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = AnimalSound.MOO,
            onSoundLabelTap = {},
            zoom = ZoomUiState(presets = listOf(0.5f, 1f, 5f), zoomRatio = 1f),
            onZoomPresetTap = {},
            onTapToFocus = {},
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
            isVolumeTooLow = false,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = AnimalSound.MOO,
            onSoundLabelTap = {},
            zoom = null,
            onZoomPresetTap = {},
            onTapToFocus = {},
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
            isVolumeTooLow = false,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = AnimalSound.MOO,
            onSoundLabelTap = {},
            zoom = null,
            onZoomPresetTap = {},
            onTapToFocus = {},
            showFakeViewfinder = false,
        )
    }
}
@Preview
@Composable
private fun PreviewCameraScreenVolumeTooLow() {
    MooCamTheme() {
        CameraScreenContent(
            onCaptureButtonTap = {},
            cameraState = CameraScreenState.Loading,
            delays = arrayOf(5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            isVolumeTooLow = true,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = AnimalSound.MOO,
            onSoundLabelTap = {},
            zoom = null,
            onZoomPresetTap = {},
            onTapToFocus = {},
        )
    }
}

@Preview
@Composable
private fun PreviewCameraScreenPrivateOutput() {
    MooCamTheme() {
        CameraScreenContent(
            onCaptureButtonTap = {},
            cameraState = CameraScreenState.Loading,
            delays = arrayOf(5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            isVolumeTooLow = false,
            isPrivateOutputConnected = true,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = AnimalSound.MOO,
            onSoundLabelTap = {},
            zoom = null,
            onZoomPresetTap = {},
            onTapToFocus = {},
        )
    }
}
