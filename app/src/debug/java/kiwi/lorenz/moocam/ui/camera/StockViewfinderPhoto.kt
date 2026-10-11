package kiwi.lorenz.moocam.ui.camera

import androidx.annotation.DrawableRes
import kiwi.lorenz.moocam.R

// Shown instead of the camera image when toggled, for store listing screenshots.
// The photo lives in the debug source set, so release builds don't ship it.
@DrawableRes
val stockViewfinderPhoto: Int? = R.drawable.stock_family_photo
