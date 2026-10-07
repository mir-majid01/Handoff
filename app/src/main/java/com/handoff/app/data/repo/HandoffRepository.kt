package com.handoff.app.data.repo

import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.HandoffJson
import com.handoff.app.core.model.Platform
import com.handoff.app.data.db.HandoffDao
import com.handoff.app.data.db.HandoffEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import java.util.UUID

data class HandoffEntry(
    val id: String,
    val title: String,
    val platform: Platform,
    val url: String,
    val createdAt: Long,
    val messageCount: Int,
    val tokenEstimate: Int,
    val snippet: String? = null,
)

/** Single gateway between UI/ViewModels and Room + JSON persistence. */
class HandoffRepository(private val dao: HandoffDao) {

    fun observeHistory(): Flow<List<HandoffEntry>> =
        dao.observeAll().map { list -> list.map { it.toEntry() } }

    suspend fun save(conversation: Conversation, tokenEstimate: Int): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(
            HandoffEntity(
                id = id,
                title = conversation.title,
                platform = conversation.platform.name,
                url = conversation.url,
                createdAt = System.currentTimeMillis(),
                messageCount = conversation.messages.size,
                tokenEstimate = tokenEstimate,
                conversationJson = HandoffJson.encodeToString(conversation),
            ),
        )
        return id
    }

    suspend fun update(id: String, conversation: Conversation, tokenEstimate: Int) {
        val existing = dao.getById(id) ?: return
        dao.upsert(
            existing.copy(
                title = conversation.title,
                messageCount = conversation.messages.size,
                tokenEstimate = tokenEstimate,
                conversationJson = HandoffJson.encodeToString(conversation),
            ),
        )
    }

    suspend fun get(id: String): Conversation? =
        dao.getById(id)?.let { entity ->
            runCatching { HandoffJson.decodeFromString<Conversation>(entity.conversationJson) }.getOrNull()
        }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clear() = dao.clear()

    /** Re-inserts a deleted handoff with its full conversation (History undo). */
    suspend fun restore(entry: HandoffEntry, conversation: Conversation?) {
        dao.upsert(
            HandoffEntity(
                id = entry.id,
                title = entry.title,
                platform = entry.platform.name,
                url = entry.url,
                createdAt = entry.createdAt,
                messageCount = entry.messageCount,
                tokenEstimate = entry.tokenEstimate,
                conversationJson = conversation?.let { HandoffJson.encodeToString(it) } ?: "",
            ),
        )
    }

    private fun HandoffEntity.toEntry() = HandoffEntry(
        id = id,
        title = title,
        platform = runCatching { Platform.valueOf(platform) }.getOrDefault(Platform.GENERIC),
        url = url,
        createdAt = createdAt,
        messageCount = messageCount,
        tokenEstimate = tokenEstimate,
        snippet = firstUserLine(conversationJson),
    )

    /** First line of the first user message, for list rows whose title is only the platform. */
    private fun firstUserLine(conversationJson: String): String? = runCatching {
        HandoffJson.decodeFromString<Conversation>(conversationJson)
    }.getOrNull()?.messages
        ?.firstOrNull { it.role == com.handoff.app.core.model.Role.USER }
        ?.plainText
        ?.lineSequence()
        ?.firstOrNull { it.isNotBlank() }
        ?.trim()
}
