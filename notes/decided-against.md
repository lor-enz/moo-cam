# Decided against

- Android 9 (API 28): saving photos there needed `WRITE_EXTERNAL_STORAGE` and its own permission handling, so the app requires Android 10 instead (b42b990).
- Blinking the screen white as the flash of the selfie camera: jarring, and the fast patterns felt like they could trigger a seizure. The flash button is hidden on the selfie camera instead; see [Selfie camera](selfie-camera.md).
- Three short flashes (150 ms on and off, 3.3 per second): over the limit of 3 flashes per second, aimed at a kid's face. Replaced by two longer flashes, 1.5 per second; a saved choice carries over to them.
