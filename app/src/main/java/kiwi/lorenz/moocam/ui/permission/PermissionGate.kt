package kiwi.lorenz.moocam.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kiwi.lorenz.moocam.R

// Android 9 and below need storage permission to save photos to MediaStore
private val needsStoragePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

private val requiredPermissions = buildList {
    add(Manifest.permission.CAMERA)
    if (needsStoragePermission) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionsState = rememberMultiplePermissionsState(
        permissions = requiredPermissions,
        onPermissionsResult = { _ ->
            askedBefore = true
        }
    )

    when {
        permissionsState.allPermissionsGranted -> {
            content()
        }

        !askedBefore -> {
            PermissionExplainerContent(
                headline = stringResource(R.string.permission_first_title),
                subText = stringResource(
                    if (needsStoragePermission) R.string.permission_first_subtext_with_storage
                    else R.string.permission_first_subtext
                ),
                buttonLabel = stringResource(R.string.permission_first_button_allow),
                onButtonClick = {
                    permissionsState.launchMultiplePermissionRequest()
                }
            )
        }

        permissionsState.shouldShowRationale -> {
            PermissionExplainerContent(
                headline = stringResource(R.string.permission_sec_title),
                subText = stringResource(
                    if (needsStoragePermission) R.string.permission_sec_subtext_with_storage
                    else R.string.permission_sec_subtext
                ),
                buttonLabel = stringResource(R.string.permission_sec_button_allow),
                onButtonClick = {
                    permissionsState.launchMultiplePermissionRequest()
                }
            )
        }

        else -> {
            PermissionExplainerContent(
                headline = stringResource(R.string.permission_denied_title),
                subText = stringResource(
                    if (needsStoragePermission) R.string.permission_denied_subtext_with_storage
                    else R.string.permission_denied_subtext
                ),
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