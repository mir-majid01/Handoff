package com.handoff.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Which product a conversation came from. GENERIC covers manual paste of unknown origin. */
@Serializable
enum class Platform(val displayName: String) {
    CLAUDE("Claude"),
    CHATGPT("ChatGPT"),
    GEMINI("Gemini"),
    KIMI("Kimi"),
    DEEPSEEK("DeepSeek"),
    QWEN("Qwen"),
    GENERIC("Chat"),
}

@Serializable
enum class Role {
    USER,
    ASSISTANT,
    SYSTEM,
}

/** A normalized piece of message content, preserving structure as Markdown-safe pieces. */
@Serializable
sealed class Block {

    @Serializable
    @SerialName("text")
    data class Text(val text: String) : Block()

    @Serializable
    @SerialName("heading")
    data class Heading(val level: Int, val text: String) : Block()

    @Serializable
    @SerialName("code")
    data class Code(val language: String, val content: String) : Block()

    @Serializable
    @SerialName("list")
    data class ListBlock(val ordered: Boolean, val items: List<String>) : Block()

    @Serializable
    @SerialName("image")
    data class Image(val alt: String, val url: String? = null) : Block()
}

@Serializable
data class Message(
    val role: Role,
    val blocks: List<Block>,
    /** UI state: user toggled this message off in Preview so it won't reach the prompt. */
    val excluded: Boolean = false,
) {
    val plainText: String
        get() = blocks.joinToString("\n\n") { block ->
            when (block) {
                is Block.Text -> block.text
                is Block.Heading -> block.text
                is Block.Code -> block.content
                is Block.ListBlock -> block.items.joinToString("\n")
                is Block.Image -> block.alt
            }
        }.trim()
}

@Serializable
data class Conversation(
    val platform: Platform,
    val title: String,
    val url: String,
    val messages: List<Message>,
) {
    val includedMessages: List<Message> get() = messages.filterNot { it.excluded }
}
