package com.handoff.app.core.builder

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartBriefTest {

    @Test
    fun `goal comes from first user message condensed`() {
        val messages = listOf(
            Message(Role.USER, listOf(Block.Text("I want to build a compiler. " + "More detail. ".repeat(80)))),
            Message(Role.ASSISTANT, listOf(Block.Text("Great goal."))),
        )
        val goal = SmartBrief.extractGoal(messages)
        assertTrue(goal.startsWith("I want to build a compiler."))
        assertTrue(goal.length <= 281)
    }

    @Test
    fun `decisions are deduped and capped`() {
        val text = "We should use Room. Let's use Jsoup for parsing. We should use Room. " +
            "We decided on sage accents. Decided to ship offline-first. " +
            "Decided to ship offline-first! Chose Kotlin for the core. Decided on sage accents."
        val decisions = SmartBrief.extractDecisions(listOf(Message(Role.USER, listOf(Block.Text(text)))), cap = 4)
        assertEquals(4, decisions.size)
        assertEquals("We should use Room.", decisions.first())
    }

    @Test
    fun `final code comes from last code-bearing message`() {
        val messages = listOf(
            Message(Role.ASSISTANT, listOf(Block.Code("kotlin", "val a = 1"))),
            Message(Role.USER, listOf(Block.Text("thanks"))),
            Message(Role.ASSISTANT, listOf(Block.Text("no code here"), Block.Code("py", "b = 2"))),
        )
        val code = SmartBrief.extractFinalCode(messages)
        assertEquals(1, code.size)
        assertEquals("py", code.first().language)
    }

    @Test
    fun `open questions exclude the last exchange`() {
        val messages = listOf(
            Message(Role.USER, listOf(Block.Text("Should we use REST?"))),
            Message(Role.ASSISTANT, listOf(Block.Text("Yes."))),
            Message(Role.USER, listOf(Block.Text("What about GraphQL?"))),
            Message(Role.ASSISTANT, listOf(Block.Text("Also fine."))),
            Message(Role.USER, listOf(Block.Text("And websockets?"))),
            Message(Role.ASSISTANT, listOf(Block.Text("For realtime."))),
        )
        val questions = SmartBrief.extractOpenQuestions(messages, cap = 5)
        assertTrue(questions.contains("Should we use REST?"))
        assertTrue(questions.contains("What about GraphQL?"))
        assertTrue(!questions.contains("And websockets?"))
    }

    @Test
    fun `brief includes last six messages verbatim`() {
        val messages = (1..10).map { Message(Role.USER, listOf(Block.Text("m$it"))) }
        val brief = SmartBrief.build(messages)
        assertEquals(6, brief.lastExchanges.size)
        assertTrue(brief.lastExchanges.first().plainText.contains("m5"))
        assertTrue(brief.lastExchanges.last().plainText.contains("m10"))
    }
}
