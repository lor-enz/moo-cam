package kiwi.lorenz.moocam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint
import kiwi.lorenz.moocam.ui.camera.CameraScreen
import kiwi.lorenz.moocam.ui.camera.PermissionGate
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MooCamTheme {
                PermissionGate()
            }
        }
    }
}