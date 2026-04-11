package kiwi.lorenz.moocam.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(lifecycleOwner as android.content.Context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider { request ->
                    _surfaceRequest.value = request
                }
            }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, android.os.Handler(android.os.Looper.getMainLooper())::post)
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
    }
}