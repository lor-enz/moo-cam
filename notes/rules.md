# Rules

Rules that apply everywhere. The rules for how to work and test are in [How to work here](how-to-work.md) and [Testing](testing.md).

- Ask for no permission beyond what the app needs, e.g. no `READ_MEDIA_IMAGES`. → [Product philosophy](product-philosophy.md)
- One dark theme only: no light theme, no dynamic color; colors only from `AppColors`. → [Theme](theme.md)
- Cream accents only on the bar or an overlay; content on them uses `onAccent`. → [Theme](theme.md)
- Pickers are popovers that apply on tap; no dialogs, no OK or Cancel. → [Option pickers](option-pickers.md)
- The layout stays in portrait; turn signage with `upright()` / `uprightPanel()`, not by rotating the screen. → [Orientation](orientation.md)
- Test reflection and name lookups in a release build; debug builds skip R8. → [Release builds](release-builds.md)
