package kiwi.lorenz.moocam.audio

import androidx.annotation.DrawableRes
import kiwi.lorenz.moocam.R
import java.io.Serializable

// What plays before the photo: always the same sound, or a surprise. Kids get used to
// hearing the same sound and stop looking, so a surprise picks another one every time.
// Serializable, so the sound picker can keep a pending choice across configuration changes.
sealed interface SoundChoice : Serializable {
    val displayName: String
    @get:DrawableRes val iconRes: Int

    data class Fixed(val sound: AnimalSound) : SoundChoice {
        override val displayName get() = sound.displayName
        override val iconRes get() = sound.iconRes
    }

    data object Surprise : SoundChoice {
        override val displayName = "Surprise"
        override val iconRes = R.drawable.shuffle_variant
    }
}
