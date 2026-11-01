package de.openhabvoice.robolectric

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.Locale

/**
 * Robolectric-Shadow fuer [TextToSpeech].
 *
 * Der Standard-Shadow von Robolectric simuliert die Initialisierung ueber einen
 * Service-Bind, dessen Callback (mit Delay) auf dem Main-Looper landet - waehrend
 * der echte Konstruktor des Frameworks synchron auf das Service-Bind wartet.
 * Auf der JVM laeuft dieser Bind nie -> der TextToSpeech-Konstruktor blockiert
 * bis zum (langen) Timeout und der Smoke-Test dauert Minuten statt Millisekunden.
 *
 * Diese Shadow schliesst die Initialisierung stattdessen sofort ab:
 *  - Konstruktor ruft den OnInitListener synchron mit SUCCESS auf
 *  - speak/stop/shutdown/setLanguage sind harmlose No-Ops
 *
 * Genutzt in de.openhabvoice.MainActivitySmokeTest via @Config(shadows = ...).
 */
@Implements(TextToSpeech::class)
class FastTextToSpeechShadow {

    @Implementation
    fun __constructor__(context: Context?, listener: TextToSpeech.OnInitListener) {
        // Initialisierung sofort "erfolgreich" melden - kein Service-Bind, kein Warten.
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
        // nichts zu tun
    }
}