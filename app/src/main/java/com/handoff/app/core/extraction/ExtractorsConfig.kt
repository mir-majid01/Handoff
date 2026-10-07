package com.handoff.app.core.extraction

import kotlinx.serialization.Serializable

/**
 * All selectors, JSON paths and platform URL patterns live here (backed by
 * `assets/extractors_config.json`) so a site change is a config edit, not a
 * code change. See README "Updating extractor selectors".
 */
@Serializable
data class ExtractorsConfig(
    val version: Int = 1,
    val platforms: Map<String, PlatformConfig> = emptyMap(),
)

@Serializable
data class PlatformConfig(
    val displayName: String,
    /** Regexes (case-insensitive) that identify share URLs of this platform. */
    val urlPatterns: List<String> = emptyList(),
    /** Optional host aliases mapped to this platform (e.g. chat.openai.com). */
    val static: StaticConfig? = null,
    val webview: WebViewConfig? = null,
)

/** Strategy 1: fetch HTML over HTTP and pull the conversation out of embedded JSON. */
@Serializable
data class StaticConfig(
    /** Candidate <script> locations for embedded state, tried in order. */
    val scripts: List<ScriptCandidate> = emptyList(),
    /** Also scan streamed Next.js RSC payloads (self.__next_f) when scripts miss. */
    val tryStreamedRsc: Boolean = true,
    val timeoutMs: Int = 20_000,
    val retries: Int = 2,
)

@Serializable
data class ScriptCandidate(
    /** Match by element id, e.g. "__NEXT_DATA__". */
    val id: String? = null,
    /** Match by attribute, e.g. type="application/json". */
    val attrName: String? = null,
    val attrValue: String? = null,
    /** Explicit JSON paths to try first, e.g. ["props","pageProps","messages"]. */
    val paths: List<List<String>> = emptyList(),
    /** Named parser: "chatgpt_mapping", "message_array", or "deep_search" (default). */
    val parser: String = "deep_search",
)

/**
 * Strategy 2: hidden WebView + injected JS that returns messages as JSON.
 * All selectors below are relative to a rendered message container.
 */
@Serializable
data class WebViewConfig(
    /** Selector polled until it exists (page considered rendered). */
    val readySelector: String,
    /** Repeatable message container selector. */
    val messageSelector: String,
    /** Attribute carrying the role on the container, e.g. data-message-author-role. */
    val roleAttr: String? = null,
    /** Class-substring → role fallbacks, checked on the container itself. */
    val roleByClass: List<ClassRole> = emptyList(),
    /** Container-level role default when nothing matches (e.g. Gemini alternates). */
    val defaultRole: String? = null,
    /** Element holding the message body; falls back to container innerText. */
    val textSelector: String? = null,
    /** Scroll to bottom to force lazy content before extracting. */
    val scrollForLazy: Boolean = false,
    /** Wait for message count to stabilize this many polls before extracting. */
    val stabilityPolls: Int = 3,
    val pollIntervalMs: Int = 500,
    val maxWaitMs: Int = 25_000,
    /** Element whose innerText is the page title (null → document.title). */
    val titleSelector: String? = null,
)

@Serializable
data class ClassRole(val contains: String, val role: String)
