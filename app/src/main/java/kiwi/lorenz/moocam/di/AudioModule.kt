package kiwi.lorenz.moocam.di

import android.media.AudioAttributes
import android.media.SoundPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSoundPool(): SoundPool {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        return SoundPool.Builder()
            .setMaxStreams(3) // animal sound + shutter + possible overlap
            .setAudioAttributes(audioAttributes)
            .build()
    }
}