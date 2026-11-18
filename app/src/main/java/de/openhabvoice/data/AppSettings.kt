package de.openhabvoice.data

data class AppSettings(
    /** Base URL of the openHAB server, e.g. "http://192.168.1.10:8080" */
    val serverUrl: String = "",
    val chatMode: ChatMode = ChatMode.OPENHAB5_CHAT,
    /** Only relevant for ChatMode.CUSTOM */
    val customPath: String = "",
    /** openHAB API token (Authorization: Bearer ... / X-OpenHAB-Token) */
    val apiToken: String = "",
    /** Optional basic auth credentials */
    val username: String = "",
    val password: String = "",
    val ttsEnabled: Boolean = true,
    val sttLanguage: String = "de-DE",
    /** Which speech recognition engine to use (system service or offline Vosk). */
    val sttEngine: SttEngine = SttEngine.AUTO,
    /** Conversation ID for the openHAB 5 chat (context is kept server-side per ID) */
    val conversationId: String = ""
) {
    val hasServer: Boolean get() = serverUrl.isNotBlank()
}
