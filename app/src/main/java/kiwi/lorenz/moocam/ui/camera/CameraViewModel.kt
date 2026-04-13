package kiwi.lorenz.moocam.ui.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    private var _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    private var countdownJob: Job? = null

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(lifecycleOwner as Context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider { request ->
                    _surfaceRequest.value = request
                }
            }

            cameraProvider?.unbindAll()

            imageCapture = ImageCapture.Builder().build()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                imageCapture, preview)
        }, android.os.Handler(android.os.Looper.getMainLooper())::post)
    }

    fun takePhoto(onPhotoSaved: () -> Unit) {
        val imageCapture = imageCapture ?: return

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
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoSaved()
                }
            }
        )
    }

    fun buttonTapped(onPhotoSaved: () -> Unit) {
        when (val state = _timerState.value) {
            is TimerState.Idle -> { startCountdown(3, onPhotoSaved) }

            is TimerState.Counting -> { stopCountdown() }

            is TimerState.Capturing -> { /* Nothing, too late */ }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
    }

    private fun startCountdown(seconds: Int, onPhotoSaved: () -> Unit) {
        countdownJob?.cancel()
        viewModelScope.launch {
            for (i in seconds downTo 1) {
                _timerState.value = TimerState.Counting(i)
                delay(1000L)
            }
            _timerState.value = TimerState.Capturing
            takePhoto(onPhotoSaved)
            _timerState.value = TimerState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
    }
}

sealed class TimerState {
    object Idle : TimerState()
    data class Counting(val secondsLeft: Int) : TimerState()
    object Capturing : TimerState()
}