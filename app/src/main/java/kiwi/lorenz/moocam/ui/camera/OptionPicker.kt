package kiwi.lorenz.moocam.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.audio.SoundChoice
import kiwi.lorenz.moocam.flash.AttentionFlash
import kiwi.lorenz.moocam.ui.TestTags
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

// The middle of the edge that faces the settings rail
private val pickerOrigin = TransformOrigin(1f, 0.5f)

// Grows out of its button in the settings rail. Tapping an option applies it right away and
// keeps the picker open, so several options can be tried and there's nothing to confirm.
@Composable
fun <T> OptionPicker(
    visible: Boolean,
    title: String,
    options: List<T>,
    selectedOption: T,
    label: (T) -> String,
    icon: (T) -> Int,
    onOptionTap: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(150)) + scaleIn(tween(200), initialScale = 0.6f, transformOrigin = pickerOrigin),
        exit = fadeOut(tween(100)) + scaleOut(tween(150), targetScale = 0.6f, transformOrigin = pickerOrigin)
    ) {
        OptionPickerContent(title, options, selectedOption, label, icon, onOptionTap)
    }
}

@Composable
private fun <T> OptionPickerContent(
    title: String,
    options: List<T>,
    selectedOption: T,
    label: (T) -> String,
    icon: (T) -> Int,
    onOptionTap: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(spacing.small)
            .testTag(TestTags.OPTION_PICKER)
            .semantics { paneTitle = title }
            .selectableGroup()
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selectedOption
            val background by animateColorAsState(
                targetValue = if (isSelected) colorResource(R.color.kiwi_green) else Color.Transparent,
                label = "optionBackground"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onOptionTap(option) }
                    )
                    .testTag(TestTags.option(index))
                    .padding(horizontal = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(icon(option)),
                    contentDescription = null,
                    tint = colorResource(R.color.off_white),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label(option),
                    style = AppTheme.typography.bodyLarge,
                    color = colorResource(R.color.off_white),
                    maxLines = 1,
                    // Shrinks with a large font size, where a long label wouldn't fit the row
                    autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 15.sp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSoundPicker() {
    MooCamTheme {
        OptionPickerContent(
            title = stringResource(R.string.choose_sound),
            options = AnimalSound.entries.map { SoundChoice.Fixed(it) } + SoundChoice.Surprise,
            selectedOption = SoundChoice.Surprise,
            label = { it.displayName },
            icon = { it.iconRes },
            onOptionTap = {}
        )
    }
}

@Preview
@Composable
private fun PreviewFlashPicker() {
    MooCamTheme {
        OptionPickerContent(
            title = stringResource(R.string.choose_flash),
            options = AttentionFlash.entries,
            selectedOption = AttentionFlash.THREE_SHORT,
            label = { it.displayName },
            icon = { it.iconRes },
            onOptionTap = {}
        )
    }
}
