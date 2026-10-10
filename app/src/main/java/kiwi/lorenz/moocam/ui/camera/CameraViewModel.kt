package kiwi.lorenz.moocam.ui.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kiwi.lorenz.moocam.R
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.audio.SoundRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject
import androidx.annotation.OptIn as OptIn1

@HiltViewModel
class CameraViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val soundRepository: SoundRepository
) : ViewModel() {

    private val _cameraState = MutableStateFlow<CameraScreenState>(CameraScreenState.Loading)
    val cameraState = _cameraState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    val delays = arrayOf(5, 7, 10, 15) // hard coded on purpose
    private val _selectedDelay = MutableStateFlow<Int>(10)
    val selectedDelay = _selectedDelay.asStateFlow()

    private var _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState = _timerState.asStateFlow()

    private var countdownJob: Job? = null


    @OptIn1(ExperimentalCamera2Interop::class)
    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        _cameraState.value = CameraScreenState.Loading
        val cameraProviderFuture = ProcessCameraProvider.getInstance(lifecycleOwner as Context)

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
                    .build()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture, preview
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize camera", e)
                _cameraState.value =
                    CameraScreenState.Error(e.message ?: "Unable to initialize camera")
            }
        }, android.os.Handler(android.os.Looper.getMainLooper())::post)
    }

    @OptIn1(ExperimentalCamera2Interop::class)
    @OptIn(ExperimentalCamera2Interop::class)
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

    private fun takePhoto(onPhotoSaved: () -> Unit, onCaptureFinished: () -> Unit) {
        val imageCapture = imageCapture ?: run {
            showCaptureFailedToast()
            onCaptureFinished()
            return
        }

        val contentValues = ContentValues().apply {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            put(MediaStore.MediaColumns.DISPLAY_NAME, "MOO_$timestamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
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
                    showCaptureFailedToast()
                    onCaptureFinished()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoSaved()
                    onCaptureFinished()
                }
            }
        )
    }

    private fun showCaptureFailedToast() {
        Toast.makeText(context, R.string.photo_capture_failed, Toast.LENGTH_LONG).show()
    }

    fun buttonTapped(onPhotoSaved: () -> Unit) {
        when (val state = _timerState.value) {
            is TimerState.Idle -> { startCountdown(selectedDelay.value, onPhotoSaved) }

            is TimerState.Counting -> { stopCountdown() }

            is TimerState.Capturing -> { /* Nothing, too late */ }
        }
    }

    fun setDelay(newDelay: Int) {
        _selectedDelay.value = newDelay
    }

    // Called when the screen stops, so no sound or capture fires while the app is in the background
    fun cancelCountdown() {
        if (_timerState.value is TimerState.Counting) stopCountdown()
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        _timerState.value = TimerState.Idle
    }

    private fun startCountdown(seconds: Int, onPhotoSaved: () -> Unit) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _timerState.value = TimerState.Counting(i)
                if (i == 2) soundRepository.playAnimalSound(AnimalSound.MOO)
                delay(1000L)
            }
            soundRepository.playShutter()
            _timerState.value = TimerState.Capturing
            takePhoto(
                onPhotoSaved = onPhotoSaved,
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

sealed class TimerState {
    object Idle : TimerState()
    data class Counting(val secondsLeft: Int) : TimerState()
    object Capturing : TimerState()
}