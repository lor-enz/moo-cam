package kiwi.lorenz.moocam.ui.camera

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kiwi.lorenz.moocam.R
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.audio.SoundChoice
import kiwi.lorenz.moocam.flash.AttentionFlash
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

// Shows a current choice on top of the viewfinder, e.g. the sound that plays before the
// photo; tapping it opens the matching picker
@Composable
fun ViewfinderChip(
    @DrawableRes iconRes: Int,
    text: String,
    onClickLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
            .clickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(R.color.off_white),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(spacing.small))
        Text(
            text = text,
            style = AppTheme.typography.bodyLarge,
            color = colorResource(R.color.off_white)
        )
    }
}

// Tapping an option previews and selects it. Only Cancel and OK close the dialog, so a tap
// next to it or the back button can't throw away the choice by accident.
// Options must be saveable in a Bundle (e.g. enums or Serializable), to keep the pending
// choice across configuration changes.
@Composable
fun <T> OptionPickerDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    label: (T) -> String,
    icon: (T) -> Int,
    onOptionTap: (T) -> Unit,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        OptionPickerContent(title, options, selectedOption, label, icon, onOptionTap, onConfirm, onDismiss)
    }
}

@Composable
private fun <T> OptionPickerContent(
    title: String,
    options: List<T>,
    selectedOption: T,
    label: (T) -> String,
    icon: (T) -> Int,
    onOptionTap: (T) -> Unit,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit
) {
    // Only applied when OK is tapped; Cancel keeps the previous option
    var pendingOption by rememberSaveable { mutableStateOf(selectedOption) }

    Column(
        modifier = Modifier
            .background(colorResource(R.color.off_white), RoundedCornerShape(24.dp))
            .padding(spacing.large)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.headlineMedium,
            color = colorResource(R.color.dark_charcoal)
        )
        Spacer(Modifier.height(spacing.medium))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            options.forEach { option ->
                val isSelected = option == pendingOption
                val shape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(shape)
                        // Unselected rows get an invisible border, so nothing shifts on selection
                        .border(
                            width = 2.dp,
                            color = if (isSelected) colorResource(R.color.kiwi_green) else Color.Transparent,
                            shape = shape
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                pendingOption = option
                                onOptionTap(option)
                            }
                        )
                        .padding(horizontal = spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(icon(option)),
                        contentDescription = null,
                        tint = colorResource(R.color.dark_charcoal),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(spacing.medium))
                    Text(
                        text = label(option),
                        style = AppTheme.typography.bodyLarge,
                        color = colorResource(R.color.dark_charcoal)
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    color = colorResource(R.color.dark_charcoal)
                )
            }
            Spacer(Modifier.width(spacing.small))
            Button(
                onClick = { onConfirm(pendingOption) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.kiwi_green),
                    contentColor = colorResource(R.color.off_white)
                )
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

@Preview
@Composable
private fun PreviewViewfinderChip() {
    MooCamTheme {
        ViewfinderChip(
            iconRes = android.R.drawable.ic_lock_silent_mode_off,
            text = AnimalSound.MOO.displayName,
            onClickLabel = "",
            enabled = true,
            onClick = {}
        )
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
            onOptionTap = {},
            onConfirm = {},
            onDismiss = {}
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
            onOptionTap = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
