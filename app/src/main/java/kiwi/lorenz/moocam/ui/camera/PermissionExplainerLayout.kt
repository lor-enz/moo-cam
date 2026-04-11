package kiwi.lorenz.moocam.ui.camera

import androidx.compose.foundation.gestures.snapping.SnapPosition
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
import androidx.compose.ui.unit.dp

@Composable
fun PermissionExplainerLayout(
    headline: String,
    subText: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(headline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(subText)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onButtonClick) {
            Text(buttonLabel)
        }

    }
}