package de.openhabvoice.data

/**
 * Speech recognition engine (STT).
 *
 * Motivation: on Amazon FireOS devices there is no Google speech service
 * (`SpeechRecognizer.isRecognitionAvailable()` returns false), so the system
 * engine cannot be used there. The offline engine (Vosk, on-device) works
 * without any speech service.
 *
 * Display texts (label/description) deliberately live NOT here but as
 * string resources (see SettingsScreen) so they can be localized.
 */
enum class SttEngine {

    /** Use the system engine if available, otherwise fall back to offline. */
    AUTO,

    /** Always use the system engine (Google / device speech service). */
    SYSTEM,

    /** Always use the offline engine (Vosk, on-device, no speech service needed). */
    OFFLINE,
}
