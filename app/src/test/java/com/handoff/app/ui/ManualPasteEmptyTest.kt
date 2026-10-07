package com.handoff.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handoff.app.HandoffApp
import com.handoff.app.ui.screens.manual.ManualPasteScreen
import com.handoff.app.ui.theme.HandoffTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Failure scenario end to end: tapping "Build prompt" with nothing pasted must
 * surface the inline error and never navigate away (onBuilt stays uncalled).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = HandoffApp::class)
class ManualPasteEmptyTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyPaste_showsError_andDoesNotNavigate() {
        val app = ApplicationProvider.getApplicationContext<HandoffApp>()
        var builtId: String? = null
        composeRule.setContent {
            HandoffTheme {
                ManualPasteScreen(
                    onBuilt = { builtId = it },
                    onBack = {},
                    app = app,
                )
            }
        }

        composeRule.onNodeWithText("Build prompt").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Paste at least a few lines first."),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Paste at least a few lines first.").assertExists()
        assertEquals(null, builtId)
    }
}
