package com.handoff.app.ui.screens.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.core.builder.BuiltPrompt
import com.handoff.app.core.builder.PromptBuilder
import com.handoff.app.core.builder.PromptMode
import com.handoff.app.core.builder.TargetAi
import com.handoff.app.core.builder.TokenLimit
import com.handoff.app.core.export.PdfExporter
import com.handoff.app.core.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExportUiState(
    val conversation: Conversation? = null,
    val mode: PromptMode = PromptMode.FULL_TRANSCRIPT,
    val target: TargetAi = TargetAi.GENERIC,
    val limit: TokenLimit = TokenLimit.UNLIMITED,
    val prompt: BuiltPrompt? = null,
)

class ExportViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val repository = app.repository
    private val settingsStore = app.settingsStore
    private val pdfExporter = PdfExporter(app)

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun load(handoffId: String) {
        viewModelScope.launch {
            val conversation = repository.get(handoffId)
            val settings = settingsStore.settings.firstOrNull()
            _state.value = _state.value.copy(
                conversation = conversation,
                mode = settings?.defaultMode ?: PromptMode.FULL_TRANSCRIPT,
                target = settings?.defaultTarget ?: TargetAi.GENERIC,
                limit = settings?.tokenLimit ?: TokenLimit.UNLIMITED,
            )
            rebuild()
        }
    }

    fun setMode(mode: PromptMode) {
        _state.value = _state.value.copy(mode = mode)
        rebuild()
    }

    fun setTarget(target: TargetAi) {
        _state.value = _state.value.copy(target = target)
        rebuild()
    }

    fun setLimit(limit: TokenLimit) {
        _state.value = _state.value.copy(limit = limit)
        rebuild()
    }

    private fun rebuild() {
        val conversation = _state.value.conversation ?: return
        val mode = _state.value.mode
        val target = _state.value.target
        val limit = _state.value.limit
        viewModelScope.launch(Dispatchers.Default) {
            val prompt = PromptBuilder.build(
                conversation = conversation,
                mode = mode,
                target = target,
                limit = limit,
            )
            _state.value = _state.value.copy(prompt = prompt)
        }
    }

    /** Prompt text for clipboard/share/txt. */
    fun promptText(): String = _state.value.prompt?.parts?.joinToString("\n\n") ?: ""

    /** Transcript PDF bytes; runs off the main thread. */
    suspend fun writePdf(output: java.io.OutputStream): Boolean =
        withContext(Dispatchers.Default) {
            val conversation = _state.value.conversation ?: return@withContext false
            try {
                pdfExporter.write(
                    conversation = conversation,
                    messages = conversation.includedMessages,
                    output = output,
                )
                true
            } catch (error: Exception) {
                false
            }
        }

    /** Writes the prompt to a SAF uri. */
    suspend fun writeText(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getApplication<HandoffApp>().contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(promptText().toByteArray(Charsets.UTF_8))
            } ?: return@withContext false
            true
        } catch (error: Exception) {
            false
        }
    }

    suspend fun writePdfToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getApplication<HandoffApp>().contentResolver.openOutputStream(uri)?.use { stream ->
                writePdf(stream)
            } ?: false
        } catch (error: Exception) {
            false
        }
    }

    fun shareIntent(): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, promptText())
            putExtra(
                Intent.EXTRA_TITLE,
                _state.value.conversation?.title ?: "Handoff prompt",
            )
        }
        return Intent.createChooser(intent, getApplication<HandoffApp>()
            .getString(com.handoff.app.R.string.share_intent_title))
    }

    fun fileName(extension: String): String {
        val title = _state.value.conversation?.title ?: "handoff"
        val safe = title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim()
            .replace(Regex("\\s+"), "_").take(60).ifEmpty { "handoff" }
        return "$safe.$extension"
    }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager ?: return
        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText("Handoff prompt", promptText()),
        )
    }
}
