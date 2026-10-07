package com.handoff.app.core.extraction

/**
 * Typed extraction failures. Each carries a human message and a suggested
 * action so the UI can offer exactly one clear next step.
 */
sealed class ExtractionError(
    message: String,
    val userMessage: String,
    val suggestedAction: ExtractionAction,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Couldn't reach the host at all (airplane mode, DNS, TLS). */
    class NetworkError(detail: String, cause: Throwable? = null) : ExtractionError(
        message = "network: $detail",
        userMessage = "Couldn't reach the site. Check your connection.",
        suggestedAction = ExtractionAction.RETRY,
        cause = cause,
    )

    /** Link returned 404 / gone — the share was deleted or mistyped. */
    class NotFound(detail: String) : ExtractionError(
        message = "not_found: $detail",
        userMessage = "That link doesn't exist (or the chat was deleted).",
        suggestedAction = ExtractionAction.CHECK_LINK,
    )

    /** Login wall, region block or anti-bot interstitial. */
    class Private(detail: String) : ExtractionError(
        message = "private: $detail",
        userMessage = "This conversation is private. Open the link in your browser first, then paste the chat manually.",
        suggestedAction = ExtractionAction.PASTE_MANUALLY,
    )

    /** Reached the page but couldn't find a conversation in it. */
    class ParseFailed(detail: String) : ExtractionError(
        message = "parse: $detail",
        userMessage = "We read the page but couldn't find the chat. The site may have changed.",
        suggestedAction = ExtractionAction.PASTE_MANUALLY,
    )

    /** Timed out waiting for content (slow network or unresponsive page). */
    class Timeout(detail: String) : ExtractionError(
        message = "timeout: $detail",
        userMessage = "The page took too long to load. Try again.",
        suggestedAction = ExtractionAction.RETRY,
    )

    /** Not a supported share URL. */
    class UnsupportedLink(detail: String) : ExtractionError(
        message = "unsupported: $detail",
        userMessage = "This isn't a Claude, ChatGPT or Gemini share link.",
        suggestedAction = ExtractionAction.CHECK_LINK,
    )
}

enum class ExtractionAction {
    RETRY,
    CHECK_LINK,
    PASTE_MANUALLY,
    OPEN_IN_BROWSER,
}
