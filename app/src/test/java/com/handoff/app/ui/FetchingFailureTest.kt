package com.handoff.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handoff.app.HandoffApp
import com.handoff.app.ui.screens.fetching.FetchingScreen
import com.handoff.app.ui.theme.HandoffTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Real failure scenario end to end at the UI level: a link that is not a
 * supported share URL goes through detect → typed UnsupportedLink error →
 * the error card with its suggested action. (HTTP-level failure mapping —
 * 404/403/5xx → typed errors — is covered in StaticExtractorTest with
 * MockWebServer, which is deterministic; real-network Robolectric tests are
 * not, so this test uses the fast deterministic path.)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = HandoffApp::class)
class FetchingFailureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unsupportedLink_landsOnErrorCardWithSuggestedAction() {
        val app = ApplicationProvider.getApplicationContext<HandoffApp>()
        composeRule.setContent {
            HandoffTheme {
                FetchingScreen(
                    url = "https://example.com/definitely-not-a-share",
                    onSuccess = {},
                    onPasteManually = {},
                    onCancel = {},
                    app = app,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Couldn't read that chat"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Couldn't read that chat").assertExists()
        composeRule.onNodeWithText("Back").assertExists()
    }
}
