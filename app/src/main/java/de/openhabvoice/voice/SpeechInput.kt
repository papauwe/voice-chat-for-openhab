package de.openhabvoice.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import de.openhabvoice.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstraction of speech recognition (testable without the Android recognizer).
 */
interface SpeechEngine {
    val isListening: StateFlow<Boolean>
    val partial: StateFlow<String>
    val error: StateFlow<String?>
    fun start(language: String, onResult: (String) -> Unit)
    fun stop()
    fun release()
}

/**
 * Wrapper around Android's SpeechRecognizer (press-to-talk).
 *
 * Flow:
 *  - start() begins recognition (de-DE or configurable)
 *  - partial results land in [partial]
 *  - on the final result [onResult] is called and recognition ends
 *  - errors land in [error]
 */
class SpeechInput(private val context: Context) : SpeechEngine {

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partial = MutableStateFlow("")
    override val partial: StateFlow<String> = _partial.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var onResult: ((String) -> Unit)? = null

    override fun start(language: String, onResult: (String) -> Unit) {
        release()
        _error.value = null
        _partial.value = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = context.getString(R.string.speech_not_available)
            return
        }

        this.onResult = onResult
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(intent)
        }
        _isListening.value = true
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun release() {
        recognizer?.destroy()
        recognizer = null
        _isListening.value = false
        _partial.value = ""
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _error.value = null }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            _isListening.value = false
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _error.value = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.speech_error_audio)
                    SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.speech_error_client)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.speech_error_permissions)
                    SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.speech_error_network)
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.speech_error_timeout)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.speech_error_busy)
                    SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.speech_error_server)
                    else -> context.getString(R.string.speech_error_unknown, error)
                }
            } else {
                _error.value = context.getString(R.string.speech_no_match)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _partial.value = ""
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onResult?.invoke(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (partial.isNotBlank()) _partial.value = partial
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
