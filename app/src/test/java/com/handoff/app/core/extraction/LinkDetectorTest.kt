package com.handoff.app.core.extraction

import com.handoff.app.core.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkDetectorTest {

    private val config = ExtractorsConfig(
        platforms = mapOf(
            "claude" to PlatformConfig(
                displayName = "Claude",
                urlPatterns = listOf("^https?://(www\\.)?claude\\.ai/share/"),
            ),
            "chatgpt" to PlatformConfig(
                displayName = "ChatGPT",
                urlPatterns = listOf(
                    "^https?://(www\\.)?chatgpt\\.com/share/",
                    "^https?://(www\\.)?chat\\.openai\\.com/share/",
                ),
            ),
            "gemini" to PlatformConfig(
                displayName = "Gemini",
                urlPatterns = listOf(
                    "^https?://(www\\.)?gemini\\.google\\.com/share/",
                    "^https?://(www\\.)?bard\\.google\\.com/share/",
                    "^https?://share\\.gemini\\.google/",
                ),
            ),
            "kimi" to PlatformConfig(
                displayName = "Kimi",
                urlPatterns = listOf("^https?://(www\\.)?kimi\\.ai/share/"),
            ),
            "deepseek" to PlatformConfig(
                displayName = "DeepSeek",
                urlPatterns = listOf("^https?://chat\\.deepseek\\.com/share/"),
            ),
            "qwen" to PlatformConfig(
                displayName = "Qwen",
                urlPatterns = listOf("^https?://chat\\.qwen\\.ai/s/"),
            ),
        ),
    )

    private fun detect(url: String): Platform? =
        (LinkDetector.detect(url, config) as? LinkDetector.Result.Detected)?.platform

    @Test
    fun `detects all three platforms`() {
        assertEquals(Platform.CLAUDE, detect("https://claude.ai/share/0123abcd-1234"))
        assertEquals(Platform.CHATGPT, detect("https://chatgpt.com/share/s-abc123"))
        assertEquals(Platform.CHATGPT, detect("https://chat.openai.com/share/s-abc123"))
        assertEquals(Platform.GEMINI, detect("https://gemini.google.com/share/1a2b3c"))
        assertEquals(Platform.GEMINI, detect("https://bard.google.com/share/old"))
    }

    @Test
    fun `tolerates messy input`() {
        assertEquals(Platform.CLAUDE, detect("  claude.ai/share/xyz \n"))
        assertEquals(Platform.CLAUDE, detect("https://www.claude.ai/share/xyz"))
        assertEquals(Platform.CLAUDE, detect("\"https://claude.ai/share/xyz\""))
    }

    @Test
    fun `detects the newer share hosts`() {
        assertEquals(Platform.KIMI, detect("https://www.kimi.ai/share/1a0faea5-a6a2"))
        assertEquals(Platform.DEEPSEEK, detect("https://chat.deepseek.com/share/7yaot74j1s3w"))
        assertEquals(Platform.QWEN, detect("https://chat.qwen.ai/s/t_5727e9e4-2516"))
        assertEquals(Platform.GEMINI, detect("https://share.gemini.google/aObdneLO47Ra"))
    }

    @Test
    fun `picks the first url out of a noisy multi-link paste`() {
        val paste = "[9:41 PM, 10/1/2026] haseeb: https://chat.qwen.ai/s/t_5727e9e4 f0 " +
            "https://chat.deepseek.com/share/7yaot74j1s3w"
        assertEquals(Platform.QWEN, detect(paste))
        assertEquals("https://chat.qwen.ai/s/t_5727e9e4", LinkDetector.normalize(paste))
    }

    @Test
    fun `rejects non share links`() {
        assertEquals(null, detect("https://example.com/page"))
        assertEquals(null, detect("https://claude.ai/chat/abc"))
        assertEquals(null, detect("not a url at all"))
        assertTrue(LinkDetector.detect("", config) is LinkDetector.Result.Unknown)
    }

    @Test
    fun `strips tracking params but keeps ids in the path`() {
        assertEquals(
            "https://chatgpt.com/share/s-abc123",
            LinkDetector.normalize("https://chatgpt.com/share/s-abc123?og=9281&sn=88bd&utm_source=x"),
        )
        assertEquals(
            "https://claude.ai/share/xyz#part-2",
            LinkDetector.stripTrackingParams("https://claude.ai/share/xyz?ref=tw#part-2"),
        )
        assertEquals(
            "https://chat.qwen.ai/s/t_1?mode=chat",
            LinkDetector.stripTrackingParams("https://chat.qwen.ai/s/t_1?mode=chat&og=5"),
        )
    }

    @Test
    fun `detects platforms behind tracking params`() {
        assertEquals(Platform.CHATGPT, detect("https://chatgpt.com/share/s-abc?ogimg=plain"))
    }
}
