package com.handoff.app.ui.screens.manual

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.core.builder.TokenEstimator
import com.handoff.app.core.extraction.ManualPasteParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManualPasteViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val repository = app.repository

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _showEmptyError = MutableStateFlow(false)
    val showEmptyError: StateFlow<Boolean> = _showEmptyError.asStateFlow()

    fun onTextChange(value: String) {
        _text.value = value
        _showEmptyError.value = false
    }

    /** Parses and saves the pasted chat; returns the new handoff id or null. */
    suspend fun buildAndSave(): String? = withContext(Dispatchers.Default) {
        val conversation = ManualPasteParser.parse(_text.value)
        if (conversation == null) {
            _showEmptyError.value = true
            return@withContext null
        }
        val estimate = TokenEstimator.estimateTokens(
            conversation.messages.joinToString("\n") { it.plainText },
        )
        repository.save(conversation, estimate)
    }
}
