package com.handoff.app.core.builder

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val conversation = Conversation(
        platform = Platform.CLAUDE,
        title = "HTML parsing in Kotlin",
        url = "https://claude.ai/share/abc-123",
        messages = listOf(
            Message(
                role = Role.USER,
                blocks = listOf(
                    Block.Text("How do I parse HTML in Kotlin? I need to extract links from a page."),
                ),
            ),
            Message(
                role = Role.ASSISTANT,
                blocks = listOf(
                    Block.Text("HTML parsing is straightforward with Jsoup. Let's use Jsoup for parsing."),
                    Block.Code(
                        language = "kotlin",
                        content = "val doc = Jsoup.connect(url).get()\nval links = doc.select(\"a[href]\")",
                    ),
                ),
            ),
            Message(
                role = Role.USER,
                blocks = listOf(Block.Text("What about pages that need JavaScript? Does that work offline?")),
            ),
            Message(
                role = Role.ASSISTANT,
                blocks = listOf(
                    Block.Text("Use a headless browser for that; Jsoup alone only sees static HTML."),
                    Block.Code(language = "kotlin", content = "val body = renderWithWebView(url)"),
                ),
            ),
            Message(
                role = Role.USER,
                blocks = listOf(Block.Text("One more thing — should I cache the rendered pages?")),
            ),
        ),
    )

    @Test
    fun `full transcript wraps every message in order`() {
        val prompt = PromptBuilder.build(conversation, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC)
        assertEquals(1, prompt.partCount)
        val text = prompt.parts.first()

        assertTrue(text.contains("<context source=\"Claude\" title=\"HTML parsing in Kotlin\" messages=\"5\">"))
        assertTrue(text.contains("**User**"))
        assertTrue(text.contains("**Assistant**"))
        assertTrue(text.contains("```kotlin"))
        assertTrue(text.contains("val links = doc.select(\"a[href]\")"))
        assertTrue(text.contains("My next message: [I will type it here]"))
        assertTrue(text.indexOf("How do I parse HTML") < text.indexOf("headless browser"))
    }

    @Test
    fun `excluded messages are dropped`() {
        val withExcluded = conversation.copy(
            messages = conversation.messages.mapIndexed { i, m -> m.copy(excluded = i == 0) },
        )
        val prompt = PromptBuilder.build(withExcluded, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC)
        val text = prompt.parts.first()
        assertFalse(text.contains("How do I parse HTML in Kotlin?"))
        assertTrue(text.contains("messages=\"4\""))
    }

    @Test
    fun `all messages excluded yields instructive prompt instead of crash`() {
        val allExcluded = conversation.copy(
            messages = conversation.messages.map { it.copy(excluded = true) },
        )
        val prompt = PromptBuilder.build(allExcluded, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC)
        assertTrue(prompt.parts.first().contains("No messages are included"))
        assertTrue(prompt.parts.first().contains("messages=\"0\""))
    }

    @Test
    fun `smart brief contains goal decisions code and last exchanges`() {
        val prompt = PromptBuilder.build(conversation, PromptMode.SMART_BRIEF, TargetAi.GENERIC)
        val text = prompt.parts.first()

        assertTrue(text.contains("**Goal**"))
        assertTrue(text.contains("extract links from a page"))
        assertTrue(text.contains("**Key decisions**"))
        assertTrue(text.contains("Let's use Jsoup for parsing."))
        assertTrue(text.contains("**Final code / artifacts**"))
        assertTrue(text.contains("renderWithWebView(url)"))
        assertTrue(text.contains("**Open questions**"))
        assertTrue(text.contains("Does that work offline?"))
        assertTrue(text.contains("**Last exchanges (verbatim)**"))
        assertFalse(text.contains("**System**"))
    }

    @Test
    fun `code and decisions mode omits narrative text`() {
        val prompt = PromptBuilder.build(conversation, PromptMode.CODE_AND_DECISIONS, TargetAi.GENERIC)
        val text = prompt.parts.first()

        assertTrue(text.contains("- Let's use Jsoup for parsing."))
        assertTrue(text.contains("Jsoup.connect(url).get()"))
        assertTrue(text.contains("From Assistant, message 2"))
        assertFalse(text.contains("HTML parsing is straightforward with Jsoup."))
        assertFalse(text.contains("**Last exchanges"))
    }

    @Test
    fun `targets change the wrapper but keep the frame`() {
        val base = PromptBuilder.build(conversation, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC).parts.first()
        val claude = PromptBuilder.build(conversation, PromptMode.FULL_TRANSCRIPT, TargetAi.CLAUDE).parts.first()
        val chatgpt = PromptBuilder.build(conversation, PromptMode.FULL_TRANSCRIPT, TargetAi.CHATGPT).parts.first()
        val gemini = PromptBuilder.build(conversation, PromptMode.FULL_TRANSCRIPT, TargetAi.GEMINI).parts.first()

        listOf(claude, chatgpt, gemini).forEach {
            assertTrue(it.contains("<context source=\"Claude\""))
            assertTrue(it.contains("</context>"))
        }
        assertTrue(claude.contains("Markdown is"))
        assertTrue(chatgpt.contains("matching the"))
        assertTrue(gemini.contains("answer in"))
        assertFalse(base.contains("Markdown is"))
    }

    @Test
    fun `oversized conversation auto-splits into labeled parts`() {
        val filler = Message(
            role = Role.USER,
            blocks = listOf(Block.Text("Please continue the analysis. ".repeat(400))),
        )
        val huge = conversation.copy(messages = List(40) { filler })
        val prompt = PromptBuilder.build(huge, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC, TokenLimit.K8)

        assertTrue(prompt.wasSplit)
        assertTrue(prompt.partCount > 1)
        assertTrue(prompt.parts.first().contains("[Handoff — Part 1 of ${prompt.partCount}"))
        assertTrue(prompt.parts.last().contains("until you have received Part ${prompt.partCount}"))
        // Every part stays under the limit it was built for.
        prompt.parts.forEach { assertTrue(TokenEstimator.estimateTokens(it) <= TokenLimit.K8.tokens!!) }
        // Content survives the split: every filler unit made it in.
        val joined = prompt.parts.joinToString("\n")
        assertEquals(40, Regex("Please continue the analysis\\.").findAll(joined).count() / 400)
    }

    @Test
    fun `no split when unlimited`() {
        val filler = Message(
            role = Role.USER,
            blocks = listOf(Block.Text("Filler text. ".repeat(5_000))),
        )
        val huge = conversation.copy(messages = List(10) { filler })
        val prompt = PromptBuilder.build(huge, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC, TokenLimit.UNLIMITED)
        assertEquals(1, prompt.partCount)
        assertFalse(prompt.wasSplit)
    }

    @Test
    fun `title is sanitized in wrapper`() {
        val dirty = conversation.copy(title = "Weird \"title\" <here>\nsecond line")
        val prompt = PromptBuilder.build(dirty, PromptMode.FULL_TRANSCRIPT, TargetAi.GENERIC)
        assertTrue(prompt.parts.first().contains("title=\"Weird title here second line\""))
    }
}
