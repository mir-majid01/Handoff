package com.handoff.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handoff.app.HandoffApp
import com.handoff.app.ui.navigation.HandoffNavHost
import com.handoff.app.ui.theme.HandoffTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regression test for the reported bug: Skip / Get started on onboarding left
 * the app stuck on the intro pages because the flag was never persisted.
 *
 * Single method by design: Robolectric reuses the JVM and the static
 * DataStore delegate across methods of this class, so multi-method flows
 * here would contaminate each other. This walks Get started → Home and
 * asserts the flag is persisted for the next launch.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = HandoffApp::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_getStarted_landsOnHome_andPersistsFlag() {
        val app = ApplicationProvider.getApplicationContext<HandoffApp>()

        // Start from a clean slate regardless of store state.
        runTest { app.settingsStore.setOnboardingDone(false) }

        composeRule.setContent {
            HandoffTheme { HandoffNavHost(app = app) }
        }

        // Onboarding is the start destination; page 1 shows the card and both controls.
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasText("Paste a link")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Skip").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("We read it")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Next").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Get started")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Get started").performClick()

        // Landing screen is Home; onboarding must not be reachable again.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Paste a share link to begin."))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Skip").assertDoesNotExist()

        runTest {
            assertTrue(app.settingsStore.settings.first().onboardingDone)
        }
    }
}
