package kiwi.lorenz.moocam.audio

import androidx.annotation.RawRes
import kiwi.lorenz.moocam.R

enum class AnimalSound(@RawRes val resId: Int, val displayName: String) {
    MOO(R.raw.moo_1, "Moo"),
    MOO_2(R.raw.moo_2, "Moo 2"),
    MEOW(R.raw.meow_1, "Meow"),
    YIPPEE(R.raw.yipee, "Yippee")
}