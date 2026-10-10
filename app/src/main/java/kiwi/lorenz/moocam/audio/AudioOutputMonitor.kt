package kiwi.lorenz.moocam.audio

import android.content.Context
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

data class AudioOutputState(
    // Media volume from 0 to 1
    val mediaVolume: Float,
    // Headphones or Bluetooth audio connected, so the moo might not be heard by the kid
    val isPrivateOutputConnected: Boolean
)

@Singleton
class AudioOutputMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val privateOutputTypes = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET, // API 31+, never reported on older devices
        AudioDeviceInfo.TYPE_BLE_SPEAKER, // API 31+, never reported on older devices
    )

    // Emits the current state, then again whenever volume or connected outputs change
    val state: Flow<AudioOutputState> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        fun update() { trySend(currentState()) }

        // AudioManager has no public volume listener, but volume changes are written to system settings
        val volumeObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) = update()
        }
        // Switching outputs also switches the volume, so update both on device changes
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = update()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = update()
        }

        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        update()

        awaitClose {
            context.contentResolver.unregisterContentObserver(volumeObserver)
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        }
    }.distinctUntilChanged()

    private fun currentState(): AudioOutputState {
        val min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volume = if (max > min) (current - min).toFloat() / (max - min) else 0f

        val isPrivateOutputConnected = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type in privateOutputTypes }

        return AudioOutputState(volume, isPrivateOutputConnected)
    }
}
