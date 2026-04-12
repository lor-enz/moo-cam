package kiwi.lorenz.moocam.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.MooCamPreview
import kiwi.lorenz.moocam.ui.theme.MooCamPreviews

@Composable
fun PermissionExplainerLayout(
    headline: String,
    subText: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = headline,
            style = AppTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.medium))
        Text(
            text = subText,
            style = AppTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.small))
        Button(onClick = onButtonClick) {
            Text(buttonLabel)
        }
    }
}

@MooCamPreviews
@Composable
private fun PreviewPermissionExplainer() {
    MooCamPreview {
        PermissionExplainerLayout(
            headline = "MooCam needs to see to moo",
            subText = "Camera access is required to take photos.\n Tap the button below to trigger the system permission dialog to show app and give MooCam access to the camera.",
            buttonLabel = "Allow Camera",
            onButtonClick = { }
        )
    }
}