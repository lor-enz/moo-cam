package kiwi.lorenz.moocam.capture

import androidx.annotation.DrawableRes
import kiwi.lorenz.moocam.R

// How many photos one countdown takes. Kids look at the camera only briefly and some take
// a moment to react to the sound, so a burst makes it more likely that one photo catches the look.
enum class CaptureMode(
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val photoCount: Int
) {
    SINGLE("Single photo", R.drawable.camera, 1),
    BURST_3("Burst of 3", R.drawable.camera_burst, 3),
    BURST_5("Burst of 5", R.drawable.camera_burst, 5)
}
