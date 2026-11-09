package kiwi.lorenz.moocam.ui.theme

import androidx.compose.ui.graphics.Color

private val Cream = Color(0xFFF3E8D3)
private val Charcoal = Color(0xFF333333)
private val Gray = Color(0xFF666666)
private val Caramel = Color(0xFFC2784D)
private val DarkCaramel = Color(0xFF8A4E2A)
private val CameraRed = Color(0xFFC81D25)

object AppColors {
    // Behind the viewfinder
    val cameraBackground = Color.Black

    // The capture button and whatever is selected. Content on it needs onAccent, not onDark.
    val accent = Cream
    val onAccent = Charcoal

    // Content on anything dark: the bar below the viewfinder and the overlays on it
    val onDark = Cream
    val controlsBackground = Charcoal
    val outline = Gray

    // Controls drawn on top of the viewfinder
    val overlayBackground = Color.Black.copy(alpha = 0.7f)
    val thumbnailPlaceholder = Color.White.copy(alpha = 0.1f)

    // Round buttons beside the capture button, e.g. to switch cameras
    val sideButton = Color.White.copy(alpha = 0.1f)

    // Dims the screen behind a panel
    val scrim = Color.Black.copy(alpha = 0.4f)
    val panel = Cream
    val onPanel = Charcoal
    val onWarning = DarkCaramel

    // Capture button while the timer runs and while the photo is taken
    val counting = Caramel
    val onCounting = Charcoal
    val capturing = CameraRed
    val onCapturing = Cream
}
