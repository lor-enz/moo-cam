package kiwi.lorenz.moocam.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kiwi.lorenz.moocam.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        onPermissionResult = { _ ->
            askedBefore = true
        }
    )

    when {
        cameraPermissionState.status.isGranted -> {
            content()
        }

        !askedBefore -> {
            PermissionExplainerContent(
                headline = stringResource(R.string.permission_first_title),
                subText = stringResource(R.string.permission_first_subtext),
                buttonLabel = stringResource(R.string.permission_first_button_allow),
                onButtonClick = {
                    cameraPermissionState.launchPermissionRequest()
                }
            )
        }

        cameraPermissionState.status.shouldShowRationale -> {
            PermissionExplainerContent(
                headline = stringResource(R.string.permission_sec_title),
                subText = stringResource(R.string.permission_sec_subtext),
                buttonLabel = stringResource(R.string.permission_sec_button_allow),
                onButtonClick = {
                    cameraPermissionState.launchPermissionRequest()
                }
            )
        }

        else -> {
            PermissionExplainerContent(
                headline = stringResource(R.string.permission_denied_title),
                subText = stringResource(R.string.permission_denied_subtext),
                buttonLabel = stringResource(R.string.permission_denied_button),
                onButtonClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}