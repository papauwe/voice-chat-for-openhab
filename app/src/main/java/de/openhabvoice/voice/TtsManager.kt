package de.openhabvoice.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Abstraktion der Sprachausgabe (testbar, ohne Android-TTS).
 */
interface TtsEngine {
    fun speak(text: String, languageTag: String)
    fun stop()
    fun shutdown()
}

/**
 * Android-TTS mit deutschen Standard-Einstellungen.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener, TtsEngine {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var language = "de-DE"

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) applyLanguage()
    }

    override fun speak(text: String, languageTag: String) {
        if (!ready) return
        language = languageTag
        applyLanguage()
        tts?.apply {
            stop()
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "openhab-voice-utterance")
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun applyLanguage() {
        val locale = runCatching { Locale.forLanguageTag(language) }.getOrDefault(Locale.GERMANY)
        tts?.language = locale
    }
}
