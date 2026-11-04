# Moo Cam

An Android camera app for photographing toddlers. The user picks a timer delay and taps the capture button; shortly before the photo is taken, an animal sound plays to make the kid look at the camera. Photos are saved to the gallery (`DCIM/MooCam`).

Built with Kotlin, Jetpack Compose, CameraX and Hilt. Requires Android 10 (API 29) or newer.

## How to work here

- Commit messages use [gitmoji](https://gitmoji.dev). Follow the rules in [`gitmoji.md`](gitmoji.md) for every commit: exactly one gitmoji from its list, an imperative subject of at most 72 characters, one logical change per commit.
- Commit only when the user asks. Propose the split first; every commit must build and pass the unit tests on its own.
- Work in milestones. After each one: build, run the unit tests, test the UI on the emulator, then report to the user, who tests on a real phone (see Testing).
- Keep permissions to the minimum the app needs. Don't add broad ones like `READ_MEDIA_IMAGES` just to improve a feature; e.g. the gallery button only shows photos taken in the current session on purpose.
- Packages are by feature: `audio` (animal sounds, audio output), `flash` (the blinking flashlight), `capture` (capture modes), `settings` (stored choices). Screens live in `ui/<screen>` (`camera`, `permission`) with their view models; `ui` itself holds only what several screens share (`SegmentedControl`, `theme`). `di` holds the Hilt modules. Put a new file in the package of its feature, or propose a new one when it fits none.
- Check the package of every file you touch: a file used by only one feature belongs in that feature's package, one used by several goes up to `ui`. If a move is advised, ask the user first, and move in a `🚚` commit of its own (moves only, so blame follows the rename).
- Give new UI elements a test tag in `ui/TestTags.kt` in the same change that adds them, so they can be driven by adb (see Testing).
- Record decisions and findings under Project notes as they come up, and update this file and the readme in the commit they belong to, not in a separate one. Keep entries short and remove ones that no longer apply, so this file stays small.

## Testing

- Check that the app builds with `./gradlew :app:assembleDebug`.
- Unit tests run on the JVM with `./gradlew :app:testDebugUnitTest`. There are only the template's examples so far; add tests when changing logic that runs without a device (e.g. `CameraViewModel`, `SettingsRepository`, `SoundRepository`).
- The emulator is for the UI: layout, the permission screens, pickers and settings, the countdown. Its camera shows a fake scene, so leave to the user what needs real hardware: photo quality, when the sound plays relative to the photo, the flashlight, speaker vs. headphones. Tell them what to check.
- Only consider such a change tested once the user confirms it. If they don't respond to that, treat it as untested; never assume it works.
- Use only an already running emulator (`Emu_Pixel_9a_No_Google`, Android 16, for everyday testing). If none is running or it misbehaves (hanging installs, very slow responses, unresponsive adb), stop and tell the user instead of waiting or working around it. Only one runs at a time; ask the user if a test needs another.
- Only test on the emulator unless the user says otherwise; their own phone may be connected via adb too.
- Helpers in `scripts/test/` use the running emulator, never a connected phone unless `ANDROID_SERIAL` names it. Install with `install.sh`, not `./gradlew installDebug`, which installs on every connected device.
  - `prepare-device.sh` turns animations off, which keeps screenshots stable. Run it before testing; an emulator booted from an older snapshot has them on again.
  - `ui.sh list|tap|hold|text|type|wait|back|home|shot` reads and drives the screen through `uiautomator`, finding elements by test tag, text or description.

## Project notes

- The option pickers are popovers inside the viewfinder, not dialogs: a tap applies an option at once, so nothing needs OK or Cancel. The camera stays usable while one is open, so the countdown has to stop a running flash preview first.
- The layout stays in portrait; `Modifier.upright()` turns signage by how the device is held. Rotation switches 15° past the middle between two orientations, so it can't flicker. Photos use the same value, so they always match what the screen shows.

## Known issues

None yet.

## Planned changes

Revamp of the viewfinder settings, in milestones the user tests on a phone. The settings rail, the pickers and the rotating signage are done.

- Rotate the popover and the volume warning as a whole, and show the headphone banner over the viewfinder in landscape. Turning the phone closes an open popover. Toasts and permission screens stay as they are.
