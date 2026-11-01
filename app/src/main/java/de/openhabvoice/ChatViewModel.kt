package de.openhabvoice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.openhabvoice.data.AppSettings
import de.openhabvoice.data.ChatMessage
import de.openhabvoice.data.OpenHabApi
import de.openhabvoice.data.OpenHabClient
import de.openhabvoice.data.PrefsSettingsStore
import de.openhabvoice.data.ResourceOpenHabTexts
import de.openhabvoice.data.SettingsStore
import de.openhabvoice.data.stripMarkdown
import de.openhabvoice.voice.SpeechEngine
import de.openhabvoice.voice.SpeechInput
import de.openhabvoice.voice.TtsEngine
import de.openhabvoice.voice.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    app: Application,
    settingsStoreOverride: SettingsStore? = null,
    clientOverride: OpenHabApi? = null,
    ttsOverride: TtsEngine? = null,
    speechOverride: SpeechEngine? = null,
    chatStringsOverride: ChatStrings? = null,
) : AndroidViewModel(app) {

    // Fuer den AndroidViewModelFactory (by viewModels()): er sucht per Reflection
    // nach einem Konstruktor (Application). Kotlin-Default-Parameter erzeugen
    // keinen solchen Ueberladung, daher explizit.
    constructor(app: Application) : this(app, null, null, null, null, null)

    val settingsStore: SettingsStore = settingsStoreOverride ?: PrefsSettingsStore(app)

    private val clientImpl =
        clientOverride ?: OpenHabClient({ settingsStore.settings.value }, ResourceOpenHabTexts(app))
    private val ttsImpl = ttsOverride ?: TtsManager(app)
    private val speechImpl = speechOverride ?: SpeechInput(app)
    private val strings = chatStringsOverride ?: ResourceChatStrings(app)

    val settings: StateFlow<AppSettings> = settingsStore.settings

    val isListening: StateFlow<Boolean> = speechImpl.isListening
    val partialText: StateFlow<String> = speechImpl.partial
    val speechError: StateFlow<String?> = speechImpl.error

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = 0L,
                role = ChatMessage.Role.ASSISTANT,
                text = strings.greeting
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var nextId = 1L

    init {
        // Conversation-ID für den openHAB-5-Chat sicherstellen
        if (settings.value.conversationId.isBlank()) {
            settingsStore.save(settings.value.copy(conversationId = UUID.randomUUID().toString()))
        }
    }

    fun onMicClick() {
        if (isListening.value) {
            speechImpl.stop()
        } else {
            ttsImpl.stop() // Barge-in: laufende Antwort sofort stummschalten
            speechImpl.start(language = settings.value.sttLanguage) { text ->
                if (text.isNotBlank()) sendText(text)
            }
        }
    }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _isSending.value) return

        ttsImpl.stop()
        appendMessage(ChatMessage.Role.USER, clean)
        _isSending.value = true

        viewModelScope.launch {
            val answer = clientImpl.send(clean)
            _isSending.value = false
            answer.fold(
                onSuccess = { reply ->
                    appendMessage(ChatMessage.Role.ASSISTANT, reply)
                    val s = settings.value
                    if (s.ttsEnabled) ttsImpl.speak(stripMarkdown(reply), s.sttLanguage)
                },
                onFailure = { e ->
                    appendMessage(ChatMessage.Role.SYSTEM, "⚠️ ${e.message ?: strings.unknownError}")
                }
            )
        }
    }

    fun saveSettings(new: AppSettings) =
        settingsStore.save(new.copy(conversationId = new.conversationId.ifBlank { settings.value.conversationId }))

    fun clearChat() {
        viewModelScope.launch {
            clientImpl.deleteConversation() // serverseitigen Verlauf löschen (openHAB 5)
            settingsStore.save(settings.value.copy(conversationId = UUID.randomUUID().toString()))
        }
        _messages.value = listOf(
            ChatMessage(id = nextId++, role = ChatMessage.Role.ASSISTANT, text = strings.newConversation)
        )
    }

    private fun appendMessage(role: ChatMessage.Role, text: String) {
        _messages.value = _messages.value + ChatMessage(nextId++, role, text)
    }

    override fun onCleared() {
        speechImpl.release()
        ttsImpl.shutdown()
    }
}
