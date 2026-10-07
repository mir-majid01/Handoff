package com.handoff.app.util

import java.util.Locale

/** Formats token counts for the sticky headers: 12, 1.4k, 2.3M. */
fun formatTokens(tokens: Int): String = when {
    tokens >= 1_000_000 -> String.format(Locale.US, "%.1fM", tokens / 1_000_000f)
    tokens >= 1_000 -> String.format(Locale.US, "%.1fk", tokens / 1_000f)
    else -> tokens.toString()
}
