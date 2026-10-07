package com.handoff.app.ui.screens.history

import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.builder.PromptBuilder
import com.handoff.app.core.builder.PromptMode
import com.handoff.app.core.builder.TargetAi
import com.handoff.app.core.builder.TokenLimit
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Platform
import com.handoff.app.data.repo.HandoffEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeletedHandoff(
    val entry: HandoffEntry,
    val conversation: Conversation?,
)

data class HistoryUiState(
    val entries: List<HandoffEntry> = emptyList(),
    val query: String = "",
    val filter: Platform? = null,
    val allPlatforms: List<Platform> = emptyList(),
    val deleted: DeletedHandoff? = null,
)

class HistoryViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val repository = app.repository
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<Platform?>(null)
    private val deleted = MutableStateFlow<DeletedHandoff?>(null)
    private var undoExpirationJob: kotlinx.coroutines.Job? = null

    val state: StateFlow<HistoryUiState> =
        combine(repository.observeHistory(), query, filter, deleted) { entries, q, platformFilter, pendingDelete ->
            val present = entries.map { it.platform }.distinct()
            val visible = entries.filter { entry ->
                (platformFilter == null || entry.platform == platformFilter) &&
                    (q.isBlank() ||
                        entry.title.contains(q, ignoreCase = true) ||
                        entry.snippet?.contains(q, ignoreCase = true) == true ||
                        entry.platform.displayName.contains(q, ignoreCase = true))
            }
            HistoryUiState(
                entries = visible,
                query = q,
                filter = platformFilter,
                allPlatforms = present,
                deleted = pendingDelete,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onSearch(text: String) {
        query.value = text
    }

    fun onFilter(platform: Platform?) {
        filter.value = platform
    }

    /** Optimistic delete with a 5s undo window; the record is held in memory. */
    fun deleteWithUndo(entry: HandoffEntry) {
        viewModelScope.launch {
            val conversation = repository.get(entry.id)
            repository.delete(entry.id)
            deleted.value = DeletedHandoff(entry, conversation)
            undoExpirationJob?.cancel()
            undoExpirationJob = viewModelScope.launch {
                delay(5_000)
                deleted.value = null
            }
        }
    }

    fun undoDelete() {
        val deletedHandoff = deleted.value ?: return
        undoExpirationJob?.cancel()
        viewModelScope.launch {
            repository.restore(deletedHandoff.entry, deletedHandoff.conversation)
            deleted.value = null
        }
    }

    private suspend fun promptText(entry: HandoffEntry): String? =
        repository.get(entry.id)?.let { conversation ->
            PromptBuilder.build(
                conversation = conversation,
                mode = PromptMode.FULL_TRANSCRIPT,
                target = TargetAi.GENERIC,
                limit = TokenLimit.UNLIMITED,
            ).parts.joinToString("\n\n")
        }

    /** Builds the full-transcript prompt and copies it to the clipboard. Returns success. */
    suspend fun copyPrompt(entry: HandoffEntry): Boolean {
        val text = promptText(entry) ?: return false
        val clipboard = getApplication<HandoffApp>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return false
        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText("Handoff prompt", text),
        )
        return true
    }

    /** Builds a share chooser for the entry's prompt, or null if it can't be read. */
    suspend fun shareIntent(entry: HandoffEntry): Intent? {
        val text = promptText(entry) ?: return null
        val app = getApplication<HandoffApp>()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, entry.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(send, app.getString(R.string.share_intent_title))
    }
}
