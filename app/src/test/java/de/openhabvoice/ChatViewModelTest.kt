package de.openhabvoice

import android.app.Application
import de.openhabvoice.data.AppSettings
import de.openhabvoice.data.ChatMessage
import de.openhabvoice.data.OpenHabApi
import de.openhabvoice.data.SettingsStore
import de.openhabvoice.voice.SpeechEngine
import de.openhabvoice.voice.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit-Tests fuer ChatViewModel mit Fakes (ohne Robolectric, ohne Android-Dienste).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: FakeSettingsStore
    private lateinit var client: FakeClient
    private lateinit var tts: FakeTts
    private lateinit var speech: FakeSpeech
    private lateinit var vm: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeSettingsStore()
        client = FakeClient()
        tts = FakeTts()
        speech = FakeSpeech()
        vm = ChatViewModel(Application(), store, client, tts, speech)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val userMsgs get() = vm.messages.value.filter { it.role == ChatMessage.Role.USER }
    private val lastMsg get() = vm.messages.value.last()

    // ---------- Initialisierung ----------

    @Test
    fun `init erzeugt Conversation-ID wenn leer`() {
        assertTrue(store.settings.value.conversationId.isNotBlank())
    }

    @Test
    fun `init behaelt vorhandene Conversation-ID`() {
        val store2 = FakeSettingsStore(AppSettings(conversationId = "fixed"))
        val vm2 = ChatViewModel(Application(), store2, client, tts, speech)
        assertEquals("fixed", vm2.settings.value.conversationId)
    }

    // ---------- sendText ----------

    @Test
    fun `leere Eingabe wird ignoriert`() = runTest {
        vm.sendText("   ")
        advanceUntilIdle()
        assertEquals(1, vm.messages.value.size)
        assertEquals(null, client.lastText)
    }

    @Test
    fun `erfolgreiche Antwort haengt USER- und ASSISTANT-Nachricht an und sendet TTS`() = runTest {
        client.result = Result.success("Antwort")
        vm.sendText("Frage")
        advanceUntilIdle()
        assertEquals(ChatMessage.Role.USER, vm.messages.value[1].role)
        assertEquals("Frage", vm.messages.value[1].text)
        assertEquals(ChatMessage.Role.ASSISTANT, vm.messages.value[2].role)
        assertEquals("Antwort", vm.messages.value[2].text)
        assertFalse(vm.isSending.value)
        assertEquals(listOf("Antwort"), tts.spoken)
    }

    @Test
    fun `Markdown in Antwort wird fuer TTS entfernt`() = runTest {
        client.result = Result.success("## Titel\n**wichtig** ✅")
        vm.sendText("Frage")
        advanceUntilIdle()
        assertEquals(listOf("Titel\nwichtig"), tts.spoken)
    }

    @Test
    fun `TTS ausgeschaltet - nichts gesprochen`() = runTest {
        store.save(store.settings.value.copy(ttsEnabled = false))
        client.result = Result.success("Antwort")
        vm.sendText("Frage")
        advanceUntilIdle()
        assertTrue(tts.spoken.isEmpty())
    }

    @Test
    fun `Fehler erzeugt SYSTEM-Nachricht ohne TTS`() = runTest {
        client.result = Result.failure(IOException("404 - Kein LLM-Interpreter"))
        vm.sendText("Frage")
        advanceUntilIdle()
        assertEquals(ChatMessage.Role.SYSTEM, lastMsg.role)
        assertTrue(lastMsg.text.startsWith("⚠️"))
        assertTrue(lastMsg.text.contains("404"))
        assertFalse(vm.isSending.value)
        assertTrue(tts.spoken.isEmpty())
    }

    @Test
    fun `doppeltes senden waehrend laufendem Request wird ignoriert`() = runTest {
        vm.sendText("erste")
        vm.sendText("zweite")
        advanceUntilIdle()
        assertEquals("erste", client.lastText)
        assertEquals(listOf("erste"), userMsgs.map { it.text })
    }

    // ---------- clearChat ----------

    @Test
    fun `clearChat leert Verlauf und setzt neue Conversation-ID`() = runTest {
        val oldId = store.settings.value.conversationId
        vm.sendText("Frage")
        advanceUntilIdle()
        vm.clearChat()
        advanceUntilIdle()
        assertTrue(client.deleted)
        assertTrue(store.settings.value.conversationId != oldId)
        assertEquals(1, vm.messages.value.size)
    }

    // ---------- saveSettings ----------

    @Test
    fun `saveSettings behaelt Conversation-ID bei leerer neuer ID`() {
        val id = store.settings.value.conversationId
        vm.saveSettings(AppSettings(serverUrl = "http://x:8080"))
        assertEquals("http://x:8080", store.settings.value.serverUrl)
        assertEquals(id, store.settings.value.conversationId)
    }

    // ---------- onMicClick ----------

    @Test
    fun `onMicClick startet Spracherkennung mit Spracheinstellung und stoppt TTS`() {
        vm.onMicClick()
        assertEquals("de-DE", speech.startLanguage)
        assertTrue(tts.stopped)
    }

    @Test
    fun `onMicClick stoppt laufende Erkennung`() {
        speech.isListening.value = true
        vm.onMicClick()
        assertTrue(speech.stopCount > 0)
    }

    @Test
    fun `Spracherkennungsergebnis loest sendText aus`() = runTest {
        vm.onMicClick()
        speech.onResultCallback!!.invoke("Sprachbefehl")
        advanceUntilIdle()
        assertEquals("Sprachbefehl", client.lastText)
        assertEquals(listOf("Sprachbefehl"), userMsgs.map { it.text })
    }

    // ---------- Fakes ----------

    private class FakeSettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
        private val _settings = MutableStateFlow(initial)
        override val settings: StateFlow<AppSettings> = _settings
        override fun save(new: AppSettings) {
            _settings.value = new
        }
    }

    private class FakeClient : OpenHabApi {
        var result: Result<String> = Result.success("Antwort")
        var lastText: String? = null
        var deleted = false
        override suspend fun send(text: String): Result<String> {
            lastText = text
            return result
        }
        override suspend fun deleteConversation(): Result<Unit> {
            deleted = true
            return Result.success(Unit)
        }
    }

    private class FakeTts : TtsEngine {
        val spoken = mutableListOf<String>()
        var stopped = false
        var shutdown = false
        override fun speak(text: String, languageTag: String) {
            spoken += text
        }
        override fun stop() {
            stopped = true
        }
        override fun shutdown() {
            shutdown = true
        }
    }

    private class FakeSpeech : SpeechEngine {
        override val isListening = MutableStateFlow(false)
        override val partial = MutableStateFlow("")
        override val error = MutableStateFlow<String?>(null)
        var startLanguage: String? = null
        var onResultCallback: ((String) -> Unit)? = null
        var stopCount = 0
        var released = false
        override fun start(language: String, onResult: (String) -> Unit) {
            startLanguage = language
            onResultCallback = onResult
        }
        override fun stop() {
            stopCount++
        }
        override fun release() {
            released = true
        }
    }
}
