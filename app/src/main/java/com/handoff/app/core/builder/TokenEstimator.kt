package com.handoff.app.core.builder

/** Context-size estimation. Chars ÷ 4 per the brief — good enough for a live gauge. */
object TokenEstimator {

    const val CHARS_PER_TOKEN = 4

    fun estimateTokens(text: String): Int = (text.length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

    fun estimateChars(tokens: Int): Int = tokens * CHARS_PER_TOKEN
}
