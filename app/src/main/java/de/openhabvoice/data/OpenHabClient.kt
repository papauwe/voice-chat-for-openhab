package de.openhabvoice.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Abstraktion der openHAB-REST-Anbindung (testbar, ohne echten Server).
 */
interface OpenHabApi {
    suspend fun send(text: String): Result<String>
    suspend fun deleteConversation(): Result<Unit>
}

/**
 * Minimale REST-Anbindung an openHAB.
 *
 * Unterstuetzt:
 *  - openHAB 5 Chat:    POST /rest/voice/interpreters?conversation=...  (Klartext rein, Klartext raus)
 *  - HABot-Chat:        POST /rest/habot/chat        Klartext rein -> ChatReply-JSON
 *  - Voice Interpreter: POST /rest/voice/interpret?text=...
 *  - Custom Endpoint:   POST <customPath>            {"message": ...}
 */
class OpenHabClient(
    private val settingsProvider: () -> AppSettings,
    textsOverride: OpenHabTexts? = null,
) : OpenHabApi {

    private val texts = textsOverride ?: DefaultOpenHabTexts

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val textType = "text/plain; charset=utf-8".toMediaType()

    override suspend fun send(text: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settingsProvider()
            require(s.hasServer) { texts.noServerConfigured }

            val base = s.serverUrl.trim().trimEnd('/')
            val conversationId = s.conversationId.ifBlank { UUID.randomUUID().toString() }

            // Vor dem Senden: letzte bekannte Message-ID merken (fuer den Doppel-Antwort-Fix)
            val lastKnownId = if (s.chatMode == ChatMode.OPENHAB5_CHAT) {
                fetchLastMessageId(base, s, conversationId)
            } else -1

            val request = when (s.chatMode) {
                ChatMode.OPENHAB5_CHAT -> Request.Builder()
                    .url("$base/rest/voice/interpreters?conversation=$conversationId")
                    .header("Accept-Language", s.sttLanguage.substringBefore('-'))
                    .post(text.toRequestBody(textType))
                    .build()
                    .addAuth(s)
                ChatMode.HABOT -> Request.Builder()
                    .url("$base/rest/habot/chat")
                    .post(text.toRequestBody(textType))
                    .build()
                    .addAuth(s)
                ChatMode.VOICE -> Request.Builder()
                    .url("$base/rest/voice/interpret?text=${URLEncoder.encode(text, "UTF-8")}")
                    .post(ByteArray(0).toRequestBody(null))
                    .build()
                    .addAuth(s)
                ChatMode.CUSTOM -> jsonPost("$base${s.customPath}", text).addAuth(s)
            }

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("${response.code} - ${describeError(response.code, body, s.chatMode)}")
                }
                if (s.chatMode == ChatMode.OPENHAB5_CHAT && body.isBlank()) {
                    // Leerer Body = z. B. reiner Befehl ohne verbale Antwort.
                    // Nur NACHRICHTEN AB lastKnownId aus dem Verlauf lesen -> keine alten Antworten doppelt.
                    fetchNewAnswer(base, s, conversationId, lastKnownId)
                } else {
                    extractAnswer(s.chatMode, body)
                }
            }
        }
    }

    /** Ruft die Conversation ab (null, wenn noch nicht vorhanden / Fehler). */
    private fun fetchConversation(base: String, s: AppSettings, conversationId: String): JSONObject? {
        val request = Request.Builder()
            .url("$base/rest/voice/conversations/$conversationId")
            .get()
            .build()
            .addAuth(s)
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) null
            else try { JSONObject(body) } catch (_: Exception) { null }
        }
    }

    /** Hoechste Message-ID der Conversation (-1, wenn leer/nicht vorhanden). */
    private fun fetchLastMessageId(base: String, s: AppSettings, conversationId: String): Int {
        val messages = fetchConversation(base, s, conversationId)?.optJSONArray("messages")
            ?: return -1
        var max = -1
        for (i in 0 until messages.length()) {
            val id = messages.optJSONObject(i)?.optInt("id", -1) ?: -1
            if (id > max) max = id
        }
        return max
    }

    /**
     * Holt die neue Antwort aus dem Verlauf – aber ausschliesslich Nachrichten,
     * die NACH unserem POST entstanden sind (id > lastKnownId).
     * Bevorzugt die letzte OPENHAB-Nachricht, sonst die letzte TOOL_RETURN-Nachricht,
     * sonst Hinweis "Befehl ausgefuehrt".
     */
    private fun fetchNewAnswer(
        base: String,
        s: AppSettings,
        conversationId: String,
        lastKnownId: Int
    ): String {
        val messages = fetchConversation(base, s, conversationId)?.optJSONArray("messages")
            ?: return texts.commandSent

        var lastAssistant = ""
        var lastToolReturn = ""
        for (i in 0 until messages.length()) {
            val m = messages.optJSONObject(i) ?: continue
            val id = m.optInt("id", -1)
            if (id <= lastKnownId) continue // alte Nachricht -> ueberspringen
            val role = m.optString("role")
            val content = m.optString("content")
            when {
                role == "OPENHAB" && content.isNotBlank() -> lastAssistant = content
                role == "TOOL_RETURN" && content.isNotBlank() -> lastToolReturn = content
            }
        }
        return when {
            lastAssistant.isNotBlank() -> lastAssistant
            lastToolReturn.isNotBlank() -> lastToolReturn
            else -> texts.commandSent
        }
    }

    /** Loescht einen serverseitigen openHAB-5-Verlauf (ignoriert Fehler, z. B. bei anderen Modi). */
    override suspend fun deleteConversation(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settingsProvider()
            if (s.chatMode != ChatMode.OPENHAB5_CHAT || s.conversationId.isBlank()) return@runCatching
            val base = s.serverUrl.trim().trimEnd('/')
            val request = Request.Builder()
                .url("$base/rest/voice/conversations/${s.conversationId}")
                .delete()
                .build()
                .addAuth(s)
            client.newCall(request).execute().use { }
        }
    }

    private fun jsonPost(url: String, message: String): Request =
        Request.Builder()
            .url(url)
            .post(JSONObject().put("message", message).toString().toRequestBody(jsonType))
            .build()

    private fun Request.addAuth(s: AppSettings): Request {
        val builder = newBuilder()
        if (s.apiToken.isNotBlank()) {
            builder.header("X-OpenHAB-Token", s.apiToken)
            builder.header("Authorization", "Bearer ${s.apiToken}")
        }
        if (s.username.isNotBlank()) {
            val credentials = okhttp3.Credentials.basic(s.username, s.password)
            builder.header("Authorization", credentials)
        }
        return builder.build()
    }

    private fun extractAnswer(mode: ChatMode, body: String): String {
        if (mode == ChatMode.VOICE) {
            // Der Voice-Interpreter liefert i. d. R. keinen Antworttext.
            return texts.commandSent
        }
        if (mode == ChatMode.OPENHAB5_CHAT) {
            // Response ist Klartext (kein JSON)
            return body.trim()
        }
        if (body.isBlank()) return texts.emptyResponse

        return try {
            val obj = JSONObject(body)
            sequenceOf("answer", "text", "message", "response", "result")
                .map { obj.optString(it) }
                .firstOrNull { it.isNotBlank() }
                ?: body.take(600)
        } catch (_: Exception) {
            body.take(600)
        }
    }

    private fun describeError(code: Int, body: String, mode: ChatMode): String {
        val hint = when {
            code == 404 && mode == ChatMode.OPENHAB5_CHAT -> texts.hintNoLlmInterpreter
            code == 404 && mode == ChatMode.HABOT -> texts.hintNoHabot
            code == 401 || code == 403 -> texts.hintCheckAuth
            else -> ""
        }
        val detail = body.take(200)
        return if (detail.isBlank()) hint.trimStart(' ', '-') else "$hint $detail".trim()
    }
}
