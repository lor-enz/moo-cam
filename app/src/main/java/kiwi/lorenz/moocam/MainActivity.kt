package kiwi.lorenz.moocam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dagger.hilt.android.AndroidEntryPoint
import kiwi.lorenz.moocam.ui.camera.CameraScreen
import kiwi.lorenz.moocam.ui.permission.PermissionGate
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MooCamTheme {
                // Exposes the test tags to adb, see TestTags.kt
                Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                    PermissionGate(
                        content = { CameraScreen() }
                    )
                }
            }
        }
    }
}