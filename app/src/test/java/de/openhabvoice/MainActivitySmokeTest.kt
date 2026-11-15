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
 * Smoke test of the real app startup: MainActivity including Compose attach and
 * ViewModel creation via the real AndroidViewModelFactory (by viewModels()).
 *
 * Would have reported the startup crash (NoSuchMethodException
 * ChatViewModel.<init>(Application)) immediately, because exactly this path -
 * unlike the direct constructor calls in the unit tests - searches for the
 * missing overload via reflection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [FastTextToSpeechShadow::class])
class MainActivitySmokeTest {

    @Test
    fun `MainActivity starts without crash up to RESUMED`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
