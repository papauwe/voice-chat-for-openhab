package de.openhabvoice.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstraktion der Einstellungen (testbar, ohne Android-Context).
 */
interface SettingsStore {
    val settings: StateFlow<AppSettings>
    fun save(new: AppSettings)
}

/**
 * Sehr einfache, synchron gespeicherte Einstellungen (SharedPreferences).
 * Für die Minimalversion vollkommen ausreichend.
 */
class PrefsSettingsStore(context: Context) : SettingsStore {

    private val prefs = context.getSharedPreferences("openhab_voice_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun save(new: AppSettings) {
        prefs.edit()
            .putString(KEY_URL, new.serverUrl.trim())
            .putString(KEY_MODE, new.chatMode.name)
            .putString(KEY_CUSTOM_PATH, new.customPath)
            .putString(KEY_TOKEN, new.apiToken)
            .putString(KEY_USER, new.username)
            .putString(KEY_PASS, new.password)
            .putBoolean(KEY_TTS, new.ttsEnabled)
            .putString(KEY_LANG, new.sttLanguage)
            .putString(KEY_CONVERSATION, new.conversationId)
            .apply()
        _settings.value = new
    }

    private fun load(): AppSettings {
        val mode = try {
            ChatMode.valueOf(prefs.getString(KEY_MODE, ChatMode.OPENHAB5_CHAT.name)!!)
        } catch (_: IllegalArgumentException) {
            ChatMode.OPENHAB5_CHAT
        }
        return AppSettings(
            serverUrl = prefs.getString(KEY_URL, "") ?: "",
            chatMode = mode,
            customPath = prefs.getString(KEY_CUSTOM_PATH, "") ?: "",
            apiToken = prefs.getString(KEY_TOKEN, "") ?: "",
            username = prefs.getString(KEY_USER, "") ?: "",
            password = prefs.getString(KEY_PASS, "") ?: "",
            ttsEnabled = prefs.getBoolean(KEY_TTS, true),
            sttLanguage = prefs.getString(KEY_LANG, "de-DE") ?: "de-DE",
            conversationId = prefs.getString(KEY_CONVERSATION, "") ?: ""
        )
    }

    private companion object {
        const val KEY_URL = "server_url"
        const val KEY_MODE = "chat_mode"
        const val KEY_CUSTOM_PATH = "custom_path"
        const val KEY_TOKEN = "api_token"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
        const val KEY_TTS = "tts_enabled"
        const val KEY_LANG = "stt_language"
        const val KEY_CONVERSATION = "conversation_id"
    }
}
