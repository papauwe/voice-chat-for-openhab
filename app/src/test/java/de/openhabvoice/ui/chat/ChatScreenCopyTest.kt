package de.openhabvoice.ui.chat

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import de.openhabvoice.ChatStrings
import de.openhabvoice.ChatViewModel
import de.openhabvoice.data.AppSettings
import de.openhabvoice.data.OpenHabApi
import de.openhabvoice.data.SettingsStore
import de.openhabvoice.ui.theme.OpenHabVoiceTheme
import de.openhabvoice.voice.SpeechEngine
import de.openhabvoice.voice.TtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI test of the copy feature: a long-press on a chat message must put the
 * raw text of the message on the clipboard (fix: texts of previous prompts
 * could not be copied before).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatScreenCopyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeSettingsStore : SettingsStore {
        private val flow = MutableStateFlow(AppSettings())
        override val settings: StateFlow<AppSettings> = flow.asStateFlow()
        override fun save(new: AppSettings) {
            flow.value = new
        }
    }

    private class FakeOpenHabApi : OpenHabApi {
        override suspend fun send(text: String): Result<String> = Result.success("ok")
        override suspend fun deleteConversation(): Result<Unit> = Result.success(Unit)
    }

    private class FakeTts : TtsEngine {
        override fun speak(text: String, languageTag: String) {}
        override fun stop() {}
        override fun shutdown() {}
    }

    private class FakeSpeech : SpeechEngine {
        private val _listening = MutableStateFlow(false)
        private val _partial = MutableStateFlow("")
        private val _error = MutableStateFlow<String?>(null)
        override val isListening: StateFlow<Boolean> = _listening.asStateFlow()
        override val partial: StateFlow<String> = _partial.asStateFlow()
        override val error: StateFlow<String?> = _error.asStateFlow()
        override fun start(language: String, onResult: (String) -> Unit) {}
        override fun stop() {}
        override fun release() {}
    }

    private fun createViewModel(): ChatViewModel {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        return ChatViewModel(
            app = app,
            settingsStoreOverride = FakeSettingsStore(),
            clientOverride = FakeOpenHabApi(),
            ttsOverride = FakeTts(),
            speechOverride = FakeSpeech(),
            chatStringsOverride = object : ChatStrings {
                override val greeting = "Test greeting"
                override val newConversation = "New conversation"
                override val unknownError = "Unknown error"
            }
        )
    }

    @Test
    fun `long-press on assistant message copies raw text to clipboard`() {
        val viewModel = createViewModel()
        composeRule.setContent {
            OpenHabVoiceTheme {
                ChatScreen(viewModel = viewModel, onOpenSettings = {})
            }
        }

        // The greeting message exists right after ViewModel creation (ASSISTANT bubble)
        composeRule.onNodeWithText("Test greeting", substring = true)
            .performTouchInput { longClick() }

        val clipboard = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertEquals("Test greeting", copied)
    }

    @Test
    fun `long-press on user message copies raw text to clipboard`() {
        val viewModel = createViewModel()
        val prompt = "Licht im Wohnzimmer einschalten"
        composeRule.setContent {
            OpenHabVoiceTheme {
                ChatScreen(viewModel = viewModel, onOpenSettings = {})
            }
        }

        // appendMessage for USER happens synchronously inside sendText -> bubble is there immediately
        viewModel.sendText(prompt)

        composeRule.onNodeWithText(prompt, substring = true)
            .performTouchInput { longClick() }

        val clipboard = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertEquals(prompt, copied)
    }
}
