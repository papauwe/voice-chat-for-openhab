package de.openhabvoice.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for OpenHabClient with MockWebServer - no real openHAB server needed.
 */
class OpenHabClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val base get() = "http://localhost:${server.port}"

    private fun settings(
        mode: ChatMode = ChatMode.OPENHAB5_CHAT,
        conversationId: String = "abc",
        apiToken: String = "",
        username: String = "",
        password: String = "",
        customPath: String = "",
        serverUrl: String = base,
    ) = AppSettings(
        serverUrl = serverUrl,
        chatMode = mode,
        conversationId = conversationId,
        apiToken = apiToken,
        username = username,
        password = password,
        customPath = customPath,
    )

    /** Dispatcher: 1st GET -> firstGet, further GETs -> laterGet, POST -> postBody (default: empty). */
    private fun conversationDispatcher(firstGet: String, laterGet: String, postBody: String = "") =
        object : Dispatcher() {
            val getCalls = AtomicInteger()
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "GET") {
                    val n = getCalls.incrementAndGet()
                    return MockResponse().setBody(if (n == 1) firstGet else laterGet)
                }
                return MockResponse().setBody(postBody)
            }
        }

    // ---------- openHAB 5 Chat ----------

    @Test
    fun `openhab5 with plain-text answer returns answer text`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.method == "GET")
                    MockResponse().setBody("""{"messages":[{"id":1,"role":"OPENHAB","content":"alt"}]}""")
                else MockResponse().setBody("Die Temperatur liegt bei 21 Grad.")
        }
        val client = OpenHabClient({
            AppSettings(serverUrl = base, chatMode = ChatMode.OPENHAB5_CHAT, conversationId = "abc")
        })
        val result = client.send("Wie ist die Temperatur?")
        assertEquals("Die Temperatur liegt bei 21 Grad.", result.getOrThrow())

        val get = server.takeRequest()
        assertEquals("/rest/voice/conversations/abc", get.path)
        val post = server.takeRequest()
        assertEquals("POST", post.method)
        assertTrue(post.path!!.startsWith("/rest/voice/interpreters"))
        assertEquals("abc", post.requestUrl!!.queryParameter("conversation"))
        assertEquals("Wie ist die Temperatur?", post.body.readUtf8())
        assertEquals("de", post.getHeader("Accept-Language"))
    }

    @Test
    fun `openhab5 with empty body fetches new answer from the history`() = runTest {
        server.dispatcher = conversationDispatcher(
            firstGet = """{"messages":[{"id":1,"role":"OPENHAB","content":"alt"}]}""",
            laterGet = """{"messages":[{"id":1,"role":"OPENHAB","content":"alt"},{"id":2,"role":"OPENHAB","content":"neu"}]}""",
        )
        val client = OpenHabClient({
            AppSettings(serverUrl = base, chatMode = ChatMode.OPENHAB5_CHAT, conversationId = "abc")
        })
        assertEquals("neu", client.send("Mach das Licht an").getOrThrow())
    }

    @Test
    fun `openhab5 empty answer with TOOL_RETURN uses tool return`() = runTest {
        server.dispatcher = conversationDispatcher(
            firstGet = """{"messages":[{"id":1,"role":"TOOL_RETURN","content":"erledigt"}]}""",
            laterGet = """{"messages":[{"id":1,"role":"TOOL_RETURN","content":"erledigt"},{"id":2,"role":"TOOL_RETURN","content":"okay"}]}""",
        )
        val client = OpenHabClient({
            AppSettings(serverUrl = base, chatMode = ChatMode.OPENHAB5_CHAT, conversationId = "abc")
        })
        assertEquals("okay", client.send("Licht an").getOrThrow())
    }

    @Test
    fun `openhab5 without new messages returns fallback text`() = runTest {
        server.dispatcher = conversationDispatcher(
            firstGet = """{"messages":[]}""",
            laterGet = """{"messages":[]}""",
        )
        val client = OpenHabClient({
            AppSettings(serverUrl = base, chatMode = ChatMode.OPENHAB5_CHAT, conversationId = "abc")
        })
        assertEquals("✅ Command sent to openHAB.", client.send("Licht an").getOrThrow())
    }

    // ---------- HABot ----------

    @Test
    fun `habot extracts answer field`() = runTest {
        server.enqueue(MockResponse().setBody("""{"answer":"Alles aus"}"""))
        val client = OpenHabClient({ settings(ChatMode.HABOT) })
        assertEquals("Alles aus", client.send("Mach alles aus").getOrThrow())
        val req = server.takeRequest()
        assertEquals("/rest/habot/chat", req.path)
        assertEquals("Mach alles aus", req.body.readUtf8())
    }

    @Test
    fun `habot extracts text field as fallback`() = runTest {
        server.enqueue(MockResponse().setBody("""{"text":"Hallo"}"""))
        val client = OpenHabClient({ settings(ChatMode.HABOT) })
        assertEquals("Hallo", client.send("hi").getOrThrow())
    }

    @Test
    fun `habot extracts message field as fallback`() = runTest {
        server.enqueue(MockResponse().setBody("""{"message":"Fertig"}"""))
        val client = OpenHabClient({ settings(ChatMode.HABOT) })
        assertEquals("Fertig", client.send("hi").getOrThrow())
    }

    @Test
    fun `habot non-JSON body is returned raw`() = runTest {
        server.enqueue(MockResponse().setBody("kein json"))
        val client = OpenHabClient({ settings(ChatMode.HABOT) })
        assertEquals("kein json", client.send("hi").getOrThrow())
    }

    // ---------- Voice Interpreter ----------

    @Test
    fun `voice mode URL-encodes command and returns fallback`() = runTest {
        server.enqueue(MockResponse().setBody(""))
        val client = OpenHabClient({ settings(ChatMode.VOICE) })
        assertEquals("✅ Command sent to openHAB.", client.send("Lampe Wohnzimmer an").getOrThrow())
        val req = server.takeRequest()
        assertEquals("/rest/voice/interpret?text=Lampe+Wohnzimmer+an", req.path)
        assertEquals("POST", req.method)
    }

    // ---------- Custom Endpoint ----------

    @Test
    fun `custom mode posts JSON to customPath`() = runTest {
        server.enqueue(MockResponse().setBody("""{"message":"Fertig"}"""))
        val client = OpenHabClient({ settings(ChatMode.CUSTOM, customPath = "/rest/mybridge") })
        assertEquals("Fertig", client.send("Status").getOrThrow())
        val req = server.takeRequest()
        assertEquals("/rest/mybridge", req.path)
        assertEquals("""{"message":"Status"}""", req.body.readUtf8())
    }

    // ---------- Auth ----------

    @Test
    fun `token sets Bearer and X-OpenHAB-Token headers`() = runTest {
        server.enqueue(MockResponse().setBody("ok"))
        val client = OpenHabClient({ settings(ChatMode.HABOT, apiToken = "tok123") })
        client.send("hi")
        val req = server.takeRequest()
        assertEquals("tok123", req.getHeader("X-OpenHAB-Token"))
        assertEquals("Bearer tok123", req.getHeader("Authorization"))
    }

    @Test
    fun `basic auth sets Authorization header`() = runTest {
        server.enqueue(MockResponse().setBody("ok"))
        val client = OpenHabClient({ settings(ChatMode.HABOT, username = "u", password = "p") })
        client.send("hi")
        val req = server.takeRequest()
        assertEquals("Basic dTpw", req.getHeader("Authorization"))
    }

    @Test
    fun `basic auth overrides token authorization`() = runTest {
        server.enqueue(MockResponse().setBody("ok"))
        val client = OpenHabClient({
            settings(ChatMode.HABOT, apiToken = "tok123", username = "u", password = "p")
        })
        client.send("hi")
        val req = server.takeRequest()
        assertEquals("tok123", req.getHeader("X-OpenHAB-Token"))
        assertEquals("Basic dTpw", req.getHeader("Authorization"))
    }

    // ---------- Error cases ----------

    @Test
    fun `missing server URL fails with hint`() = runTest {
        val client = OpenHabClient({ settings(ChatMode.HABOT, serverUrl = "") })
        val result = client.send("hi")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("No server URL configured"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `404 in openhab5 mode returns LLM hint`() = runTest {
        // 2x 404: the first GET (conversation) and the POST each need a response
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))
        val client = OpenHabClient({ settings(ChatMode.OPENHAB5_CHAT, conversationId = "abc") })
        val msg = client.send("hi").exceptionOrNull()!!.message!!
        assertTrue(msg.startsWith("404"))
        assertTrue(msg.contains("LLM interpreter installed"))
    }

    @Test
    fun `401 returns authentication hint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val client = OpenHabClient({ settings(ChatMode.HABOT) })
        val msg = client.send("hi").exceptionOrNull()!!.message!!
        assertTrue(msg.contains("401"))
        assertTrue(msg.contains("Check authentication"))
    }

    // ---------- deleteConversation ----------

    @Test
    fun `deleteConversation deletes in openhab5 mode via DELETE`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OpenHabClient({ settings(ChatMode.OPENHAB5_CHAT, conversationId = "abc") })
        client.deleteConversation()
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/rest/voice/conversations/abc", req.path)
    }

    @Test
    fun `deleteConversation is ignored in other modes`() = runTest {
        val client = OpenHabClient({ settings(ChatMode.HABOT, conversationId = "abc") })
        client.deleteConversation()
        assertEquals(0, server.requestCount)
    }
}
