# Orientation

- The layout stays in portrait; `Modifier.upright()` turns signage by how the device is held, and `uprightPanel()` also swaps the size of elements that aren't square. Rotation switches 15° past the middle between two orientations, so it can't flicker. Photos use the same value, so they always match what the screen shows. Toasts and permission screens don't turn.
