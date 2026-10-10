package kiwi.lorenz.moocam.flash

import androidx.annotation.DrawableRes
import kiwi.lorenz.moocam.R

// Blinks the flashlight together with the sound, to catch the kid's eye as well as their ear.
// The pattern lists how long the light is on, off, on, … in milliseconds. Every pattern ends
// at least a second before the photo, so the exposure has settled again by then.
enum class AttentionFlash(
    val displayName: String,
    // For the chip on the viewfinder, which shares its row with the sound chip
    val shortName: String,
    @DrawableRes val iconRes: Int,
    val pattern: List<Long>
) {
    OFF("Off", "Off", R.drawable.flash_off, emptyList()),
    LONG("One long flash", "Long", R.drawable.flash, listOf(1000L)),
    SHORT("One short flash", "Short", R.drawable.flash, listOf(200L)),
    THREE_SHORT("Three short flashes", "3× short", R.drawable.flash, listOf(150L, 150L, 150L, 150L, 150L)),
    // Picks one of the flashing patterns each time, so the kid doesn't get used to it
    RANDOM("Random", "Random", R.drawable.shuffle_variant, emptyList());

    companion object {
        val patterns = listOf(LONG, SHORT, THREE_SHORT)
    }
}
