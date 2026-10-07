package com.handoff.app.core.export

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutTest {

    @Test
    fun `layout has title meta role labels and code groups`() {
        val conversation = Conversation(
            platform = Platform.CHATGPT,
            title = "Pagination check",
            url = "",
            messages = listOf(
                Message(Role.USER, listOf(Block.Text("Show me the layout plan"))),
                Message(
                    Role.ASSISTANT,
                    listOf(
                        Block.Text("Here is a plan."),
                        Block.Code("kotlin", "fun a() = 1\nfun b() = 2"),
                        Block.ListBlock(ordered = false, items = listOf("one", "two")),
                    ),
                ),
            ),
        )
        val lines = PdfLayout.buildLines(conversation, conversation.messages)

        assertTrue(lines.first().text.contains("Pagination check"))
        assertTrue(lines.any { it.kind == PdfLayout.LineKind.ROLE && it.text.startsWith("User") })
        assertTrue(lines.any { it.kind == PdfLayout.LineKind.CODE })
        val codeLines = lines.filter { it.kind == PdfLayout.LineKind.CODE }
        assertEquals(codeLines.map { it.codeGroup }.distinct().size, 1)
        assertTrue(lines.any { it.kind == PdfLayout.LineKind.BULLET && it.text.startsWith("•") })
        assertTrue(lines.last { it.kind == PdfLayout.LineKind.BULLET }.text.endsWith("two"))
    }

    @Test
    fun `empty conversation still yields a title line`() {
        val conversation = Conversation(Platform.GENERIC, "Solo", "", emptyList())
        val lines = PdfLayout.buildLines(conversation, emptyList())
        assertEquals("Solo", lines.first().text)
    }
}
