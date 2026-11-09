# Index

Notes that don't load every session. Paths are under `app/src/main/java/kiwi/lorenz/moocam/` unless they start with `app/`.

- [Release builds](release-builds.md): R8 and `mapping.txt`. Applies to `app/build.gradle.kts`, `app/proguard-rules.pro`. Read when changing the release config, or code that uses reflection or serialization.
- [Option pickers](option-pickers.md): popover pickers, and the countdown stopping the flash preview. Applies to `ui/camera/OptionPicker.kt`, `ui/camera/SettingsRail.kt`, `flash/`. Read when touching a picker or the countdown.
- [Orientation](orientation.md): portrait layout, upright signage, photo rotation. Applies to `ui/camera/UprightSignage.kt`, `ui/camera/CameraViewModel.kt`. Read when adding UI over the viewfinder or changing capture rotation.
- [Gallery button](gallery-button.md): stored photo URI, no read permission, behavior after a reinstall. Applies to `ui/camera/CameraScreen.kt`, `ui/camera/CameraViewModel.kt`, `settings/`. Read when touching the gallery button or how photos are saved.
- [Theme](theme.md): the dark look and accent contrast. Applies to `ui/theme/`. Read when adding colors or content on the accent.
- [Decided against](decided-against.md): what was tried and dropped. Read before proposing a larger change.
- [Known issues](known-issues.md): open bugs. Read when debugging.
- [Planned changes](planned.md): what comes next. Read when picking up new work.
