package com.handoff.app.core.extraction

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Strategy 3 (last resort): chunk visible page text into paragraphs and assign
 * alternating roles starting with the user. Low confidence by design — it only
 * runs when structured extraction failed, and the UI labels the result as
 * heuristic so the user can fix it in Preview or fall back to manual paste.
 */
class GenericHeuristicExtractor {

    fun extract(html: String, url: String): Conversation? {
        val document = Jsoup.parse(html, "")
        document.select("script,style,noscript,nav,footer,header,aside,button,svg,form,iframe").remove()

        val paragraphs = document.body()
            ?.select("div,p,section,article,li,td")
            ?.mapNotNull { element ->
                val text = element.ownText().trim().ifEmpty { null }
                text
            }
            .orEmpty()
            .filter { isContentLine(it) }
            .distinct()
            .toMutableList()

        if (paragraphs.size < 2) return null

        // Merge very short lines into their predecessor (UI remnants, captions).
        val merged = mutableListOf<String>()
        for (text in paragraphs) {
            if (merged.isNotEmpty() && text.length < MERGE_THRESHOLD &&
                merged.last().length + text.length < MAX_MERGED_LENGTH
            ) {
                merged[merged.size - 1] = merged.last() + " " + text
            } else {
                merged.add(text)
            }
        }

        val messages = merged.mapIndexed { index, text ->
            Message(
                role = if (index % 2 == 0) Role.USER else Role.ASSISTANT,
                blocks = listOf(Block.Text(text)),
            )
        }
        return Conversation(
            platform = Platform.GENERIC,
            title = document.title().trim().ifEmpty { "Pasted conversation" },
            url = url,
            messages = messages,
        )
    }

    private fun isContentLine(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < MIN_LENGTH) return false
        if (trimmed.length > MAX_LENGTH) return false
        val lower = trimmed.lowercase()
        return JUNK.none { lower == it || lower.startsWith("$it ") }
    }

    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 4_000
        private const val MERGE_THRESHOLD = 24
        private const val MAX_MERGED_LENGTH = 600

        private val JUNK = listOf(
            "copy", "copy code", "copied", "regenerate", "regenerate response", "retry",
            "edit", "share", "like", "dislike", "thumbs up", "thumbs down", "good response",
            "bad response", "report", "continue", "stop", "new chat", "send", "you",
            "chatgpt", "claude", "gemini", "loading…", "loading...", "thinking…",
        )
    }
}
