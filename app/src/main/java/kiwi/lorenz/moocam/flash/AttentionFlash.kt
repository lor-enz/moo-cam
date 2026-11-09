package kiwi.lorenz.moocam.flash

import androidx.annotation.DrawableRes
import kiwi.lorenz.moocam.R

// Blinks the flashlight together with the sound, to catch the kid's eye as well as their ear.
// The pattern lists how long the light is on, off, on, … in milliseconds. Every pattern ends
// at least a second before the photo, so the exposure has settled again by then. Nothing blinks
// more than 3 times a second, which can trigger seizures; the light is aimed at a kid's face.
enum class AttentionFlash(
    val displayName: String,
    // For the caption in the settings rail, which has room for little text
    val shortName: String,
    @DrawableRes val iconRes: Int,
    val pattern: List<Long>
) {
    OFF("Off", "Off", R.drawable.flash_off, emptyList()),
    LONG("One long flash", "Long", R.drawable.flash, listOf(1000L)),
    SHORT("One short flash", "Short", R.drawable.flash, listOf(200L)),
    TWO("Two flashes", "2×", R.drawable.flash, listOf(350L, 300L, 350L)),
    // Picks one of the flashing patterns each time, so the kid doesn't get used to it
    RANDOM("Random", "Random", R.drawable.shuffle_variant, emptyList());

    companion object {
        val patterns = listOf(LONG, SHORT, TWO)
    }
}
