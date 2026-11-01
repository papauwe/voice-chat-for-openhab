package de.openhabvoice

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vertrags-Test fuer die ViewModel-Erzeugung: MainActivity erzeugt den ViewModel
 * via `by viewModels()` -> AndroidViewModelFactory sucht per Reflection nach einem
 * Konstruktor, der genau (Application) nimmt. Kotlin-Default-Parameter erzeugen
 * KEINE solche Ueberladung -> ohne expliziten Sekundaerkonstruktor crasht der
 * App-Start mit NoSuchMethodException (siehe Crash vom 31.10.).
 *
 * Die Unit-Tests mit Constructor-Injection wuerden diesen Fehler NICHT finden,
 * weil sie den 5-Parameter-Konstruktor direkt aufrufen.
 */
class ChatViewModelFactoryContractTest {

    @Test
    fun `ChatViewModel hat einen Konstruktor, der nur Application nimmt`() {
        val ctors = ChatViewModel::class.java.constructors
        val appCtors = ctors.filter {
            it.parameterTypes.size == 1 && it.parameterTypes[0] == Application::class.java
        }
        assertTrue(
            "ChatViewModel braucht einen expliziten Konstruktor (Application) " +
                "fuer die AndroidViewModelFactory. Vorhandene Signaturen: " +
                ctors.joinToString(" | ") { c ->
                    c.parameterTypes.joinToString(",") { p -> p.simpleName }
                },
            appCtors.isNotEmpty()
        )
    }

    @Test
    fun `der Application-Konstruktor ist der einzige Einstiegspunkt neben dem Full-Konstruktor`() {
        // Kotlin erzeugt fuer Default-Parameter im Hauptkonstruktor zusaetzlich einen
        // synthetischen Konstruktor mit (int, DefaultConstructorMarker) - der ist
        // fuer die AndroidViewModelFactory irrelevant und wird hier gefiltert.
        val ctors = ChatViewModel::class.java.constructors
            .filterNot { c ->
                val last = c.parameterTypes.lastOrNull()
                last == Int::class.javaPrimitiveType ||
                    c.parameterTypes.any { it == kotlin.jvm.internal.DefaultConstructorMarker::class.java }
            }
            .map { it.parameterTypes.toList() }
        assertEquals(
            setOf(
                listOf(Application::class.java),
                listOf(
                    Application::class.java,
                    de.openhabvoice.data.SettingsStore::class.java,
                    de.openhabvoice.data.OpenHabApi::class.java,
                    de.openhabvoice.voice.TtsEngine::class.java,
                    de.openhabvoice.voice.SpeechEngine::class.java,
                ),
            ),
            ctors.toSet()
        )
    }
}
