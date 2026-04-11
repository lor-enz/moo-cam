package kiwi.lorenz.moocam.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import kiwi.lorenz.moocam.R

@Composable
fun MooCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val lightColorScheme = lightColorScheme(
        primary = colorResource(R.color.kiwi_green),
        onPrimary = colorResource(R.color.off_white),
        secondary = colorResource(R.color.kiwi_brown),
        onSecondary = colorResource(R.color.white),
        tertiary = colorResource(R.color.light_green),
        onTertiary = colorResource(R.color.dark_green),
        background = colorResource(R.color.off_white),
        onBackground = colorResource(R.color.dark_charcoal),
        surface = colorResource(R.color.off_white),
        onSurface = colorResource(R.color.dark_charcoal),
        outline = colorResource(R.color.gallery)
    )

    val darkColorScheme = darkColorScheme(
        primary = colorResource(R.color.kiwi_green),
        onPrimary = colorResource(R.color.off_white),
        secondary = colorResource(R.color.kiwi_brown),
        onSecondary = colorResource(R.color.white),
        tertiary = colorResource(R.color.dark_green),
        onTertiary = colorResource(R.color.light_green),
        background = colorResource(R.color.dark_charcoal),
        onBackground = colorResource(R.color.off_white),
        surface = colorResource(R.color.dark_charcoal),
        onSurface = colorResource(R.color.off_white),
        outline = colorResource(R.color.gallery)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
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
