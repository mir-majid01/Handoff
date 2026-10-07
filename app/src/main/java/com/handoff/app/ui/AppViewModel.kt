package com.handoff.app.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handoff.app.HandoffApp
import com.handoff.app.data.prefs.AppSettings
import com.handoff.app.data.prefs.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide settings exposed as StateFlow; MainActivity uses it for theming,
 * screens use it for haptics and defaults.
 */
class AppViewModel(app: HandoffApp) : AndroidViewModel(app) {

    val settings: StateFlow<AppSettings> = app.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { getApplication<HandoffApp>().settingsStore.setThemeMode(mode) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { getApplication<HandoffApp>().settingsStore.setHapticsEnabled(enabled) }
    }
}
