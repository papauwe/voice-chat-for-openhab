package de.openhabvoice

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test for ViewModel creation: MainActivity creates the ViewModel
 * via `by viewModels()` -> AndroidViewModelFactory searches via reflection for a
 * constructor taking exactly (Application). Kotlin default parameters do NOT
 * generate such an overload -> without an explicit secondary constructor the
 * app crashes on startup with NoSuchMethodException (see crash from 31.10.).
 *
 * The unit tests with constructor injection would NOT find this bug because
 * they call the 5-parameter constructor directly.
 */
class ChatViewModelFactoryContractTest {

    @Test
    fun `ChatViewModel has a constructor taking only Application`() {
        val ctors = ChatViewModel::class.java.constructors
        val appCtors = ctors.filter {
            it.parameterTypes.size == 1 && it.parameterTypes[0] == Application::class.java
        }
        assertTrue(
            "ChatViewModel needs an explicit (Application) constructor " +
                "for the AndroidViewModelFactory. Existing signatures: " +
                ctors.joinToString(" | ") { c ->
                    c.parameterTypes.joinToString(",") { p -> p.simpleName }
                },
            appCtors.isNotEmpty()
        )
    }

    @Test
    fun `the Application constructor is the only entry point besides the full constructor`() {
        // For default parameters in the primary constructor Kotlin additionally
        // generates a synthetic constructor with (int, DefaultConstructorMarker) -
        // irrelevant for the AndroidViewModelFactory and filtered out here.
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
                    ChatStrings::class.java,
                ),
            ),
            ctors.toSet()
        )
    }
}
