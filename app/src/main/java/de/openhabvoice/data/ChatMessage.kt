package de.openhabvoice.data

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}
