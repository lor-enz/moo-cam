package kiwi.lorenz.moocam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// The app has one dark look, whatever the system's theme or wallpaper
private val MooCamColorScheme = darkColorScheme(
    primary = AppColors.accent,
    onPrimary = AppColors.onAccent,
    background = AppColors.controlsBackground,
    onBackground = AppColors.onDark,
    surface = AppColors.controlsBackground,
    onSurface = AppColors.onDark
)

@Composable
fun MooCamTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = MooCamColorScheme,
            typography = Typography,
            content = content
        )
    }
}

object AppTheme {
    val colorScheme: androidx.compose.material3.ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography

    val spacing: Spacing
        @Composable
        get() = LocalSpacing.current
}

/**
 * A wrapper for Previews that ensures the Theme and a Background Surface are applied.
 */
@Composable
fun MooCamPreview(content: @Composable () -> Unit) {
    MooCamTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
