package com.handoff.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "handoffs")
data class HandoffEntity(
    @PrimaryKey val id: String,
    val title: String,
    val platform: String,
    val url: String,
    val createdAt: Long,
    val messageCount: Int,
    val tokenEstimate: Int,
    /** Full Conversation serialized with HandoffJson. */
    val conversationJson: String,
)
