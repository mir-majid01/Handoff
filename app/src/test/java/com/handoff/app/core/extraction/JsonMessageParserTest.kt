package com.handoff.app.core.extraction

import com.handoff.app.core.model.Role
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMessageParserTest {

    private fun loadFixture(name: String): String =
        javaClass.getResource("/fixtures/$name")!!.readText()

    @Test
    fun `claude style array parses sender and content blocks`() {
        val root = Json.parseToJsonElement(loadFixture("claude_share.json"))
        val messages = JsonMessageParser.deepSearch(root)!!

        assertEquals(4, messages.size)
        assertEquals(Role.USER, messages[0].role)
        assertEquals("How do I make a sourdough starter from scratch?", messages[0].plainText)
        assertEquals(Role.ASSISTANT, messages[1].role)
        assertTrue(messages[1].plainText.contains("Feed it daily"))
    }

    @Test
    fun `chatgpt mapping walks from current node in order`() {
        val root = Json.parseToJsonElement(loadFixture("chatgpt_share.json"))
        val messages = JsonMessageParser.parse(root, "chatgpt_mapping")!!

        assertEquals(2, messages.size)
        assertEquals(Role.USER, messages[0].role)
        assertEquals("Explain quicksort briefly and give me Python code.", messages[0].plainText)
        assertEquals(Role.ASSISTANT, messages[1].role)
        assertTrue(messages[1].plainText.contains("partitions the array"))
    }

    @Test
    fun `deep search finds mapping nested in state`() {
        val root = Json.parseToJsonElement(loadFixture("chatgpt_share.json"))
        val messages = JsonMessageParser.deepSearch(root)!!
        assertEquals(2, messages.size)
    }

    @Test
    fun `content parts with code become code blocks`() {
        val json = """
            [{"role":"assistant","content":[
               {"type":"text","text":"Here you go:"},
               {"type":"code","language":"python","content":"print('hi')"},
               {"type":"image","text":"diagram"},
               {"type":"tool_use","content":null}
            ]}]
        """.trimIndent()
        val messages = JsonMessageParser.fromMessageArray(
            Json.parseToJsonElement(json) as kotlinx.serialization.json.JsonArray,
        )
        val blocks = messages.single().blocks
        assertEquals("blocks=$blocks", 3, blocks.size)
        assertTrue(blocks[0] is com.handoff.app.core.model.Block.Text)
        val code = blocks[1] as com.handoff.app.core.model.Block.Code
        assertEquals("python", code.language)
        assertEquals("print('hi')", code.content)
        assertTrue(blocks[2] is com.handoff.app.core.model.Block.Image)
    }

    @Test
    fun `empty or irrelevant json yields null`() {
        assertNull(JsonMessageParser.deepSearch(Json.parseToJsonElement("{\"a\":1,\"b\":\"x\"}")))
        assertNull(JsonMessageParser.deepSearch(Json.parseToJsonElement("[]")))
    }

    @Test
    fun `role normalization covers platform vocabulary`() {
        assertEquals(Role.USER, JsonMessageParser.normalizeRole("human"))
        assertEquals(Role.ASSISTANT, JsonMessageParser.normalizeRole("model"))
        assertEquals(Role.SYSTEM, JsonMessageParser.normalizeRole("system"))
        assertNull(JsonMessageParser.normalizeRole("tool"))
    }
}
