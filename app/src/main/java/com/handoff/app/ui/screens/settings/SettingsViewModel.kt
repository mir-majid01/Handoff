package com.handoff.app.ui.screens.settings

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.BuildConfig
import com.handoff.app.HandoffApp
import com.handoff.app.core.builder.PromptMode
import com.handoff.app.core.builder.TargetAi
import com.handoff.app.core.builder.TokenLimit
import com.handoff.app.data.prefs.ThemeMode
import kotlinx.coroutines.launch

class SettingsViewModel(app: HandoffApp) : AndroidViewModel(app) {

    private val settingsStore = getApplication<HandoffApp>().settingsStore
    private val repository = getApplication<HandoffApp>().repository

    val settings = settingsStore.settings
    val version: String = BuildConfig.VERSION_NAME

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }
    fun setDefaultMode(mode: PromptMode) = viewModelScope.launch { settingsStore.setDefaultMode(mode) }
    fun setDefaultTarget(target: TargetAi) = viewModelScope.launch { settingsStore.setDefaultTarget(target) }
    fun setTokenLimit(limit: TokenLimit) = viewModelScope.launch { settingsStore.setTokenLimit(limit) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { settingsStore.setHapticsEnabled(enabled) }

    fun clearHistory(onCleared: () -> Unit) = viewModelScope.launch {
        repository.clear()
        onCleared()
    }
}
