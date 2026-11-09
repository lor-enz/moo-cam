# Testing

- Check that the app builds with `./gradlew :app:assembleDebug`.
- Unit tests run on the JVM with `./gradlew :app:testDebugUnitTest`. There are only the template's examples so far; add tests when changing logic that runs without a device (e.g. `CameraViewModel`, `SettingsRepository`, `SoundRepository`).
- The emulator is for the UI: layout, the permission screens, pickers and settings, the countdown. Its camera shows a fake scene and can't zoom, which hides the zoom presets; to see them, feed `ZoomPresetRow` fake presets temporarily. Leave to the user what needs real hardware: photo quality, when the sound plays relative to the photo, the flashlight, speaker vs. headphones. Tell them what to check.
- Only consider such a change tested once the user confirms it. If they don't respond to that, treat it as untested; never assume it works.
- Use only an already running emulator (`Emu_Pixel_9a_No_Google`, Android 16, for everyday testing). If none is running or it misbehaves (hanging installs, very slow responses, unresponsive adb), stop and tell the user instead of waiting or working around it. Only one runs at a time; ask the user if a test needs another.
- Only test on the emulator unless the user says otherwise; their own phone may be connected via adb too.
- Helpers in `scripts/test/` use the running emulator, never a connected phone unless `ANDROID_SERIAL` names it. Install with `install.sh`, not `./gradlew installDebug`, which installs on every connected device.
  - `prepare-device.sh` turns animations off, which keeps screenshots stable. Run it before testing; an emulator booted from an older snapshot has them on again.
  - `ui.sh list|tap|hold|text|type|wait|back|home|shot` reads and drives the screen through `uiautomator`, finding elements by test tag, text or description.
