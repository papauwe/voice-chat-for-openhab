package de.openhabvoice.data

/**
 * Wie die App mit dem openHAB-Server spricht.
 *
 * Anzeigetexte (Label/Beschreibung) bewusst NICHT hier, sondern als
 * String-Ressourcen (siehe SettingsScreen) – damit sie lokalisiert sind.
 */
enum class ChatMode(val defaultPath: String) {

    /** openHAB 5 Chat (Main-UI-Assistent): POST /rest/voice/interpreters?conversation=… mit Klartext-Antwort. */
    OPENHAB5_CHAT("/rest/voice/interpreters"),

    /** HABot-Chat: liefert echte Antworttexte und hält den Konversationskontext (HLI/LLM). */
    HABOT("/rest/habot/chat"),

    /** Voice Interpreter: führt den Befehl über die Human Language Interpreters aus. */
    VOICE("/rest/voice/interpret"),

    /** Frei konfigurierbarer Endpoint (z. B. eigene Bridge), erwartet JSON-Antwort mit "answer". */
    CUSTOM("")
}
