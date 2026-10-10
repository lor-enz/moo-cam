# Moo Cam

An Android camera app for photographing toddlers. The user picks a timer delay and taps the capture button; shortly before the photo is taken, an animal sound plays to make the kid look at the camera. Photos are saved to the gallery (`DCIM/MooCam`).

Built with Kotlin, Jetpack Compose, CameraX and Hilt. Requires Android 10 (API 29) or newer.

## How to work here

- Commit messages use [gitmoji](https://gitmoji.dev). Follow the rules in [`gitmoji.md`](gitmoji.md) for every commit: exactly one gitmoji from its list, an imperative subject of at most 72 characters, one logical change per commit.
- Don't add Claude as co-author or any other AI attribution to commit messages.
- Keep permissions to the minimum the app needs. Don't add broad ones like `READ_MEDIA_IMAGES` just to improve a feature; e.g. the gallery button only shows photos taken in the current session on purpose.

## Testing

- Check that the app builds with `./gradlew :app:assembleDebug`. There are no meaningful automated tests yet.
- A passing build doesn't prove a change works. For anything the user can see or hear (camera, sounds, permissions, UI), ask the user to verify it on their real device and say what to check.
- Only consider a change tested once the user confirms it. If they don't respond to that, treat it as untested; never assume it works.
