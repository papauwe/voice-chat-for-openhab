package de.openhabvoice

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import de.openhabvoice.robolectric.FastTextToSpeechShadow

/**
 * Smoke-Test des echten App-Starts: MainActivity inkl. Compose-Attach und
 * ViewModel-Erzeugung ueber die echte AndroidViewModelFactory (by viewModels()).
 *
 * Haette den Start-Crash (NoSuchMethodException ChatViewModel.<init>(Application))
 * sofort gemeldet, weil genau dieser Pfad - nicht der direkte Konstruktor-Aufruf
 * der Unit-Tests - die fehlende Ueberladung per Reflection sucht.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [FastTextToSpeechShadow::class])
class MainActivitySmokeTest {

    @Test
    fun `MainActivity startet ohne Crash bis RESUMED`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
