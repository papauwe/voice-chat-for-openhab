package de.openhabvoice.voice

/**
 * Maps the user's speech recognition language setting (BCP-47, e.g. "de-DE")
 * to the official small Vosk model for that language.
 *
 * Vosk models are per-language (no multilingual small models exist). If no
 * small model is available for a language, this returns null - in that case
 * the app shows a clear error instead of silently falling back to another
 * language.
 *
 * Model list: https://alphacephei.com/vosk/models (all Apache 2.0 licensed).
 */
object VoskModels {

    /** Base URL where the model zips are downloaded from. */
    const val MODELS_BASE_URL = "https://alphacephei.com/vosk/models"

    /** Small Vosk models per language code (language part of the BCP-47 tag). */
    private val SMALL_MODELS = mapOf(
        "de" to "vosk-model-small-de-0.15",        // ~45 MB
        "en" to "vosk-model-small-en-us-0.15",     // ~40 MB (US variant)
        "fr" to "vosk-model-small-fr-0.22",        // ~41 MB
        "es" to "vosk-model-small-es-0.42",        // ~38 MB
        "it" to "vosk-model-small-it-0.22",        // ~41 MB
        "nl" to "vosk-model-small-nl-0.22",
        "ru" to "vosk-model-small-ru-0.22",
        "pt" to "vosk-model-small-pt-0.3",
        "pl" to "vosk-model-small-pl-0.22",
        "cs" to "vosk-model-small-cs-0.6",
        "sv" to "vosk-model-small-sv-rhasspy-0.1",
        "da" to "vosk-model-small-da-0.5",
        "fi" to "vosk-model-small-fi-0.1",
        "tr" to "vosk-model-small-tr-0.3",
        "ca" to "vosk-model-small-ca-0.4",
        "eo" to "vosk-model-small-eo-0.42",
        "fa" to "vosk-model-small-fa-0.42",
        "hi" to "vosk-model-small-hi-0.22",
        "hu" to "vosk-model-small-hu-0.42",
        "ja" to "vosk-model-small-ja-0.22",
        "kz" to "vosk-model-small-kz-0.15",
        "ua" to "vosk-model-small-ua-0.22",
        "uz" to "vosk-model-small-uz-0.22",
        "vi" to "vosk-model-small-vi-0.3",
        "cn" to "vosk-model-small-cn-0.22",
    )

    /**
     * Returns the small Vosk model name for the given BCP-47 language tag
     * (e.g. "de-DE", "en-US"), or null if no small model exists for it.
     */
    fun smallModelFor(languageTag: String): String? {
        val language = languageTag.substringBefore('-').lowercase()
        return SMALL_MODELS[language]
    }

    /** Download URL for a model name. */
    fun downloadUrl(modelName: String): String = "$MODELS_BASE_URL/$modelName.zip"
}
