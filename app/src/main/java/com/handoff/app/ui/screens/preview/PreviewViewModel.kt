package com.handoff.app.ui.screens.preview

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.core.builder.TokenEstimator
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreviewUiState(
    val conversation: Conversation? = null,
    val searchQuery: String = "",
    /** Original index into the full message list paired with the message. */
    val visibleMessages: List<Pair<Int, Message>> = emptyList(),
    val includedCount: Int = 0,
    val excludedCount: Int = 0,
    val tokenEstimate: Int = 0,
    val collapsedCode: Set<Int> = emptySet(),
)

class PreviewViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val repository = app.repository

    private val conversation = MutableStateFlow<Conversation?>(null)
    private val query = MutableStateFlow("")
    private val collapsedCode = MutableStateFlow<Set<Int>>(emptySet())

    val state: StateFlow<PreviewUiState> =
        combine(conversation, query, collapsedCode) { convo, q, collapsed ->
            val indexed = convo?.messages?.mapIndexed { index, message -> index to message } ?: emptyList()
            val visible = indexed.filter { (_, message) ->
                q.isBlank() || message.plainText.contains(q, ignoreCase = true)
            }
            val includedCount = convo?.messages?.count { !it.excluded } ?: 0
            val excludedCount = convo?.messages?.count { it.excluded } ?: 0
            val estimate = convo?.messages
                ?.filter { !it.excluded }
                ?.sumOf { TokenEstimator.estimateTokens(it.plainText) }
                ?: 0
            PreviewUiState(
                conversation = convo,
                searchQuery = q,
                visibleMessages = visible,
                includedCount = includedCount,
                excludedCount = excludedCount,
                tokenEstimate = estimate,
                collapsedCode = collapsed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreviewUiState())

    fun load(handoffId: String) {
        viewModelScope.launch {
            conversation.value = repository.get(handoffId)
        }
    }

    fun onSearch(queryText: String) {
        query.value = queryText
    }

    fun toggleExcluded(indexInConversation: Int) {
        val current = conversation.value ?: return
        val messages = current.messages.toMutableList()
        val message = messages.getOrNull(indexInConversation) ?: return
        messages[indexInConversation] = message.copy(excluded = !message.excluded)
        conversation.value = current.copy(messages = messages)
    }

    fun toggleCodeCollapsed(blockKey: Int) {
        collapsedCode.value = collapsedCode.value.toMutableSet().apply {
            if (!add(blockKey)) remove(blockKey)
        }
    }

    fun saveChanges(handoffId: String) {
        val current = conversation.value ?: return
        viewModelScope.launch {
            val estimate = current.messages
                .filter { !it.excluded }
                .sumOf { TokenEstimator.estimateTokens(it.plainText) }
            repository.update(handoffId, current, estimate)
        }
    }
}
