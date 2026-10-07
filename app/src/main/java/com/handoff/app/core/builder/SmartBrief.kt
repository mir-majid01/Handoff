package com.handoff.app.core.builder

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Role

/**
 * Local, rule-based condensation for Smart Brief mode: goal, key decisions,
 * final code/artifacts, open questions, and the last three exchanges verbatim.
 * Deterministic and fully offline.
 */
object SmartBrief {

    data class Brief(
        val goal: String,
        val decisions: List<String>,
        val codeBlocks: List<Block.Code>,
        val openQuestions: List<String>,
        val lastExchanges: List<Message>,
    )

    private val DECISION_KEYWORDS = listOf(
        "decided", "decision", "we'll go", "we will go", "go with", "going with",
        "let's use", "lets use", "we'll use", "we will use", "chose", "chosen",
        "instead of", "rather than", "switch to", "switched to", "agreed",
        "settled on", "final approach", "from now on", "keep it", "stick with",
        "should use", "we should",
    )

    private val SENTENCE_SPLIT = Regex("(?<=[.!?])\\s+")

    fun build(messages: List<Message>): Brief {
        val goal = extractGoal(messages)
        val decisions = extractDecisions(messages, cap = 8)
        val code = extractFinalCode(messages)
        val questions = extractOpenQuestions(messages, cap = 5)
        val last = messages.takeLast(6)
        return Brief(goal, decisions, code, questions, last)
    }

    internal fun extractGoal(messages: List<Message>): String {
        val firstUser = messages.firstOrNull { it.role == Role.USER } ?: return ""
        return condense(firstUser.plainText, maxSentences = 2, maxChars = 280)
    }

    internal fun extractDecisions(messages: List<Message>, cap: Int): List<String> {
        val seen = mutableListOf<String>()
        for (message in messages) {
            for (sentence in sentencesOf(message.plainText)) {
                val lower = sentence.lowercase()
                if (DECISION_KEYWORDS.any { lower.contains(it) }) {
                    val normalized = sentence.trim()
                    val key = normalized.lowercase().trimEnd('.', '!', '?', ',', ';', ':').take(120)
                    if (seen.none { it.lowercase().trimEnd('.', '!', '?', ',', ';', ':').take(120) == key }) {
                        seen.add(normalized)
                        if (seen.size >= cap) return seen
                    }
                }
            }
        }
        return seen
    }

    /**
     * "Final" code: every code block of the last assistant message that has one.
     * If that message has none, walk backwards to the most recent code-bearing message.
     */
    internal fun extractFinalCode(messages: List<Message>): List<Block.Code> {
        val lastAssistantIndex = messages.indexOfLast { it.role == Role.ASSISTANT }
        if (lastAssistantIndex == -1) return emptyList()
        for (index in lastAssistantIndex downTo 0) {
            val code = messages[index].blocks.filterIsInstance<Block.Code>().filter { it.content.isNotBlank() }
            if (code.isNotEmpty()) return code
        }
        return emptyList()
    }

    /** User questions among the last six messages, excluding the final exchange, capped. */
    internal fun extractOpenQuestions(messages: List<Message>, cap: Int): List<String> {
        if (messages.size <= 2) return emptyList()
        val window = messages.takeLast(6).dropLast(2)
        val questions = window
            .filter { it.role == Role.USER }
            .map { it.plainText.trim() }
            .filter { it.endsWith("?") }
        return questions.takeLast(cap)
    }

    internal fun sentencesOf(text: String): List<String> =
        text.split(SENTENCE_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }

    private fun condense(text: String, maxSentences: Int, maxChars: Int): String {
        val parts = sentencesOf(text).take(maxSentences).joinToString(" ")
        return if (parts.length <= maxChars) parts else parts.take(maxChars - 1).trimEnd() + "…"
    }
}
