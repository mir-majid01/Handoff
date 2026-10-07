package com.handoff.app.core.extraction

import com.handoff.app.core.model.Platform

/** Identifies the platform from a pasted URL using config-driven regexes. */
object LinkDetector {

    sealed class Result {
        data class Detected(val platform: Platform, val config: PlatformConfig) : Result()
        data class Unknown(val detail: String) : Result()
    }

    fun detect(rawUrl: String, config: ExtractorsConfig): Result {
        val url = normalize(rawUrl)
            ?: return Result.Unknown("Empty or malformed link")

        for ((key, platformConfig) in config.platforms) {
            val matched = platformConfig.urlPatterns.any { pattern ->
                runCatching { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(url) }
                    .getOrDefault(false)
            }
            if (matched) {
                val platform = platformOf(key)
                return Result.Detected(platform, platformConfig)
            }
        }
        return Result.Unknown("No platform pattern matched $url")
    }

    /** Trims junk, keeps the first URL token from noisy pastes, adds a scheme when missing. */
    fun normalize(rawUrl: String): String? {
        val cleaned = rawUrl.trim()
            .trim('"', '\'', '<', '>')
        if (cleaned.isEmpty()) return null
        // Pastes often carry prose or several links; a single link is the first http(s) token.
        val token = cleaned.split(Regex("\\s+")).firstOrNull { it.contains("://") }
            ?: cleaned.takeIf { it.none(Char::isWhitespace) && it.contains('.') }
            ?: return null
        val candidate = token.trim('"', '\'', '<', '>', ',', ';', ')', '.')
        val withScheme = if (Regex("(?i)^[a-z][a-z0-9+.-]*://").containsMatchIn(candidate)) {
            candidate
        } else {
            "https://$candidate"
        }
        return stripTrackingParams(withScheme)
    }

    /** Share links carry the id in the path; marketing params (?og=, ?utm_*, ?sn…) are noise. */
    fun stripTrackingParams(url: String): String {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val base = url.substring(0, queryStart)
        val queryAndFragment = url.substring(queryStart + 1)
        val fragmentIndex = queryAndFragment.indexOf('#')
        val query = if (fragmentIndex >= 0) queryAndFragment.substring(0, fragmentIndex) else queryAndFragment
        val fragment = if (fragmentIndex >= 0) queryAndFragment.substring(fragmentIndex) else ""
        val kept = query.split('&')
            .filter { it.isNotBlank() }
            .filterNot { param ->
                val key = param.substringBefore('=').lowercase()
                key in TRACKING_PARAMS || key.startsWith("utm_")
            }
        return if (kept.isEmpty()) base + fragment else "$base?${kept.joinToString("&")}$fragment"
    }

    private val TRACKING_PARAMS = setOf(
        "og", "ogm", "ogimg", "sn", "ref", "refer", "referral", "source", "ispv",
        "trace_id", "cid", "abtk", "share_from", "from",
    )

    private fun platformOf(configKey: String): Platform = when (configKey.lowercase()) {
        "claude" -> Platform.CLAUDE
        "chatgpt" -> Platform.CHATGPT
        "gemini" -> Platform.GEMINI
        "kimi" -> Platform.KIMI
        "deepseek" -> Platform.DEEPSEEK
        "qwen" -> Platform.QWEN
        else -> Platform.GENERIC
    }
}
