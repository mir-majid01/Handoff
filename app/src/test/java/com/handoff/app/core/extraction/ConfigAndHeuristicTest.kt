package com.handoff.app.core.extraction

import com.handoff.app.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigAndHeuristicTest {

    @Test
    fun `bundled extractors config parses and covers all platforms`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = ExtractorsConfigLoader.load(context)

        assertEquals(6, config.platforms.size)
        listOf("claude", "chatgpt", "gemini", "kimi", "deepseek", "qwen").forEach { key ->
            val platform = config.platforms[key]
            assertNotNull("Missing platform $key", platform)
            assertTrue(platform!!.urlPatterns.isNotEmpty())
            assertNotNull("Missing webview config for $key", platform.webview)
        }
    }

    @Test
    fun `webview extractor js embeds selectors and role rules`() {
        val extractor = WebViewExtractor()
        val config = WebViewConfig(
            readySelector = "main",
            messageSelector = "[data-message-author-role]",
            roleAttr = "data-message-author-role",
            roleByClass = listOf(ClassRole("font-user-message", "user")),
            textSelector = ".markdown",
        )
        val js = extractor.buildExtractorJs(config)
        assertTrue(js.contains("[data-message-author-role]"))
        assertTrue(js.contains("data-message-author-role"))
        assertTrue(js.contains(".markdown"))
        assertTrue(js.contains("font-user-message"))
        assertTrue(js.contains("JSON.stringify"))
        // Image-only turns must survive as placeholders, not vanish (Gemini generated images).
        assertTrue(js.contains("el.querySelector('img')"))
        // Strings are safely quoted for JS.
        assertTrue(extractor.jsString("a\"b").contains("\\\""))
    }

    @Test
    fun `parseResult unwraps the JSON string encoding evaluateJavascript sends`() {
        // evaluateJavascript hands a JS string back JSON-encoded: outer quotes + escapes.
        val inner = """{"title":"T","messages":[{"role":"user","blocks":[{"type":"text","text":"hi"}]}]}"""
        val wire = kotlinx.serialization.json.JsonPrimitive(inner).toString()
        val result = WebViewExtractor().parseResult(wire)
        assertNotNull(result)
        assertEquals("T", result!!.title)
        assertEquals(1, result.messages.size)
    }

    @Test
    fun `heuristic chunks visible text into alternating messages`() {
        val html = javaClass.getResource("/fixtures/generic.html")!!.readText()
        val conversation = GenericHeuristicExtractor().extract(html, "https://example.com/thread")
        assertNotNull(conversation)
        assertEquals(5, conversation!!.messages.size)
        assertEquals(Role.USER, conversation.messages.first().role)
        assertEquals(Role.ASSISTANT, conversation.messages[1].role)
        assertTrue(conversation.messages.first().plainText.contains("camping trip"))
    }
}
