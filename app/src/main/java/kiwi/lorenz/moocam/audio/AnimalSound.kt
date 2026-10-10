package kiwi.lorenz.moocam.audio

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import kiwi.lorenz.moocam.R

enum class AnimalSound(@RawRes val resId: Int, val displayName: String, @DrawableRes val iconRes: Int) {
    MOO(R.raw.moo_1, "Moo", R.drawable.cow),
    MOO_2(R.raw.moo_2, "Moo 2", R.drawable.cow),
    MEOW(R.raw.meow_1, "Meow", R.drawable.cat),
    YIPPEE(R.raw.yipee, "Yippee", R.drawable.party_popper)
}