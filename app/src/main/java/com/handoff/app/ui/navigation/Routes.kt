package com.handoff.app.ui.navigation

import android.net.Uri

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val MANUAL_PASTE = "manual-paste"
    const val SETTINGS = "settings"
    const val FETCHING = "fetching/{url}"
    const val PREVIEW = "preview/{handoffId}"
    const val EXPORT = "export/{handoffId}"

    fun fetching(url: String) = "fetching/${Uri.encode(url)}"
    fun preview(handoffId: String) = "preview/${Uri.encode(handoffId)}"
    fun export(handoffId: String) = "export/${Uri.encode(handoffId)}"
}
