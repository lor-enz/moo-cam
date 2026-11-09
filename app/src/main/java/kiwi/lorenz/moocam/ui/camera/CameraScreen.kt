package kiwi.lorenz.moocam.ui.camera

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPoint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kiwi.lorenz.moocam.audio.SoundChoice
import kiwi.lorenz.moocam.capture.CaptureMode
import kiwi.lorenz.moocam.flash.AttentionFlash
import kiwi.lorenz.moocam.ui.SegmentedControl
import kiwi.lorenz.moocam.ui.TestTags
import kiwi.lorenz.moocam.ui.theme.AppColors
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
    val lensFacing by viewModel.lensFacing.collectAsStateWithLifecycle()
    LaunchedEffect(lifecycleOwner, lensFacing) {
        viewModel.bindCamera(lifecycleOwner)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.cancelCountdown()
        viewModel.onCameraStopped()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshLastPhoto()
    }
    val deviceRotation by viewModel.deviceRotation.collectAsStateWithLifecycle()
    DeviceRotationEffect(rotation = deviceRotation, onRotationChange = viewModel::setDeviceRotation)
    DisplayRotationEffect(onRotationChange = viewModel::setDisplayRotation)

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
    val hasFlashUnit by viewModel.hasFlashUnit.collectAsStateWithLifecycle()
    val selectedFlash by viewModel.selectedFlash.collectAsStateWithLifecycle()
    val captureMode by viewModel.captureMode.collectAsStateWithLifecycle()
    val hasFrontCamera by viewModel.hasFrontCamera.collectAsStateWithLifecycle()
    var openSetting by rememberSaveable { mutableStateOf<ViewfinderSetting?>(null) }
    BackHandler(enabled = openSetting != null) { openSetting = null }
    // Simpler than moving an open picker along; it reopens the right way up
    LaunchedEffect(deviceRotation) { openSetting = null }

    // Top to bottom in the rail
    val settings = listOfNotNull(
        ViewfinderSetting.SOUND,
        ViewfinderSetting.FLASH.takeIf { hasFlashUnit },
        ViewfinderSetting.CAPTURE_MODE
    )

    CompositionLocalProvider(LocalUprightAngle provides rememberUprightAngle(deviceRotation)) {
        CameraScreenContent(
            onCaptureButtonTap = {
                openSetting = null
                viewModel.buttonTapped()
            },
            cameraState = cameraState,
            timerState = timerState,
            delays = viewModel.delays,
            selectedDelay = selectedDelay, // is passing the Int directly best practice here?
            onDelayChange = { newDelay ->
                openSetting = null
                viewModel.setDelay(newDelay)
            },
            isVolumeTooLow = isVolumeTooLow,
            isPrivateOutputConnected = isPrivateOutputConnected,
            lastPhotoUri = lastPhotoUri,
            onGalleryButtonTap = { uri ->
                openSetting = null
                openInGallery(context, uri)
            },
            selectedSound = selectedSound,
            selectedFlash = selectedFlash.takeIf { hasFlashUnit },
            captureMode = captureMode,
            openSetting = openSetting,
            onSettingTap = { setting -> openSetting = setting.takeIf { it != openSetting } },
            onPickerDismiss = { openSetting = null },
            settingPickers = {
                val railMargin = spacing.medium
                val pickerModifier = { setting: ViewfinderSetting ->
                    besideRailButton(settings.indexOf(setting), settings.size, railMargin)
                }
                OptionPicker(
                    visible = openSetting == ViewfinderSetting.SOUND,
                    title = stringResource(R.string.choose_sound),
                    options = viewModel.sounds,
                    selectedOption = selectedSound,
                    label = { it.displayName },
                    icon = { it.iconRes },
                    onOptionTap = { sound ->
                        viewModel.setSound(sound)
                        viewModel.previewSound(sound)
                    },
                    modifier = pickerModifier(ViewfinderSetting.SOUND)
                )
                OptionPicker(
                    visible = openSetting == ViewfinderSetting.FLASH,
                    title = stringResource(R.string.choose_flash),
                    options = viewModel.flashes,
                    selectedOption = selectedFlash,
                    label = { it.displayName },
                    icon = { it.iconRes },
                    onOptionTap = { flash ->
                        viewModel.setFlash(flash)
                        viewModel.previewFlash(flash)
                    },
                    modifier = pickerModifier(ViewfinderSetting.FLASH)
                )
                OptionPicker(
                    visible = openSetting == ViewfinderSetting.CAPTURE_MODE,
                    title = stringResource(R.string.choose_capture_mode),
                    options = viewModel.captureModes,
                    selectedOption = captureMode,
                    label = { it.displayName },
                    icon = { it.iconRes },
                    onOptionTap = { mode -> viewModel.setCaptureMode(mode) },
                    modifier = pickerModifier(ViewfinderSetting.CAPTURE_MODE)
                )
            },
            zoom = zoom,
            onZoomPresetTap = { ratio -> viewModel.setZoom(ratio) },
            onTapToFocus = { point -> viewModel.focusOn(point) },
            isFocusLocked = isFocusLocked,
            isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
            onSwitchCameraTap = {
                openSetting = null
                viewModel.switchCamera()
            }.takeIf { hasFrontCamera }
        )
    }
}

