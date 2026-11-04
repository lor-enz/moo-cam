package kiwi.lorenz.moocam.ui

// Show up as resource ids, so scripts/test/ui.sh can find the elements through adb
object TestTags {
    const val SOUND_BUTTON = "sound_button"
    const val FLASH_BUTTON = "flash_button"
    const val CAPTURE_MODE_BUTTON = "capture_mode_button"
    const val OPTION_PICKER = "option_picker"
    const val CLOSE_OPTION_PICKER = "close_option_picker"
    const val PRIVATE_OUTPUT_BANNER = "private_output_banner"

    fun option(index: Int) = "option_$index"
}
