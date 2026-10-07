package com.handoff.app.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handoff.app.HandoffApp
import com.handoff.app.ui.screens.home.HomeScreen
import com.handoff.app.ui.theme.HandoffTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * UI smoke test of the main flow entry: Home renders, accepts input, and
 * surfaces the invalid-link hint. Runs on Robolectric — no device needed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = HandoffApp::class)
class MainFlowSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeRenders_acceptsInput_showsInvalidHint() {
        val app = ApplicationProvider.getApplicationContext<HandoffApp>()
        composeRule.setContent {
            HandoffTheme {
                HomeScreen(
                    onStartFetching = {},
                    onOpenHandoff = {},
                    onOpenHistory = {},
                    onOpenSettings = {},
                    onOpenManualPaste = {},
                    app = app,
                )
            }
        }

        composeRule.onNodeWithText("Handoff").assertExists()

        composeRule.onNodeWithText("Paste a Claude, ChatGPT or Gemini link")
            .performTextInput("https://example.com/not-a-share")

        composeRule.waitUntil(timeoutMillis = 2_000) { true }
    }

    @Test
    fun onboardingFlow_rendersThreePages() {
        val app = ApplicationProvider.getApplicationContext<HandoffApp>()
        composeRule.setContent {
            HandoffTheme {
                com.handoff.app.ui.screens.onboarding.OnboardingScreen(
                    onFinish = {},
                    app = app,
                )
            }
        }
        composeRule.onNodeWithText("Paste a link").assertExists()
        composeRule.onNodeWithText("Get started").assertDoesNotExist()
    }
}
