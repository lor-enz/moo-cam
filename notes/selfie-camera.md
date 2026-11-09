# Selfie camera

- The button right of the capture button switches between the back and the selfie camera. The choice isn't stored: the app always starts with the back camera. Phones without a selfie camera don't get the button.
- Only the viewfinder is mirrored; photos are saved unmirrored, like the system camera app's. CameraX's `ImageCapture` doesn't mirror unless asked to.
- While switching, the old viewfinder stays until the new camera is ready, so the controls don't disappear for a moment.
- Selfie cameras have no flashlight, so the flash button is hidden there; blinking the screen instead was dropped, see [Decided against](decided-against.md). The emulator's selfie camera reports a flashlight anyway, so it shows the button.
