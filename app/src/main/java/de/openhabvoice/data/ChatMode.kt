package de.openhabvoice.data

/**
 * Wie die App mit dem openHAB-Server spricht.
 */
enum class ChatMode(val label: String, val defaultPath: String, val description: String) {

    /** openHAB 5 Chat (Main-UI-Assistent): POST /rest/voice/interpreters?conversation=… mit Klartext-Antwort. */
    OPENHAB5_CHAT(
        "openHAB 5 Chat",
        "/rest/voice/interpreters",
        "Der neue Main-UI-Assistent (LLM), Verlauf serverseitig pro Conversation-ID"
    ),

    /** HABot-Chat: liefert echte Antworttexte und hält den Konversationskontext (HLI/LLM). */
    HABOT(
        "HABot Chat",
        "/rest/habot/chat",
        "POST /rest/habot/chat – braucht das HABot-Add-on"
    ),

    /** Voice Interpreter: führt den Befehl über die Human Language Interpreters aus. */
    VOICE(
        "Voice Interpreter",
        "/rest/voice/interpret",
        "POST /rest/voice/interpret?text=… – führt Befehle aus, aber ohne Antworttext"
    ),

    /** Frei konfigurierbarer Endpoint (z. B. eigene Bridge), erwartet JSON-Antwort mit "answer". */
    CUSTOM(
        "Eigener Endpoint",
        "",
        "POST <Pfad> mit {\"message\": \"…\"} – Antwort wird aus answer/text/message gelesen"
    )
}
