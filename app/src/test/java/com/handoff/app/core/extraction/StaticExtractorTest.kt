package com.handoff.app.core.extraction

import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StaticExtractorTest {

    private lateinit var server: MockWebServer
    private val extractor = StaticExtractor()

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    private fun fixture(name: String): String =
        javaClass.getResource("/fixtures/$name")!!.readText()

    @Test
    fun `parses claude fixture with NEXT_DATA script`() {
        val config = ExtractorsConfig(
            platforms = mapOf(
                "claude" to PlatformConfig(
                    displayName = "Claude",
                    static = StaticConfig(
                        scripts = listOf(
                            ScriptCandidate(id = "__NEXT_DATA__", parser = "deep_search"),
                        ),
                    ),
                ),
            ),
        )
        val conversation = extractor.parseHtml(
            fixture("claude_share.html"),
            Platform.CLAUDE,
            config.platforms.getValue("claude").static!!,
        )
        assertNotNull(conversation)
        assertEquals(4, conversation!!.messages.size)
        assertEquals(Role.USER, conversation.messages.first().role)
        assertEquals("Sourdough starter help – Claude", conversation.title)
    }

    @Test
    fun `recovers conversation from streamed next_f payload`() {
        val config = StaticConfig(scripts = emptyList(), tryStreamedRsc = true)
        val conversation = extractor.parseHtml(fixture("chatgpt_nextf.html"), Platform.CHATGPT, config)
        assertNotNull(conversation)
        assertEquals(2, conversation!!.messages.size)
        assertEquals("Name three sorting algorithms.", conversation.messages.first().plainText)
        assertEquals("Quicksort, mergesort, and heapsort.", conversation.messages.last().plainText)
    }

    @Test
    fun `page without conversation parses to null`() {
        val config = StaticConfig(scripts = emptyList(), tryStreamedRsc = true)
        assertNull(extractor.parseHtml("<html><body><p>Hello world page</p></body></html>", Platform.GENERIC, config))
    }

    @Test
    fun `fetch maps http codes to typed errors`() = runBlocking {
        fun url() = server.url("/share/abc").toString()

        server.enqueue(MockResponse().setResponseCode(404))
        val notFound = runCatching { extractor.fetchHtml(url(), StaticConfig(retries = 1)) }
        assertTrue(notFound.exceptionOrNull() is ExtractionError.NotFound)

        server.enqueue(MockResponse().setResponseCode(403))
        val forbidden = runCatching { extractor.fetchHtml(url(), StaticConfig(retries = 1)) }
        assertTrue(forbidden.exceptionOrNull() is ExtractionError.Private)

        server.enqueue(MockResponse().setResponseCode(500))
        val serverError = runCatching { extractor.fetchHtml(url(), StaticConfig(retries = 1)) }
        assertTrue(serverError.exceptionOrNull() is ExtractionError.NetworkError)

        server.enqueue(MockResponse().setBody(fixture("claude_share.html")))
        val body = extractor.fetchHtml(url(), StaticConfig(retries = 1))
        assertTrue(body.contains("__NEXT_DATA__"))
    }

    @Test
    fun `retries on transient failure then succeeds`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setBody("ok-body"))
        val body = extractor.fetchHtml(server.url("/share/x").toString(), StaticConfig(retries = 2))
        assertEquals("ok-body", body)
        assertEquals(2, server.requestCount)
    }
}
