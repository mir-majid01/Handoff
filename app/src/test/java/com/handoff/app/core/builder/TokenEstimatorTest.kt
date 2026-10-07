package com.handoff.app.core.builder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenEstimatorTest {

    @Test
    fun `chars divided by four`() {
        assertEquals(0, TokenEstimator.estimateTokens(""))
        assertEquals(1, TokenEstimator.estimateTokens("abc"))
        assertEquals(2, TokenEstimator.estimateTokens("abcde"))
        assertEquals(200, TokenEstimator.estimateTokens("abcdefgh".repeat(100)))
    }

    @Test
    fun `char round trip is monotonic`() {
        val chars = TokenEstimator.estimateChars(1_000)
        assertEquals(1_000, TokenEstimator.estimateTokens("x".repeat(chars)))
    }
}
