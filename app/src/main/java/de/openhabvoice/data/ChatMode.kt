package de.openhabvoice.data

/**
 * How the app talks to the openHAB server.
 *
 * Display texts (label/description) deliberately live NOT here but as
 * string resources (see SettingsScreen) so they can be localized.
 */
enum class ChatMode(val defaultPath: String) {

    /** openHAB 5 chat (Main UI assistant): POST /rest/voice/interpreters?conversation=... with a plain-text answer. */
    OPENHAB5_CHAT("/rest/voice/interpreters"),

    /** HABot chat: returns real answer texts and keeps the conversation context (HLI/LLM). */
    HABOT("/rest/habot/chat"),

    /** Voice interpreter: executes the command via the human language interpreters. */
    VOICE("/rest/voice/interpret"),

    /** Freely configurable endpoint (e.g. a custom bridge), expects a JSON answer with "answer". */
    CUSTOM("")
}
