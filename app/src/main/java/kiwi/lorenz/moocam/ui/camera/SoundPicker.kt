package kiwi.lorenz.moocam.ui.camera

import androidx.compose.foundation.background
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import kiwi.lorenz.moocam.ui.theme.AppTheme
import kiwi.lorenz.moocam.ui.theme.AppTheme.spacing
import kiwi.lorenz.moocam.ui.theme.MooCamTheme

// Shows the sound that plays before the photo; tapping it opens the sound picker
@Composable
fun SoundLabel(
    sound: AnimalSound,
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
                onClickLabel = stringResource(R.string.choose_sound),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_lock_silent_mode_off),
            contentDescription = null,
            tint = colorResource(R.color.off_white),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(spacing.small))
        Text(
            text = sound.displayName,
            style = AppTheme.typography.bodyLarge,
            color = colorResource(R.color.off_white)
        )
    }
}

// Tapping a sound plays and selects it. Only Cancel and OK close the dialog, so a tap
// next to it or the back button can't throw away the choice by accident.
@Composable
fun SoundPickerDialog(
    sounds: List<AnimalSound>,
    selectedSound: AnimalSound,
    onSoundTap: (AnimalSound) -> Unit,
    onConfirm: (AnimalSound) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        SoundPickerContent(sounds, selectedSound, onSoundTap, onConfirm, onDismiss)
    }
}

@Composable
private fun SoundPickerContent(
    sounds: List<AnimalSound>,
    selectedSound: AnimalSound,
    onSoundTap: (AnimalSound) -> Unit,
    onConfirm: (AnimalSound) -> Unit,
    onDismiss: () -> Unit
) {
    // Only applied when OK is tapped; Cancel keeps the previous sound
    var pendingSound by rememberSaveable { mutableStateOf(selectedSound) }

    Column(
        modifier = Modifier
            .background(colorResource(R.color.off_white), RoundedCornerShape(24.dp))
            .padding(spacing.large)
    ) {
        Text(
            text = stringResource(R.string.choose_sound),
            style = AppTheme.typography.headlineMedium,
            color = colorResource(R.color.dark_charcoal)
        )
        Spacer(Modifier.height(spacing.medium))
        Column(modifier = Modifier.selectableGroup()) {
            sounds.forEach { sound ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = sound == pendingSound,
                            role = Role.RadioButton,
                            onClick = {
                                pendingSound = sound
                                onSoundTap(sound)
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sound == pendingSound,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorResource(R.color.kiwi_green),
                            unselectedColor = colorResource(R.color.dark_charcoal)
                        )
                    )
                    Spacer(Modifier.width(spacing.medium))
                    Text(
                        text = sound.displayName,
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
                onClick = { onConfirm(pendingSound) },
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
private fun PreviewSoundLabel() {
    MooCamTheme {
        SoundLabel(sound = AnimalSound.MOO, enabled = true, onClick = {})
    }
}

@Preview
@Composable
private fun PreviewSoundPicker() {
    MooCamTheme {
        SoundPickerContent(
            sounds = AnimalSound.entries,
            selectedSound = AnimalSound.MEOW,
            onSoundTap = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
