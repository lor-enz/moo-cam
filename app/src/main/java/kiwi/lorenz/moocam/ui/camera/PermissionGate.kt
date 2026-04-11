package kiwi.lorenz.moocam.ui.camera

import android.Manifest
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var askedBefore by rememberSaveable { mutableStateOf(false) }

    when {
        cameraPermissionState.status.isGranted -> {
            CameraScreen()
        }

        !askedBefore -> {
            // UNKNOWN - never asked yet
            LaunchedEffect(Unit) {
                askedBefore = true
                cameraPermissionState.launchPermissionRequest()
            }
        }

        cameraPermissionState.status.shouldShowRationale -> {
            // DENIED - can ask again
            PermissionExplainerLayout(
                headline = "Permission issue 1",
                subText = "Grant permission pls",
                buttonLabel = "Yes",
                onButtonClick = {  }
            )
        }

        else -> {
            // DENIED_PERMANENT - must go to settings
            PermissionExplainerLayout(
                headline = "Permission issue 2",
                subText = "Grant permission pls",
                buttonLabel = "Yes",
                onButtonClick = {  }
            )
        }
    }


}