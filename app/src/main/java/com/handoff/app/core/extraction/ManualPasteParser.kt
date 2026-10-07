package com.handoff.app.core.extraction

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role

/**
 * Manual paste fallback: parses raw chat text the user pasted. Recognizes
 * common role prefixes ("You:", "User:", "Assistant:", "ChatGPT:", "Claude:",
 "AI:", "Me:") and otherwise alternates on paragraph boundaries.
 */
object ManualPasteParser {

    private val ROLE_PREFIX = Regex(
        "^\\s*(you|user|me|human)\\s*[:\\-—]\\s*",
        RegexOption.IGNORE_CASE,
    )

    private val ASSISTANT_PREFIX = Regex(
        "^\\s*(assistant|chatgpt|claude|gemini|bard|ai|model|gpt)\\s*[:\\-—]\\s*",
        RegexOption.IGNORE_CASE,
    )

    fun parse(rawText: String, platform: Platform = Platform.GENERIC): Conversation? {
        val paragraphs = rawText.split(Regex("\n\\s*\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) return null

        val messages = mutableListOf<Message>()
        for (paragraph in paragraphs) {
            val labeled = labeledRole(paragraph)
            if (labeled != null) {
                val (role, body) = labeled
                if (body.trim().isEmpty()) continue
                val last = messages.lastOrNull()
                if (last != null && last.role == role) {
                    messages[messages.size - 1] = last.copy(blocks = last.blocks + Block.Text(body.trim()))
                } else {
                    messages.add(Message(role = role, blocks = listOf(Block.Text(body.trim()))))
                }
            } else {
                // Unlabeled paragraph: alternate strictly, one message per paragraph.
                val role = when (messages.lastOrNull()?.role) {
                    Role.USER -> Role.ASSISTANT
                    Role.ASSISTANT -> Role.USER
                    else -> Role.USER
                }
                messages.add(Message(role = role, blocks = listOf(Block.Text(paragraph))))
            }
        }
        if (messages.isEmpty()) return null
        return Conversation(
            platform = platform,
            title = "Pasted conversation",
            url = "",
            messages = messages,
        )
    }

    /** Returns (role, remainder) when the paragraph starts with a role label. */
    private fun labeledRole(paragraph: String): Pair<Role, String>? {
        ROLE_PREFIX.find(paragraph)?.let { return Role.USER to paragraph.substring(it.value.length) }
        ASSISTANT_PREFIX.find(paragraph)?.let { return Role.ASSISTANT to paragraph.substring(it.value.length) }
        return null
    }
}
