package com.handoff.app.util

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Central haptics. Respects the user's "haptics" toggle at call sites by
 * going through [com.handoff.app.ui.AppViewModel]-provided helpers, or direct
 * view calls where a setting is not reachable.
 */
object Haptics {

    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun reject(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
