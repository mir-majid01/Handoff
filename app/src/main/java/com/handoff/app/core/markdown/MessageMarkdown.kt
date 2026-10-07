package com.handoff.app.core.markdown

import com.handoff.app.core.model.Block

/** Renders normalized blocks to clean Markdown, preserving code, lists, headings. */
object MessageMarkdown {

    fun render(blocks: List<Block>): String =
        blocks.mapNotNull { render(it) }.joinToString("\n\n").trim()

    fun render(block: Block): String? = when (block) {
        is Block.Text -> block.text.trim().takeIf { it.isNotEmpty() }
        is Block.Heading -> "#".repeat(block.level.coerceIn(1, 6)).let { "$it ${block.text.trim()}" }
        is Block.Code -> renderCode(block)
        is Block.ListBlock -> block.items
            .filter { it.isNotBlank() }
            .mapIndexed { index, item ->
                val marker = if (block.ordered) "${index + 1}." else "-"
                "$marker ${item.trim()}"
            }
            .joinToString("\n")
            .takeIf { it.isNotEmpty() }
        is Block.Image -> if (block.url != null) {
            "![${block.alt}](${block.url})"
        } else {
            "*[image: ${block.alt}]*"
        }
    }

    private fun renderCode(block: Block.Code): String {
        val content = block.content.trimEnd('\n', '\r', ' ')
        if (content.isEmpty()) return ""
        val fence = buildString {
            append("```")
            while (content.contains(this)) append("`")
        }
        val language = block.language.trim().ifEmpty { "" }
        return "$fence$language\n$content\n$fence"
    }
}
