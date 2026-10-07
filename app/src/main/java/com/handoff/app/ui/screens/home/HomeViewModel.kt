package com.handoff.app.ui.screens.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.core.extraction.ExtractorsConfig
import com.handoff.app.core.extraction.ExtractorsConfigLoader
import com.handoff.app.core.extraction.LinkDetector
import com.handoff.app.core.model.Platform
import com.handoff.app.data.repo.HandoffEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val repository = app.repository
    private val config: ExtractorsConfig = ExtractorsConfigLoader.load(app)

    val history: StateFlow<List<HandoffEntry>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _detectedPlatform = MutableStateFlow<Platform?>(null)
    val detectedPlatform: StateFlow<Platform?> = _detectedPlatform.asStateFlow()

    private val _clipboardLink = MutableStateFlow<Pair<String, Platform>?>(null)
    val clipboardLink: StateFlow<Pair<String, Platform>?> = _clipboardLink.asStateFlow()

    private val _showInvalid = MutableStateFlow(false)
    val showInvalid: StateFlow<Boolean> = _showInvalid.asStateFlow()

    fun onTextChanged(value: String) {
        _text.value = value
        _showInvalid.value = false
        _detectedPlatform.value = detectPlatform(value)
    }

    fun readClipboard() {
        val clipboard = getApplication<HandoffApp>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        _text.value = clipText.trim()
        _detectedPlatform.value = detectPlatform(clipText)
        _showInvalid.value = _text.value.isNotEmpty() && _detectedPlatform.value == null
    }

    /** Called when Home resumes: populates the one-tap banner if the clipboard holds a link. */
    fun checkClipboardForBanner() {
        val clipboard = getApplication<HandoffApp>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
        val platform = detectPlatform(clipText)
        _clipboardLink.value = if (platform != null && clipText != _text.value) {
            clipText to platform
        } else {
            null
        }
    }

    fun dismissClipboardBanner() {
        _clipboardLink.value = null
    }

    fun useClipboardBannerLink() {
        val (url, platform) = _clipboardLink.value ?: return
        _text.value = url
        _detectedPlatform.value = platform
        _clipboardLink.value = null
    }

    /** Returns the detected platform, or null (and shows the invalid hint). */
    fun startIfValid(): Platform? {
        val platform = detectPlatform(_text.value)
        if (platform == null) {
            _showInvalid.value = true
        }
        return platform
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }

    /** Swipe-to-delete from the Recent list; History keeps the undo window. */
    fun delete(entry: HandoffEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    private fun detectPlatform(raw: String): Platform? = when (val result = LinkDetector.detect(raw, config)) {
        is LinkDetector.Result.Detected -> result.platform
        is LinkDetector.Result.Unknown -> null
    }
}
