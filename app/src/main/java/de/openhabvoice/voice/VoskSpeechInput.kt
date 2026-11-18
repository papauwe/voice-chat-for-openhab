package de.openhabvoice.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import de.openhabvoice.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException

/**
 * Offline speech recognition with Vosk (press-to-talk, on-device).
 *
 * Works on devices without a speech service (e.g. Amazon FireOS tablets).
 * The first use downloads the small Vosk model for the configured language
 * automatically (~40-50 MB, once); afterwards recognition runs fully offline.
 *
 * Flow (same semantics as SpeechInput):
 *  - start() begins recognition (with automatic model download if needed)
 *  - partial results land in [partial]
 *  - on stop() the final result is delivered via [onResult]
 *  - errors land in [error]; download progress lands in [status]
 */
class VoskSpeechInput(
    context: Context,
    private val modelStore: VoskModelStore = VoskModelStore(context),
) : SpeechEngine {

    private val appContext = context.applicationContext

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partial = MutableStateFlow("")
    override val partial: StateFlow<String> = _partial.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    override val status: StateFlow<String?> = _status.asStateFlow()

    private var onResult: ((String) -> Unit)? = null
    private var recognitionThread: Thread? = null

    @Volatile
    private var stopRequested = false
    private var released = false

    @SuppressLint("MissingPermission") // RECORD_AUDIO is requested by the UI before start()
    override fun start(language: String, onResult: (String) -> Unit) {
        stop()
        _error.value = null
        _partial.value = ""
        _status.value = null
        this.onResult = onResult

        if (released) {
            _error.value = appContext.getString(R.string.speech_error_client)
            return
        }

        val modelName = VoskModels.smallModelFor(language)
        if (modelName == null) {
            _error.value = appContext.getString(R.string.vosk_no_model_for_language, language)
            return
        }

        if (modelStore.installedDir(modelName) == null) {
            // First use: download the model in the background, then start recognition.
            _status.value = appContext.getString(R.string.vosk_downloading_started)
            Thread({
                try {
                    modelStore.download(modelName) { downloaded, total ->
                        if (total > 0) {
                            val percent = (downloaded * 100 / total).toInt()
                            _status.value = appContext.getString(R.string.vosk_downloading_progress, percent)
                        }
                    }
                    _status.value = null
                    runRecognition(modelName)
                } catch (e: IOException) {
                    Log.w(TAG, "Model download failed", e)
                    _status.value = null
                    _error.value = appContext.getString(R.string.vosk_download_failed, e.message ?: "")
                }
            }, "vosk-download").start()
            return
        }

        runRecognition(modelName)
    }

    /**
     * Loads the model and starts the audio loop on a background thread
     * (model loading and AudioRecord reads must not run on the main thread).
     */
    private fun runRecognition(modelName: String) {
        val modelDir = modelStore.installedDir(modelName) ?: run {
            _error.value = appContext.getString(R.string.vosk_download_failed, "")
            return
        }
        var model: Model? = null
        var recognizer: Recognizer? = null
        try {
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
        } catch (e: Exception) {
            Log.w(TAG, "Vosk init failed", e)
            recognizer?.closeQuietly()
            model?.close()
            _error.value = appContext.getString(R.string.speech_error_client)
            return
        }

        val audio = createAudioRecord()
        if (audio == null) {
            recognizer.closeQuietly()
            model.close()
            _error.value = appContext.getString(R.string.speech_error_audio)
            return
        }

        _isListening.value = true
        stopRequested = false
        val thread = Thread({
            try {
                audio.startRecording()
                // Vosk's acceptWaveForm takes short[]/float[] PCM data, so read
                // 16-bit PCM directly into a ShortArray.
                val buffer = ShortArray(BUFFER_SIZE)
                var lastFinal = ""
                while (!stopRequested) {
                    val read = audio.read(buffer, 0, BUFFER_SIZE)
                    if (read <= 0) continue
                    if (recognizer.acceptWaveForm(buffer, read)) {
                        // A chunk was recognized as final - keep the last non-empty one.
                        val text = resultText(recognizer.result)
                        if (text.isNotBlank()) lastFinal = text
                    } else {
                        val partial = resultText(recognizer.partialResult)
                        if (partial.isNotBlank()) _partial.value = partial
                    }
                }
                // Press-to-talk: flush the remaining audio and deliver the final result.
                val finalText = resultText(recognizer.finalResult).ifBlank { lastFinal }
                _isListening.value = false
                _partial.value = ""
                if (finalText.isNotBlank()) onResult?.invoke(finalText)
                else _error.value = appContext.getString(R.string.speech_no_match)
            } catch (e: Exception) {
                Log.w(TAG, "Vosk recognition failed", e)
                _isListening.value = false
                _error.value = appContext.getString(R.string.speech_error_client)
            } finally {
                try {
                    if (audio.state == AudioRecord.STATE_INITIALIZED) audio.stop()
                } catch (_: IllegalStateException) {
                }
                audio.release()
                recognizer.closeQuietly()
                model.close()
            }
        }, "vosk-recognition")
        recognitionThread = thread
        thread.start()
    }

    override fun stop() {
        stopRequested = true
        // Do not join here: stop() may be called from the main thread while the
        // recognition thread is flushing the final result.
        recognitionThread = null
    }

    override fun release() {
        stop()
        released = true
    }

    private fun createAudioRecord(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return null
        val audio = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, BUFFER_SIZE * 2)
        )
        if (audio.state != AudioRecord.STATE_INITIALIZED) {
            audio.release()
            return null
        }
        return audio
    }

    private fun resultText(json: String?): String = runCatching {
        JSONObject(json.orEmpty()).optString("text", "")
    }.getOrDefault("")

    private fun Recognizer.closeQuietly() {
        try {
            close()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16000
        const val BUFFER_SIZE = 4096
        const val TAG = "VoskSpeechInput"
    }
}
