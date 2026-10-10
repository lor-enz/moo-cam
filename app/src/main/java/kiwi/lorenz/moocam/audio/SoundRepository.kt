package kiwi.lorenz.moocam.audio

import android.content.Context
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kiwi.lorenz.moocam.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(
    private val soundPool: SoundPool,
    @ApplicationContext private val context: Context
) {
    // animal sounds — loaded by ID, keyed by enum
    private val animalSoundIds = mutableMapOf<AnimalSound, Int>()

    // shutter sound ID
    private var shutterSoundId: Int = 0

    init {
        // load all animal sounds upfront
        AnimalSound.entries.forEach { sound ->
            animalSoundIds[sound] = soundPool.load(context, sound.resId, 1)
        }

        // load shutter sound — pick whichever one you decide to keep
        shutterSoundId = soundPool.load(context, R.raw.shutter_click_normalized_trim, 1)
    }

    fun playAnimalSound(sound: AnimalSound) {
        animalSoundIds[sound]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playShutter() {
        soundPool.play(shutterSoundId, 1f, 1f, 1, 0, 1f)
    }
}