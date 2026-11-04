package kiwi.lorenz.moocam.ui.camera

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

// Stacks the settings on the viewfinder
@Composable
fun SettingsRail(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        content = content
    )
}

// Shows a current choice, e.g. the sound that plays before the photo; tapping it opens the
// matching picker
@Composable
fun SettingsRailButton(
    @DrawableRes iconRes: Int,
    caption: String,
    onClickLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Null disables the long press
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .size(56.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .combinedClickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(R.color.off_white),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = caption,
            style = AppTheme.typography.labelSmall,
            color = colorResource(R.color.off_white),
            maxLines = 1,
            // Shrinks with a large font size, where a long caption wouldn't fit the circle
            autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = 10.sp)
        )
    }
}

@Preview
@Composable
private fun PreviewSettingsRail() {
    MooCamTheme {
        SettingsRail {
            SettingsRailButton(
                iconRes = android.R.drawable.ic_lock_silent_mode_off,
                caption = AnimalSound.MOO.displayName,
                onClickLabel = "",
                enabled = true,
                onClick = {}
            )
            SettingsRailButton(
                iconRes = R.drawable.flash,
                caption = "3× short",
                onClickLabel = "",
                enabled = true,
                onClick = {}
            )
            SettingsRailButton(
                iconRes = R.drawable.camera_burst,
                caption = "3",
                onClickLabel = "",
                enabled = false,
                onClick = {}
            )
        }
    }
}
