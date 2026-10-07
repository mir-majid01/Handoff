package com.handoff.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.handoff.app.data.prefs.AppSettings
import com.handoff.app.data.prefs.ThemeMode
import com.handoff.app.ui.navigation.HandoffNavHost
import com.handoff.app.ui.theme.HandoffTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HandoffApp
        val sharedUrl = extractSharedUrl(intent)

        setContent {
            val settings by app.settingsStore.settings.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            HandoffTheme(darkTheme = darkTheme) {
                HandoffNavHost(app = app, initialSharedUrl = sharedUrl)
            }
        }
    }

    /** Accepts ACTION_SEND text (share a link straight from Claude/ChatGPT/Gemini). */
    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        return text.ifEmpty { null }
    }
}
