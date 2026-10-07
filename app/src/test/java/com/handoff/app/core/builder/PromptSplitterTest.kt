package com.handoff.app.core.builder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptSplitterTest {

    @Test
    fun `fits in one part when under budget`() {
        val units = listOf("alpha", "beta")
        val parts = PromptSplitter.splitUnits(units, maxCharsPerPart = 100)
        assertEquals(1, parts.size)
        assertTrue(parts[0].contains("alpha"))
        assertTrue(parts[0].contains("beta"))
    }

    @Test
    fun `splits at unit boundaries without losing content`() {
        val units = (1..10).map { "unit-$it-${"x".repeat(30)}" }
        val parts = PromptSplitter.splitUnits(units, maxCharsPerPart = 100)
        assertTrue(parts.size > 1)
        parts.forEach { assertTrue(it.length <= 100) }
        val joined = parts.joinToString("\n")
        units.forEach { assertTrue(joined.contains(it)) }
        // Units are never broken: every unit appears intact.
        units.forEach { unit -> assertTrue(parts.any { it.contains(unit) }) }
    }

    @Test
    fun `oversized unit is hard split by paragraphs`() {
        val paragraph = "p".repeat(60)
        val unit = "$paragraph\n\n$paragraph\n\n$paragraph"
        val parts = PromptSplitter.splitUnits(listOf(unit), maxCharsPerPart = 80)
        assertTrue(parts.size >= 3)
        parts.forEach { assertTrue(it.length <= 80) }
        assertEquals(unit.length, parts.joinToString("").length + (parts.size - 1) * 2)
    }

    @Test
    fun `single pathological line is chunked raw`() {
        val unit = "y".repeat(250)
        val parts = PromptSplitter.splitUnits(listOf(unit), maxCharsPerPart = 100)
        assertTrue(parts.size >= 3)
        parts.forEach { assertTrue(it.length <= 100) }
        assertEquals(250, parts.joinToString("").length)
    }

    @Test
    fun `tiny budget still produces output`() {
        val parts = PromptSplitter.splitUnits(listOf("hello world"), maxCharsPerPart = 5)
        assertTrue(parts.isNotEmpty())
        assertTrue(parts.joinToString("").contains("hello"))
    }

    @Test
    fun `empty input yields single empty part`() {
        val parts = PromptSplitter.splitUnits(emptyList(), maxCharsPerPart = 100)
        assertEquals(listOf(""), parts)
    }
}
