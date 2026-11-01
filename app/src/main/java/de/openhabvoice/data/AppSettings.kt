package de.openhabvoice.data

data class AppSettings(
    /** Basis-URL des openHAB-Servers, z. B. "http://192.168.1.10:8080" */
    val serverUrl: String = "",
    val chatMode: ChatMode = ChatMode.OPENHAB5_CHAT,
    /** Nur relevant für ChatMode.CUSTOM */
    val customPath: String = "",
    /** openHAB API-Token (Authorization: Bearer … / X-OpenHAB-Token) */
    val apiToken: String = "",
    /** Optional: Basic-Auth */
    val username: String = "",
    val password: String = "",
    val ttsEnabled: Boolean = true,
    val sttLanguage: String = "de-DE",
    /** Conversation-ID für den openHAB-5-Chat (Kontext bleibt serverseitig pro ID erhalten) */
    val conversationId: String = ""
) {
    val hasServer: Boolean get() = serverUrl.isNotBlank()
}
