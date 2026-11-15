package de.openhabvoice.robolectric

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.Locale

/**
 * Robolectric shadow for [TextToSpeech].
 *
 * Robolectric's default shadow simulates initialization via a service bind whose
 * (delayed) callback is posted to the main looper - while the framework's real
 * constructor waits synchronously for that bind.
 * On the JVM this bind never happens -> the TextToSpeech constructor blocks
 * until the (long) timeout and the smoke test takes minutes instead of milliseconds.
 *
 * This shadow completes initialization immediately instead:
 *  - the constructor calls the OnInitListener synchronously with SUCCESS
 *  - speak/stop/shutdown/setLanguage are harmless no-ops
 *
 * Used in de.openhabvoice.MainActivitySmokeTest via @Config(shadows = ...).
 */
@Implements(TextToSpeech::class)
class FastTextToSpeechShadow {

    @Implementation
    fun __constructor__(context: Context?, listener: TextToSpeech.OnInitListener) {
        // Report initialization as "successful" immediately - no service bind, no waiting.
        listener.onInit(TextToSpeech.SUCCESS)
    }

    @Implementation
    fun setLanguage(loc: Locale): Int = TextToSpeech.LANG_AVAILABLE

    @Implementation
    fun isLanguageAvailable(loc: Locale): Int = TextToSpeech.LANG_AVAILABLE

    @Implementation
    fun speak(text: CharSequence, queueMode: Int, params: Bundle?, utteranceId: String?): Int =
        TextToSpeech.SUCCESS

    @Implementation
    fun stop(): Int = TextToSpeech.SUCCESS

    @Implementation
    fun shutdown() {
        // nothing to do
    }
}