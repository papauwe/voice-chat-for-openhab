package de.openhabvoice

import android.content.Context

/**
 * User-visible texts of the chat layer.
 *
 * [DefaultChatStrings] is the English source of truth used by plain JVM unit
 * tests (no Robolectric, no resources); [ResourceChatStrings] resolves the
 * localized strings from strings.xml and is used in production.
 */
interface ChatStrings {
    val greeting: String
    val newConversation: String
    val unknownError: String
}

object DefaultChatStrings : ChatStrings {
    override val greeting =
        "Hi! Tap the microphone and ask openHAB something - the answer appears here and is read aloud. 🎙️"
    override val newConversation = "New conversation. What can openHAB do for you?"
    override val unknownError = "Unknown error"
}

class ResourceChatStrings(private val context: Context) : ChatStrings {
    override val greeting get() = context.getString(R.string.chat_greeting)
    override val newConversation get() = context.getString(R.string.chat_new_conversation)
    override val unknownError get() = context.getString(R.string.unknown_error)
}
