package kiwi.lorenz.moocam.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kiwi.lorenz.moocam.audio.AnimalSound
import kiwi.lorenz.moocam.audio.SoundChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Settings that survive app restarts. Missing or unknown values come back as null,
// so callers decide on the default.
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val selectedSoundKey = stringPreferencesKey("selected_sound")
    private val selectedDelayKey = intPreferencesKey("selected_delay")

    // Saved in the sound's place; no AnimalSound has this name
    private val surpriseValue = "SURPRISE"

    // If the file can't be read, carry on with defaults instead of crashing
    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    // Stored by enum name, so reordering or adding sounds doesn't change saved choices
    val selectedSound: Flow<SoundChoice?> = prefs
        .map { prefs ->
            val saved = prefs[selectedSoundKey]
            if (saved == surpriseValue) SoundChoice.Surprise
            else AnimalSound.entries.find { it.name == saved }?.let { SoundChoice.Fixed(it) }
        }
        .distinctUntilChanged()

    val selectedDelay: Flow<Int?> = prefs
        .map { prefs -> prefs[selectedDelayKey] }
        .distinctUntilChanged()

    suspend fun setSelectedSound(choice: SoundChoice) {
        val value = when (choice) {
            is SoundChoice.Fixed -> choice.sound.name
            SoundChoice.Surprise -> surpriseValue
        }
        dataStore.edit { it[selectedSoundKey] = value }
    }

    suspend fun setSelectedDelay(seconds: Int) {
        dataStore.edit { it[selectedDelayKey] = seconds }
    }
}
