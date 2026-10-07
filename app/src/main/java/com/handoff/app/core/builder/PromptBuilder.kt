package com.handoff.app.core.builder

import com.handoff.app.core.markdown.MessageMarkdown
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role

data class BuiltPrompt(
    val parts: List<String>,
    val tokenEstimate: Int,
    val mode: PromptMode,
    val target: TargetAi,
    val wasSplit: Boolean,
) {
    val partCount: Int get() = parts.size
}

object PromptBuilder {

    /**
     * Builds the final prompt. When the estimate exceeds the token limit the
     * content is auto-split into labeled parts; each part carries the full
     * wrapper so any assistant can digest it standalone.
     */
    fun build(
        conversation: Conversation,
        mode: PromptMode,
        target: TargetAi,
        limit: TokenLimit = TokenLimit.UNLIMITED,
        autoSplit: Boolean = true,
    ): BuiltPrompt {
        val included = conversation.includedMessages

        // All messages excluded: produce an instructive prompt instead of failing.
        val units = if (included.isEmpty()) {
            listOf(
                "(No messages are included in this handoff. " +
                    "Go back to Preview and include at least one message.)",
            )
        } else {
            contentUnits(included, mode)
        }
        val maxChars = limit.tokens?.let { TokenEstimator.estimateChars(it) - PromptSplitter.WRAPPER_OVERHEAD_CHARS }

        val contents = if (maxChars != null && autoSplit && included.isNotEmpty()) {
            PromptSplitter.splitUnits(units, maxChars)
        } else {
            listOf(units.joinToString("\n\n"))
        }

        val multi = contents.size > 1
        val parts = contents.mapIndexed { index, content ->
            wrap(conversation, target, included.size, content, multi, index + 1, contents.size)
        }
        return BuiltPrompt(
            parts = parts,
            tokenEstimate = parts.sumOf { TokenEstimator.estimateTokens(it) },
            mode = mode,
            target = target,
            wasSplit = multi,
        )
    }

    // ---- Content assembly -------------------------------------------------

    private fun contentUnits(messages: List<Message>, mode: PromptMode): List<String> = when (mode) {
        PromptMode.FULL_TRANSCRIPT -> messages.map { renderLabeled(it) }
        PromptMode.SMART_BRIEF -> smartBriefUnits(messages)
        PromptMode.CODE_AND_DECISIONS -> codeAndDecisionsUnits(messages)
    }

    private fun renderLabeled(message: Message): String {
        val role = when (message.role) {
            Role.USER -> "User"
            Role.ASSISTANT -> "Assistant"
            Role.SYSTEM -> "System"
        }
        val body = MessageMarkdown.render(message.blocks).ifEmpty { "*(no content)*" }
        return "**$role**\n\n$body"
    }

    private fun smartBriefUnits(messages: List<Message>): List<String> {
        val brief = SmartBrief.build(messages)
        val units = mutableListOf<String>()
        units += "**Goal**\n\n${brief.goal.ifEmpty { "Not stated." }}"
        if (brief.decisions.isNotEmpty()) {
            units += "**Key decisions**\n\n" + brief.decisions.joinToString("\n") { "- $it" }
        }
        if (brief.codeBlocks.isNotEmpty()) {
            val code = brief.codeBlocks.joinToString("\n\n") { MessageMarkdown.render(it) ?: "" }
            units += "**Final code / artifacts**\n\n$code"
        }
        if (brief.openQuestions.isNotEmpty()) {
            units += "**Open questions**\n\n" + brief.openQuestions.joinToString("\n") { "- $it" }
        }
        if (brief.lastExchanges.isNotEmpty()) {
            units += "**Last exchanges (verbatim)**\n\n" + brief.lastExchanges.joinToString("\n\n---\n\n") { renderLabeled(it) }
        }
        return units
    }

    private fun codeAndDecisionsUnits(messages: List<Message>): List<String> {
        val units = mutableListOf<String>()
        val decisions = SmartBrief.extractDecisions(messages, cap = 12)
        if (decisions.isNotEmpty()) {
            units += "**Decisions**\n\n" + decisions.joinToString("\n") { "- $it" }
        }
        messages.forEachIndexed { index, message ->
            val code = message.blocks.filterIsInstance<com.handoff.app.core.model.Block.Code>()
                .filter { it.content.isNotBlank() }
            if (code.isNotEmpty()) {
                val role = when (message.role) {
                    Role.USER -> "User"
                    Role.ASSISTANT -> "Assistant"
                    Role.SYSTEM -> "System"
                }
                val label = "From $role, message ${index + 1}"
                val body = code.joinToString("\n\n") { MessageMarkdown.render(it) ?: "" }
                units += "**$label**\n\n$body"
            }
        }
        return units.ifEmpty { listOf("No code blocks or decision statements were found in this conversation.") }
    }

    // ---- Wrapper ------------------------------------------------------------

    private fun wrap(
        conversation: Conversation,
        target: TargetAi,
        messageCount: Int,
        content: String,
        multiPart: Boolean,
        partIndex: Int,
        partTotal: Int,
    ): String {
        val intro = when (target) {
            TargetAi.GENERIC ->
                "You are continuing a conversation that began in another AI assistant.\n" +
                    "Read the context below, do not repeat it back, and do not summarize it\n" +
                    "unless asked. Treat it as shared history. Pick up exactly where it ends."
            TargetAi.CLAUDE ->
                "You are continuing a conversation that began in another AI assistant.\n" +
                    "The context below is verbatim shared history. Do not repeat it back and do\n" +
                    "not summarize it unless asked. Pick up exactly where it ends. Markdown is\n" +
                    "welcome where it aids clarity."
            TargetAi.CHATGPT ->
                "You are continuing a conversation that began in another AI assistant.\n" +
                    "The context below is verbatim shared history. Do not repeat it back and do\n" +
                    "not summarize it unless asked. Pick up exactly where it ends, matching the\n" +
                    "tone of the transcript. Use markdown where it helps."
            TargetAi.GEMINI ->
                "You are continuing a conversation that began in another AI assistant.\n" +
                    "The context below is verbatim shared history. Do not repeat it back and do\n" +
                    "not summarize it unless asked. Pick up exactly where it ends and answer in\n" +
                    "the same style, with markdown where it helps."
        }

        val partSuffix = if (multiPart) " part=\"${partIndex}of$partTotal\"" else ""
        val partBanner = if (multiPart) {
            "[Handoff — Part $partIndex of $partTotal. " +
                "Reply only \"received\" until you have received Part $partTotal.]\n\n"
        } else {
            ""
        }

        val platform = conversation.platform.displayName
        val title = sanitizeTitle(conversation.title)

        return partBanner +
            intro + "\n\n" +
            "<context source=\"$platform\" title=\"$title\" messages=\"$messageCount\"$partSuffix>\n" +
            content + "\n" +
            "</context>\n\n" +
            "My next message: [I will type it here]"
    }

    private fun sanitizeTitle(title: String): String =
        title.replace(Regex("[\"<>\\p{Cntrl}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
            .ifEmpty { "Untitled" }
}
