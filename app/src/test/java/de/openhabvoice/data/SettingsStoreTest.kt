package de.openhabvoice.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-Tests fuer PrefsSettingsStore: Persistenz-Roundtrip ueber SharedPreferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearPrefs() {
        context.getSharedPreferences("openhab_voice_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `speichert und laedt alle Felder korrekt`() {
        PrefsSettingsStore(context).save(
            AppSettings(
                serverUrl = "  http://192.168.1.10:8080  ",
                chatMode = ChatMode.HABOT,
                customPath = "/rest/bridge",
                apiToken = "tok",
                username = "user",
                password = "pw",
                ttsEnabled = false,
                sttLanguage = "en-US",
                conversationId = "conv-1",
            )
        )
        val s = PrefsSettingsStore(context).settings.value
        assertEquals("http://192.168.1.10:8080", s.serverUrl)
        assertEquals(ChatMode.HABOT, s.chatMode)
        assertEquals("/rest/bridge", s.customPath)
        assertEquals("tok", s.apiToken)
        assertEquals("user", s.username)
        assertEquals("pw", s.password)
        assertEquals(false, s.ttsEnabled)
        assertEquals("en-US", s.sttLanguage)
        assertEquals("conv-1", s.conversationId)
    }

    @Test
    fun `leere Prefs liefern Defaults`() {
        val s = PrefsSettingsStore(context).settings.value
        assertEquals("", s.serverUrl)
        assertEquals(ChatMode.OPENHAB5_CHAT, s.chatMode)
        assertEquals(true, s.ttsEnabled)
        assertEquals("de-DE", s.sttLanguage)
        assertEquals("", s.conversationId)
    }

    @Test
    fun `ungueltiger ChatMode in Prefs faellt auf OPENHAB5_CHAT zurueck`() {
        context.getSharedPreferences("openhab_voice_prefs", Context.MODE_PRIVATE)
            .edit().putString("chat_mode", "GIBTS_NICHT").commit()
        val s = PrefsSettingsStore(context).settings.value
        assertEquals(ChatMode.OPENHAB5_CHAT, s.chatMode)
    }
}
