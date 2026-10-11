# Moo Cam

The secret weapon of toddler photography.
A timer that blinks and moos — so you don't have to.

## How?

Getting a great photo of a toddler is basically impossible. They look everywhere except the camera. You end up with a hundred shots of the side of their head.
MooCam fixes that.
Start the timer, and in the final seconds your phone's flashlight starts blinking and an animal sound plays — just long enough to make them look up, tilt their head, and give you that perfect shot.
That's it. No subscriptions. No ads. No nonsense. Just you, your kid, and a well-timed moo.

## Play Store Listing Information:

Images

1. "Tired of bad photos?"
   Screenshot of Moo Cam App viewfinder - [Kid is looking away]
2. "A moo grabs their attention"
  Screenshot of Moo Cam App viewfinder with "Moo" Text overlayed next to a speaker icon - [Kid is looking towards the camera surprised]
3. "Perfect Photo" 
  Screenshot of Moo Cam App viewfinder - [Kid is smiling towards camera]


## Detailed Feature List

### Done

- Camera permission handling with friendly explainer screens 
- Camera viewfinder with aspect ratio matching the final photo
- Selectable timer Delay UI
- Capture button showing countdown
- Animal sound playing 2 seconds before photo
- Photo saved to gallery
- Multiple rear camera support
- Selectable animal sounds
- Show recently taken photos in-app
- Tap to focus
- Blinking flashlight in final countdown seconds

## Releasing

Releases to Google Play are published by a GitHub Action ([`.github/workflows/release.yml`](.github/workflows/release.yml)).

1. Increase `versionCode` in [`version.properties`](version.properties) by one, and update `versionName` (the version users see, e.g. `1.1`).
2. Commit it, e.g. `🔖 Release version 1.1`, and push to `main`.
3. The action builds a signed bundle, uploads it to the **internal testing** track as a draft and tags the commit as `v<versionCode>`.
4. In the Play Console, review the draft and roll it out, or promote it to closed testing or production.

The action only runs when `version.properties` changes on `main`, and skips a `versionCode` that already has a tag, so every version is released once. Play rejects a `versionCode` that isn't higher than the last upload; the action then fails without publishing anything.

It needs these repository secrets:

| Secret | Content |
| --- | --- |
| `UPLOAD_KEYSTORE_BASE64` | The upload keystore, base64-encoded (`base64 -w0 upload.jks`) |
| `UPLOAD_KEYSTORE_PASSWORD` | The keystore password |
| `UPLOAD_KEY_ALIAS` | The key alias, e.g. `upload` |
| `UPLOAD_KEY_PASSWORD` | The key password (the same as the keystore password for PKCS12 keystores) |
| `PLAY_SERVICE_ACCOUNT_JSON` | The JSON key of a Google service account with permission to release Moo Cam to testing tracks |

To build a release locally, create `keystore.properties` in the project root (git ignores it) and run `./gradlew :app:bundleRelease`:

```
storeFile=/path/to/upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```
