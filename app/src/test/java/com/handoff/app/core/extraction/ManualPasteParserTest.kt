package com.handoff.app.core.extraction

import com.handoff.app.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualPasteParserTest {

    @Test
    fun `recognizes role prefixes`() {
        val text = """
            You: How do I boil an egg?

            Assistant: Bring water to a rolling boil, then cook for 7 minutes.
        """.trimIndent()
        val conversation = ManualPasteParser.parse(text)!!
        assertEquals(2, conversation.messages.size)
        assertEquals(Role.USER, conversation.messages[0].role)
        assertEquals("How do I boil an egg?", conversation.messages[0].plainText)
        assertEquals(Role.ASSISTANT, conversation.messages[1].role)
        assertEquals("Bring water to a rolling boil, then cook for 7 minutes.", conversation.messages[1].plainText)
    }

    @Test
    fun `platform names map to assistant`() {
        val conversation = ManualPasteParser.parse("ChatGPT: Sure!")!!
        assertEquals(Role.ASSISTANT, conversation.messages.single().role)
        assertEquals("Sure!", conversation.messages.single().plainText)
    }

    @Test
    fun `unlabeled paragraphs alternate starting with user`() {
        val conversation = ManualPasteParser.parse("First question.\n\nFirst answer.\n\nSecond question.")!!
        assertEquals(Role.USER, conversation.messages[0].role)
        assertEquals(Role.ASSISTANT, conversation.messages[1].role)
        assertEquals(Role.USER, conversation.messages[2].role)
    }

    @Test
    fun `same-role consecutive paragraphs merge`() {
        val conversation = ManualPasteParser.parse("You: line one\nmore of line one\n\nAssistant: reply")!!
        assertEquals(2, conversation.messages.size)
        assertEquals("line one\nmore of line one", conversation.messages[0].plainText)
    }

    @Test
    fun `blank input yields null`() {
        assertNull(ManualPasteParser.parse("   \n \n"))
    }
}
