package de.openhabvoice.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
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
 * Abstraction of the openHAB REST connection (testable without a real server).
 */
interface OpenHabApi {
    suspend fun send(text: String): Result<String>
    suspend fun deleteConversation(): Result<Unit>
}

/**
 * Minimal REST client for openHAB.
 *
 * Supports:
 *  - openHAB 5 chat:    POST /rest/voice/interpreters?conversation=...  (plain text in, plain text out)
 *  - HABot chat:        POST /rest/habot/chat        plain text in -> ChatReply JSON
 *  - Voice interpreter: POST /rest/voice/interpret?text=...
 *  - Custom endpoint:   POST <customPath>            {"message": ...}
 */
class OpenHabClient(
    private val settingsProvider: () -> AppSettings,
    textsOverride: OpenHabTexts? = null,
) : OpenHabApi {

    private val texts = textsOverride ?: DefaultOpenHabTexts
    // openHAB's Jetty closes idle keep-alive connections after ~30 s (default idleTimeout).
    // OkHttp's default pool keeps them for 5 minutes -> reusing such a connection throws
    // "unexpected end of stream". Keep the pool idle timeout BELOW the server's idle
    // timeout so OkHttp discards connections before the server closes them.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 20, TimeUnit.SECONDS))
        // Important: no silent retry for POST messages - otherwise the same
        // message could end up twice in the history on a dropped connection
        // (-> tools being executed twice on the server).
        .retryOnConnectionFailure(false)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val textType = "text/plain; charset=utf-8".toMediaType()

    override suspend fun send(text: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settingsProvider()
            require(s.hasServer) { texts.noServerConfigured }

            val base = s.serverUrl.trim().trimEnd('/')
            val conversationId = s.conversationId.ifBlank { UUID.randomUUID().toString() }

            // Before sending: remember the last known message ID (duplicate-answer fix)
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
                    // Empty body = e.g. a pure command without a verbal answer.
                    // Only read messages AFTER lastKnownId from the history -> no duplicated old answers.
                    fetchNewAnswer(base, s, conversationId, lastKnownId)
                } else {
                    extractAnswer(s.chatMode, body)
                }
            }
        }
    }

    /** Fetches the conversation (null if it does not exist yet or on error). */
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

    /** Highest message ID of the conversation (-1 if empty/missing). */
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
     * Fetches the new answer from the conversation history - but only messages
     * created AFTER our POST (id > lastKnownId).
     * Prefers the last OPENHAB message, otherwise the last TOOL_RETURN message,
     * otherwise the "command sent" hint.
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
            if (id <= lastKnownId) continue // old message -> skip
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

    /** Deletes a server-side openHAB 5 conversation history (ignores errors, e.g. in other modes). */
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
            // The voice interpreter usually does not return an answer text.
            return texts.commandSent
        }
        if (mode == ChatMode.OPENHAB5_CHAT) {
            // Response is plain text (no JSON)
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