// The activity is locked to portrait, so the display never rotates. Read how the device is
// held from the sensor instead, so landscape photos are saved upright. Only listens while
// the screen is started, to not use the sensor in the background.
@Composable
private fun DeviceRotationEffect(rotation: Int, onRotationChange: (Int) -> Unit) {
    val context = LocalContext.current
    val currentRotation by rememberUpdatedState(rotation)
    val currentOnRotationChange by rememberUpdatedState(onRotationChange)
    LifecycleStartEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // Unknown when the phone lies flat; keep the last rotation then
                if (orientation == ORIENTATION_UNKNOWN) return
                currentOnRotationChange(deviceRotationFor(orientation, currentRotation))
            }
        }
        listener.enable()
        onStopOrDispose { listener.disable() }
    }
}

// Reports the rotation of the display this screen is on. A display listener, because turning
// the device by 180° rotates the display without a configuration change.
@Composable
private fun DisplayRotationEffect(onRotationChange: (Int) -> Unit) {
    val view = LocalView.current
    val currentOnRotationChange by rememberUpdatedState(onRotationChange)
    DisposableEffect(view) {
        val displayManager = view.context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                val display = view.display ?: return
                if (displayId == display.displayId) currentOnRotationChange(display.rotation)
            }
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
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
    selectedSound: SoundChoice,
    // Null hides the flash button, for phones without a flashlight
    selectedFlash: AttentionFlash?,
    captureMode: CaptureMode,
    openSetting: ViewfinderSetting?,
    onSettingTap: (ViewfinderSetting) -> Unit,
    onPickerDismiss: () -> Unit,
    settingPickers: @Composable BoxScope.() -> Unit,
    zoom: ZoomUiState?,
    onZoomPresetTap: (Float) -> Unit,
    onTapToFocus: (MeteringPoint) -> Unit,
    isFocusLocked: Boolean = false,
    isFrontCamera: Boolean = false,
    // Null hides the switch button, for phones without a selfie camera
    onSwitchCameraTap: (() -> Unit)? = null,
    // A real viewfinder can't render in previews; set false to preview the Loading/Error layouts
    showFakeViewfinder: Boolean = LocalInspectionMode.current
) {
    // Debug only: long-pressing the capture mode button toggles a stock photo in the viewfinder,
    // for store listing screenshots
    var showStockPhoto by remember { mutableStateOf(false) }
    val isSideways = rememberIsSideways()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cameraBackground)
            .statusBarsPadding()) {
            // Drawn on top of the viewfinder
            val viewfinderOverlay: @Composable BoxScope.() -> Unit = {
                zoom?.let {
                    ZoomPresetRow(
                        zoom = it,
                        onPresetTap = onZoomPresetTap,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(spacing.medium)
                    )
                }
                // Too wide to turn in the controls below
                if (isPrivateOutputConnected && isSideways) {
                    PrivateOutputBanner(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(spacing.medium)
                            .uprightPanel()
                            .testTag(TestTags.PRIVATE_OUTPUT_BANNER)
                    )
                }
                // Invisible. On top of the viewfinder and the zoom presets, so a tap there only
                // closes the picker; the rail stays usable to switch to another picker
                if (openSetting != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .testTag(TestTags.CLOSE_OPTION_PICKER)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClickLabel = stringResource(R.string.close_options),
                                onClick = onPickerDismiss
                            )
                    )
                }
                settingPickers()
                SettingsRail(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(spacing.medium)
                ) {
                    SettingsRailButton(
                        iconRes = android.R.drawable.ic_lock_silent_mode_off,
                        caption = selectedSound.displayName,
                        onClickLabel = stringResource(R.string.choose_sound),
                        enabled = timerState is TimerState.Idle,
                        isOpen = openSetting == ViewfinderSetting.SOUND,
                        onClick = { onSettingTap(ViewfinderSetting.SOUND) },
                        modifier = Modifier.testTag(TestTags.SOUND_BUTTON)
                    )
                    selectedFlash?.let { flash ->
                        SettingsRailButton(
                            iconRes = if (flash == AttentionFlash.OFF) R.drawable.flash_off else R.drawable.flash,
                            caption = flash.shortName,
                            onClickLabel = stringResource(R.string.choose_flash),
                            enabled = timerState is TimerState.Idle,
                            isOpen = openSetting == ViewfinderSetting.FLASH,
                            onClick = { onSettingTap(ViewfinderSetting.FLASH) },
                            modifier = Modifier.testTag(TestTags.FLASH_BUTTON)
                        )
                    }
                    // Shows the photo count, not the mode's name, which is too long for the caption
                    SettingsRailButton(
                        iconRes = captureMode.iconRes,
                        caption = captureMode.photoCount.toString(),
                        onClickLabel = stringResource(R.string.choose_capture_mode),
                        enabled = timerState is TimerState.Idle,
                        isOpen = openSetting == ViewfinderSetting.CAPTURE_MODE,
                        onClick = { onSettingTap(ViewfinderSetting.CAPTURE_MODE) },
                        onLongClick = stockViewfinderPhoto?.let { { showStockPhoto = !showStockPhoto } },
                        modifier = Modifier
                            .testTag(TestTags.CAPTURE_MODE_BUTTON)
                            .semantics { contentDescription = captureMode.displayName }
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
                            if (showStockPhoto && stockViewfinderPhoto != null) {
                                Image(
                                    painter = painterResource(stockViewfinderPhoto),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
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
                            CircularProgressIndicator(color = AppColors.onDark)
                        }
                    }
                    is CameraScreenState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppColors.controlsBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.camera_start_failed, cameraState.message),
                                color = AppColors.onDark,
                                modifier = Modifier.uprightPanel()
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.controlsBackground)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SegmentedControl(
                    modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.medium),
                    items = delays.toList(), // TODO arrays? list? think about this
                    selectedItem = selectedDelay,
                    onItemSelected = { i -> onDelayChange(i) },
                    label = { seconds -> "$seconds s" },
                    labelModifier = Modifier.upright(),
                    // Tall enough for the labels to turn with the device
                    itemPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                )
                Spacer(Modifier.height(spacing.large))
                if (isPrivateOutputConnected) {
                    // Stays in the layout when sideways, so the capture button doesn't jump
                    PrivateOutputBanner(
                        modifier = Modifier
                            .padding(horizontal = spacing.large)
                            .upright()
                            .then(
                                if (isSideways) Modifier.alpha(0f).clearAndSetSemantics {}
                                else Modifier.testTag(TestTags.PRIVATE_OUTPUT_BANNER)
                            )
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
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        onSwitchCameraTap?.let { onClick ->
                            SwitchCameraButton(
                                isFrontCamera = isFrontCamera,
                                enabled = timerState is TimerState.Idle,
                                onClick = onClick,
                                modifier = Modifier.testTag(TestTags.SWITCH_CAMERA_BUTTON)
                            )
                        }
                    }
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
            .background(AppColors.scrim)
            // Swallow all touches so the camera can't be used without sound
            .clickable(interactionSource = null, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .uprightPanel()
                .padding(spacing.large)
                .background(AppColors.panel, RoundedCornerShape(24.dp))
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.volume_plus),
                contentDescription = null,
                tint = AppColors.onPanel,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(spacing.medium))
            Text(
                text = stringResource(R.string.volume_too_low_title),
                style = AppTheme.typography.headlineMedium,
                color = AppColors.onPanel,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(spacing.small))
            Text(
                text = stringResource(R.string.volume_too_low_text),
                style = AppTheme.typography.bodyLarge,
                color = AppColors.onPanel,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrivateOutputBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AppColors.panel, RoundedCornerShape(12.dp))
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(android.R.drawable.stat_sys_warning),
            contentDescription = null,
            tint = AppColors.onWarning,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(spacing.small))
        Text(
            text = stringResource(R.string.private_output_warning),
            style = AppTheme.typography.bodyMedium,
            color = AppColors.onWarning
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
            .upright()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(AppColors.thumbnailPlaceholder)
            .border(2.dp, AppColors.onDark, CircleShape)
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
                tint = AppColors.onDark,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Switches between the back and the selfie camera
@Composable
private fun SwitchCameraButton(
    isFrontCamera: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .upright()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(AppColors.sideButton)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.flip_camera),
            contentDescription = stringResource(
                if (isFrontCamera) R.string.switch_to_back_camera else R.string.switch_to_front_camera
            ),
            tint = AppColors.onDark,
            modifier = Modifier.size(28.dp)
        )
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
                    containerColor = AppColors.capturing,
                    contentColor = AppColors.onCapturing
                )
            }

            is TimerState.Counting -> {
                ButtonDefaults.buttonColors(
                    containerColor = AppColors.counting,
                    contentColor = AppColors.onCounting
                )
            }

            is TimerState.Idle -> {
                ButtonDefaults.buttonColors(
                    containerColor = AppColors.accent,
                    contentColor = AppColors.onAccent
                )
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = buttonText,
                textAlign = TextAlign.Center,
                style = AppTheme.typography.headlineMedium,
                maxLines = 1,
                modifier = Modifier.upright()
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
            delays = arrayOf(2, 5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Counting(12),
            isVolumeTooLow = false,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = SoundChoice.Fixed(AnimalSound.MOO),
            selectedFlash = AttentionFlash.THREE_SHORT,
            captureMode = CaptureMode.BURST_3,
            openSetting = null,
            onSettingTap = {},
            onPickerDismiss = {},
            settingPickers = {},
            zoom = ZoomUiState(presets = listOf(0.5f, 1f, 5f), zoomRatio = 1f),
            onZoomPresetTap = {},
            onTapToFocus = {},
            onSwitchCameraTap = {},
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
            delays = arrayOf(2, 5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            isVolumeTooLow = false,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = SoundChoice.Fixed(AnimalSound.MOO),
            selectedFlash = AttentionFlash.THREE_SHORT,
            captureMode = CaptureMode.BURST_3,
            openSetting = null,
            onSettingTap = {},
            onPickerDismiss = {},
            settingPickers = {},
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
            delays = arrayOf(2, 5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            isVolumeTooLow = false,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = SoundChoice.Fixed(AnimalSound.MOO),
            selectedFlash = AttentionFlash.THREE_SHORT,
            captureMode = CaptureMode.BURST_3,
            openSetting = null,
            onSettingTap = {},
            onPickerDismiss = {},
            settingPickers = {},
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
            delays = arrayOf(2, 5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            isVolumeTooLow = true,
            isPrivateOutputConnected = false,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = SoundChoice.Fixed(AnimalSound.MOO),
            selectedFlash = AttentionFlash.THREE_SHORT,
            captureMode = CaptureMode.BURST_3,
            openSetting = null,
            onSettingTap = {},
            onPickerDismiss = {},
            settingPickers = {},
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
            delays = arrayOf(2, 5, 7, 10, 15),
            selectedDelay = 7,
            onDelayChange = {},
            timerState = TimerState.Idle,
            isVolumeTooLow = false,
            isPrivateOutputConnected = true,
            lastPhotoUri = null,
            onGalleryButtonTap = {},
            selectedSound = SoundChoice.Fixed(AnimalSound.MOO),
            selectedFlash = AttentionFlash.THREE_SHORT,
            captureMode = CaptureMode.BURST_3,
            openSetting = null,
            onSettingTap = {},
            onPickerDismiss = {},
            settingPickers = {},
            zoom = null,
            onZoomPresetTap = {},
            onTapToFocus = {},
        )
    }
}
