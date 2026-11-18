package de.openhabvoice.voice

import de.openhabvoice.data.SttEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the pure engine selection ([pickEngine]) in [SpeechRouter].
 *
 * Scenario: Amazon FireOS devices ship without a speech service, so
 * AUTO must fall back to the offline engine there.
 */
class SpeechRouterTest {

    @Test
    fun autoUsesSystemEngineWhenAvailable() {
        val engine = pickEngine(SttEngine.AUTO, systemAvailable = true)
        assertEquals(SttEngine.SYSTEM, engine)
    }

    @Test
    fun autoFallsBackToOfflineWhenSystemUnavailable() {
        // FireOS: no speech service installed -> offline Vosk must be used.
        val engine = pickEngine(SttEngine.AUTO, systemAvailable = false)
        assertEquals(SttEngine.OFFLINE, engine)
    }

    @Test
    fun systemForcesSystemEngineEvenWhenUnavailable() {
        val engine = pickEngine(SttEngine.SYSTEM, systemAvailable = false)
        assertEquals(SttEngine.SYSTEM, engine)
    }

    @Test
    fun offlineForcesOfflineEngineEvenWhenSystemAvailable() {
        val engine = pickEngine(SttEngine.OFFLINE, systemAvailable = true)
        assertEquals(SttEngine.OFFLINE, engine)
    }
}
