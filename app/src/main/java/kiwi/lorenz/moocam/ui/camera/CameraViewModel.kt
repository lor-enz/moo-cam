package kiwi.lorenz.moocam.ui.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.hardware.camera2.CameraCharacteristics
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.audio.AudioOutputMonitor
import kiwi.lorenz.moocam.audio.SoundRepository
import kiwi.lorenz.moocam.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import javax.inject.Inject
import androidx.annotation.OptIn as OptIn1

@HiltViewModel
class CameraViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val soundRepository: SoundRepository,
    private val settingsRepository: SettingsRepository,
    audioOutputMonitor: AudioOutputMonitor
) : ViewModel() {

    private val _cameraState = MutableStateFlow<CameraScreenState>(CameraScreenState.Loading)
    val cameraState = _cameraState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    // Whether a tapped focus spot is locked, so the viewfinder can keep showing it
    private val _isFocusLocked = MutableStateFlow(false)
    val isFocusLocked = _isFocusLocked.asStateFlow()

    // Null until the camera is bound
    private val _zoom = MutableStateFlow<ZoomUiState?>(null)
    val zoom = _zoom.asStateFlow()

    val delays = arrayOf(5, 7, 10, 15) // hard coded on purpose
    private val defaultDelay = 10

    // Eagerly, so the saved value is already loaded when the countdown reads it.
    // A saved delay that's no longer offered falls back to the default.
    val selectedDelay = settingsRepository.selectedDelay
        .map { saved -> saved?.takeIf { it in delays } ?: defaultDelay }
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaultDelay)

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState = _timerState.asStateFlow()

    private var countdownJob: Job? = null

    val sounds = AnimalSound.entries
    val selectedSound = settingsRepository.selectedSound
        .map { it ?: AnimalSound.MOO }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AnimalSound.MOO)

    // Last photo taken in this session, shown in the gallery button
    private val _lastPhotoUri = MutableStateFlow<Uri?>(null)
    val lastPhotoUri = _lastPhotoUri.asStateFlow()

    // Below this media volume the moo is too quiet to catch a kid's attention
    private val minMediaVolume = 0.2f

    private val audioOutputState = audioOutputMonitor.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Start as false, so nothing flashes up before the first real value arrives
    val isVolumeTooLow = audioOutputState
        .map { it != null && it.mediaVolume < minMediaVolume }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isPrivateOutputConnected = audioOutputState
        .map { it?.isPrivateOutputConnected == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // One-off events for the UI, e.g. to show a toast
    private val _events = Channel<CameraEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()


    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        _cameraState.value = CameraScreenState.Loading
        _isFocusLocked.value = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val aspectRatio = resolveSensorAspectRatio(cameraProvider!!)
                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        if (aspectRatio == AspectRatio.RATIO_4_3)
                            AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                        else
                            AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build().also {
                        it.setSurfaceProvider { request ->
                            _cameraState.value = CameraScreenState.Ready(request, aspectRatio)
                        }
                    }

                cameraProvider?.unbindAll()

                imageCapture = ImageCapture.Builder()
                    .setResolutionSelector(resolutionSelector)
                    // Fast shutter for moving toddlers. It also keeps the tap-to-focus lock:
                    // MAXIMIZE_QUALITY (and using the flash) makes CameraX refocus for the
                    // photo and unlock the focus afterwards.
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val camera = cameraProvider!!.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture, preview
                )
                this.camera = camera
                // Replace the observer of an earlier bind instead of adding a second one
                camera.cameraInfo.zoomState.removeObservers(lifecycleOwner)
                camera.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                    // Zoom buttons are a nice-to-have; never let them break the camera
                    val presets = runCatching {
                        zoomPresets(camera.cameraInfo, state.minZoomRatio, state.maxZoomRatio)
                    }.getOrElse { e ->
                        Log.w(TAG, "Could not work out zoom presets", e)
                        listOf(1f)
                    }
                    _zoom.value = ZoomUiState(
                        presets = presets,
                        zoomRatio = state.zoomRatio
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize camera", e)
                _cameraState.value =
                    CameraScreenState.Error(e.message ?: "Unable to initialize camera")
            }
        }, android.os.Handler(android.os.Looper.getMainLooper())::post)
    }

    fun setZoom(ratio: Float) {
        val cameraControl = camera?.cameraControl ?: return
        // A locked focus spot was picked for the old framing and may be on another lens,
        // so go back to automatic focus. The user can tap again.
        cameraControl.cancelFocusAndMetering()
        _isFocusLocked.value = false
        // The returned future only fails if a newer zoom replaces this one, which is fine
        cameraControl.setZoomRatio(ratio)
    }

    // Focuses and sets the exposure for this spot. Stays locked until the next tap: CameraX
    // would go back to automatic focus after 5 seconds, but the countdown can be longer.
    fun focusOn(point: MeteringPoint) {
        val action = FocusMeteringAction.Builder(point)
            .disableAutoCancel()
            .build()
        val cameraControl = camera?.cameraControl ?: return
        cameraControl.startFocusAndMetering(action)
        _isFocusLocked.value = true
    }

    // CameraX drops the focus lock when the camera closes, e.g. when the app goes to the background
    fun onCameraStopped() {
        _isFocusLocked.value = false
    }

    @OptIn1(ExperimentalCamera2Interop::class)
    private fun resolveSensorAspectRatio(cameraProvider: ProcessCameraProvider): Int {
        val backCameraInfo = cameraProvider.availableCameraInfos.firstOrNull {
            CameraSelector.DEFAULT_BACK_CAMERA.filter(listOf(it)).isNotEmpty()
        } ?: return AspectRatio.RATIO_4_3

        val sensorRect = Camera2CameraInfo.from(backCameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            ?: return AspectRatio.RATIO_4_3

        val sensorRatio = sensorRect.width().toFloat() / sensorRect.height()
        return if (abs(sensorRatio - 4f / 3f) <= abs(sensorRatio - 16f / 9f))
            AspectRatio.RATIO_4_3
        else
            AspectRatio.RATIO_16_9
    }

    private fun takePhoto(onCaptureFinished: () -> Unit) {
        val imageCapture = imageCapture ?: run {
            _events.trySend(CameraEvent.CaptureFailed)
            onCaptureFinished()
            return
        }

        val contentValues = ContentValues().apply {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            put(MediaStore.MediaColumns.DISPLAY_NAME, "MOO_$timestamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // DCIM is where camera apps store photos; galleries show MooCam as its own album
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/MooCam")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${error.message}", error)
                    _events.trySend(CameraEvent.CaptureFailed)
                    onCaptureFinished()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    _lastPhotoUri.value = outputFileResults.savedUri
                    _events.trySend(CameraEvent.PhotoSaved)
                    onCaptureFinished()
                }
            }
        )
    }

    // The photo may have been deleted in the gallery app meanwhile; then go back to the placeholder
    fun refreshLastPhoto() {
        val uri = _lastPhotoUri.value ?: return
        viewModelScope.launch {
            val exists = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                        ?.use { it.moveToFirst() } == true
                }.getOrDefault(false)
            }
            if (!exists && _lastPhotoUri.value == uri) _lastPhotoUri.value = null
        }
    }

    fun buttonTapped() {
        when (val state = _timerState.value) {
            is TimerState.Idle -> { startCountdown(selectedDelay.value) }

            is TimerState.Counting -> { stopCountdown() }

            is TimerState.Capturing -> { /* Nothing, too late */ }
        }
    }

    fun setDelay(newDelay: Int) {
        viewModelScope.launch { settingsRepository.setSelectedDelay(newDelay) }
    }

    fun setSound(sound: AnimalSound) {
        viewModelScope.launch { settingsRepository.setSelectedSound(sound) }
    }

    // Lets the user hear a sound while picking one
    fun previewSound(sound: AnimalSound) {
        soundRepository.playAnimalSound(sound)
    }

    // Called when the screen stops, so no sound or capture fires while the app is in the background
    fun cancelCountdown() {
        if (_timerState.value is TimerState.Counting) stopCountdown()
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        _timerState.value = TimerState.Idle
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _timerState.value = TimerState.Counting(i)
                if (i == 2) soundRepository.playAnimalSound(selectedSound.value)
                delay(1000L)
            }
            soundRepository.playShutter()
            _timerState.value = TimerState.Capturing
            takePhoto(
                onCaptureFinished = {
                    _timerState.value = TimerState.Idle 
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
    }
}

sealed class CameraScreenState {
    object Loading : CameraScreenState()
    data class Ready(val surfaceRequest: SurfaceRequest, val aspectRatio: Int) : CameraScreenState()
    data class Error(val message: String) : CameraScreenState()
}

sealed class CameraEvent {
    object PhotoSaved : CameraEvent()
    object CaptureFailed : CameraEvent()
}

sealed class TimerState {
    object Idle : TimerState()
    data class Counting(val secondsLeft: Int) : TimerState()
    object Capturing : TimerState()
}