package com.handoff.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.handoff.app.HandoffApp

/**
 * True when decorative motion should be skipped: the user turned animations off
 * (animator scale 0), we're in a Compose preview, or we're running under an
 * automated test harness (Robolectric). Tests must see this true so perpetual
 * animations can't hang Compose's waitForIdle.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return true
    val context = LocalContext.current
    return remember(context) {
        val animatorOff = runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
        animatorOff || runningInTestHarness()
    }
}

private fun runningInTestHarness(): Boolean = runCatching {
    // Any of these live only on the unit-test classpath; a production APK never sees them.
    val loader = HandoffApp::class.java.classLoader ?: return false
    listOf(
        "org/robolectric/Robolectric.class",
        "org/junit/Test.class",
        "androidx/compose/ui/test/ComposeContentTestRule.class",
    ).any { loader.getResource(it) != null }
}.getOrDefault(false)
