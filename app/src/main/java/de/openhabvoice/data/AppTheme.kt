package de.openhabvoice.data

/**
 * Accent color scheme of the app.
 *
 * The presets provide hand-tuned Material 3 palettes (dark + light).
 * DYNAMIC uses the personal Material You colors of the device (Android 12+)
 * and falls back to the openHAB scheme on older systems (e.g. FireOS).
 *
 * Display texts (label/description) deliberately live NOT here but as
 * string resources (see SettingsScreen) so they can be localized.
 */
enum class AppTheme {

    /** Classic openHAB orange (default, identical to the original app look). */
    OPENHAB,

    /** Calm blue accent colors. */
    BLUE,

    /** Fresh green accent colors. */
    GREEN,

    /** Violet accent colors. */
    VIOLET,

    /** Material You: personal system colors from the wallpaper (Android 12+). */
    DYNAMIC,
}