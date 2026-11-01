package de.openhabvoice.data

import android.content.Context
import de.openhabvoice.R

/**
 * User-visible texts produced by [OpenHabClient] (errors, fallback answers).
 *
 * [DefaultOpenHabTexts] is the English source of truth used by plain JVM unit
 * tests (no Robolectric, no resources); [ResourceOpenHabTexts] resolves the
 * localized strings from strings.xml and is used in production.
 */
interface OpenHabTexts {
    val noServerConfigured: String
    val commandSent: String
    val emptyResponse: String
    val hintNoLlmInterpreter: String
    val hintNoHabot: String
    val hintCheckAuth: String
}

object DefaultOpenHabTexts : OpenHabTexts {
    override val noServerConfigured =
        "No server URL configured. Please set it in the settings first."
    override val commandSent = "✅ Command sent to openHAB."
    override val emptyResponse = "(Empty response from server)"
    override val hintNoLlmInterpreter =
        " - Is an LLM interpreter installed? (Add-on e.g. \"openai\" HLI; OpenRouter uses the OpenAI API)"
    override val hintNoHabot =
        " - Is the HABot add-on installed on the server? Alternatively change the mode."
    override val hintCheckAuth = " - Check authentication (token/user)."
}

class ResourceOpenHabTexts(private val context: Context) : OpenHabTexts {
    override val noServerConfigured get() = context.getString(R.string.client_no_server)
    override val commandSent get() = context.getString(R.string.client_command_sent)
    override val emptyResponse get() = context.getString(R.string.client_empty_response)
    override val hintNoLlmInterpreter get() = context.getString(R.string.client_hint_llm)
    override val hintNoHabot get() = context.getString(R.string.client_hint_habot)
    override val hintCheckAuth get() = context.getString(R.string.client_hint_auth)
}
