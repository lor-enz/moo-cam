package kiwi.lorenz.moocam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kiwi.lorenz.moocam.ui.permission.PermissionGate
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