package com.handoff.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.handoff.app.core.builder.PromptMode
import com.handoff.app.core.builder.TargetAi
import com.handoff.app.core.builder.TokenLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "handoff_settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultMode: PromptMode = PromptMode.FULL_TRANSCRIPT,
    val defaultTarget: TargetAi = TargetAi.GENERIC,
    val hapticsEnabled: Boolean = true,
    val tokenLimit: TokenLimit = TokenLimit.UNLIMITED,
    val onboardingDone: Boolean = false,
)

/** All user preferences, on-device only. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val DEFAULT_TARGET = stringPreferencesKey("default_target")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val TOKEN_LIMIT = stringPreferencesKey("token_limit")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultMode = prefs[Keys.DEFAULT_MODE]?.let { runCatching { PromptMode.valueOf(it) }.getOrNull() }
                ?: PromptMode.FULL_TRANSCRIPT,
            defaultTarget = prefs[Keys.DEFAULT_TARGET]?.let { runCatching { TargetAi.valueOf(it) }.getOrNull() }
                ?: TargetAi.GENERIC,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            tokenLimit = prefs[Keys.TOKEN_LIMIT]?.let { runCatching { TokenLimit.valueOf(it) }.getOrNull() }
                ?: TokenLimit.UNLIMITED,
            onboardingDone = prefs[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setDefaultMode(mode: PromptMode) =
        context.dataStore.edit { it[Keys.DEFAULT_MODE] = mode.name }

    suspend fun setDefaultTarget(target: TargetAi) =
        context.dataStore.edit { it[Keys.DEFAULT_TARGET] = target.name }

    suspend fun setHapticsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }

    suspend fun setTokenLimit(limit: TokenLimit) =
        context.dataStore.edit { it[Keys.TOKEN_LIMIT] = limit.name }

    suspend fun setOnboardingDone(done: Boolean) =
        context.dataStore.edit { it[Keys.ONBOARDED] = done }
}
