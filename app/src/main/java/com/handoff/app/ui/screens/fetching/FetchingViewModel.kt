package com.handoff.app.ui.screens.fetching

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.core.builder.TokenEstimator
import com.handoff.app.core.extraction.ExtractionError
import com.handoff.app.core.extraction.FetchStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FetchUiState {
    data class Running(val stage: FetchStage) : FetchUiState
    data class Success(val handoffId: String) : FetchUiState
    data class Failed(val error: ExtractionError) : FetchUiState
}

class FetchingViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val engine = app.extractionEngine
    private val repository = app.repository

    private val _state = MutableStateFlow<FetchUiState>(FetchUiState.Running(FetchStage.CONNECTING))
    val state: StateFlow<FetchUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun start(url: String) {
        if (job?.isActive == true) return
        _state.value = FetchUiState.Running(FetchStage.CONNECTING)
        job = viewModelScope.launch(Dispatchers.IO) {
            // Blocking OkHttp/Jsoup work must stay off the main dispatcher.
            try {
                val conversation = engine.extract(url) { stage ->
                    _state.value = FetchUiState.Running(stage)
                }
                val estimate = TokenEstimator.estimateTokens(
                    conversation.messages.joinToString("\n") { it.plainText },
                )
                val id = repository.save(conversation, estimate)
                _state.value = FetchUiState.Success(id)
            } catch (error: ExtractionError) {
                _state.value = FetchUiState.Failed(error)
            } catch (error: IllegalArgumentException) {
                _state.value = FetchUiState.Failed(ExtractionError.ParseFailed(error.message ?: "empty"))
            } catch (t: Throwable) {
                // Never let an unexpected failure hang the screen or crash the app.
                _state.value = FetchUiState.Failed(ExtractionError.ParseFailed(t.message ?: "unexpected"))
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
