package de.openhabvoice.voice

import de.openhabvoice.data.SttEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Pure engine selection - extracted for JVM unit tests.
 *
 *  - OFFLINE: always the offline engine (Vosk), even if the system engine exists.
 *  - SYSTEM: always the system engine, even if no offline model is installed.
 *  - AUTO: system engine if available, otherwise the offline engine
 *    (this is what happens on FireOS devices without a speech service).
 */
fun pickEngine(preference: SttEngine, systemAvailable: Boolean): SttEngine = when (preference) {
    SttEngine.OFFLINE -> SttEngine.OFFLINE
    SttEngine.SYSTEM -> SttEngine.SYSTEM
    SttEngine.AUTO -> if (systemAvailable) SttEngine.SYSTEM else SttEngine.OFFLINE
}

/**
 * Routes speech recognition to the configured engine (system service or
 * offline Vosk). Re-evaluated on every start() so a settings change takes
 * effect immediately. The exposed flows always follow the active engine.
 */
class SpeechRouter(
    private val systemEngine: SpeechEngine,
    private val offlineEngine: SpeechEngine,
    private val preference: () -> SttEngine,
    private val systemAvailable: () -> Boolean,
    scope: CoroutineScope,
) : SpeechEngine {

    private val _active = MutableStateFlow(pickEngine(preference(), systemAvailable()))

    /** The active engine as a flow, so the exposed flows can follow it. */
    private val activeEngine: Flow<SpeechEngine> = _active.map { engineFor(it) }

    override val isListening: StateFlow<Boolean> = activeEngine
        .flatMapLatest { it.isListening }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override val partial: StateFlow<String> = activeEngine
        .flatMapLatest { it.partial }
        .stateIn(scope, SharingStarted.Eagerly, "")

    override val error: StateFlow<String?> = activeEngine
        .flatMapLatest { it.error }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val status: StateFlow<String?> = activeEngine
        .flatMapLatest { it.status }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override fun start(language: String, onResult: (String) -> Unit) {
        val engine = pickEngine(preference(), systemAvailable())
        _active.value = engine
        engineFor(engine).start(language, onResult)
    }

    override fun stop() = engineFor(_active.value).stop()

    override fun release() {
        systemEngine.release()
        offlineEngine.release()
    }

    private fun engineFor(engine: SttEngine): SpeechEngine =
        if (engine == SttEngine.OFFLINE) offlineEngine else systemEngine
}
