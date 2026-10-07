package com.handoff.app.core.extraction

import android.content.Context
import com.handoff.app.core.model.Conversation

/** Stages surfaced to the Fetching screen, in display order. */
enum class FetchStage(val label: String) {
    CONNECTING("Connecting"),
    READING("Reading messages"),
    CLEANING("Cleaning"),
    BUILDING("Building prompt"),
}

/**
 * Orchestrates the fallback chain:
 *   1. static fetch + embedded JSON (fast path)
 *   2. hidden WebView + injected JS (rendered pages)
 *   3. generic heuristic over the fetched HTML
 * The best error seen so far wins when everything fails.
 */
class ExtractionEngine(
    private val context: Context,
    private val config: ExtractorsConfig,
    private val staticExtractor: StaticExtractor = StaticExtractor(),
    private val webviewExtractor: WebViewExtractor = WebViewExtractor(),
    private val heuristicExtractor: GenericHeuristicExtractor = GenericHeuristicExtractor(),
) {

    suspend fun extract(
        rawUrl: String,
        onStage: suspend (FetchStage) -> Unit = {},
    ): Conversation {
        val detected = LinkDetector.detect(rawUrl, config)
        val (platform, platformConfig) = when (detected) {
            is LinkDetector.Result.Detected -> detected.platform to detected.config
            is LinkDetector.Result.Unknown ->
                throw ExtractionError.UnsupportedLink(detected.detail)
        }
        val url = LinkDetector.normalize(rawUrl) ?: rawUrl

        var html: String? = null
        val errors = mutableListOf<ExtractionError>()

        // 1. Static fetch + embedded JSON.
        val staticConfig = platformConfig.static
        if (staticConfig != null) {
            onStage(FetchStage.CONNECTING)
            html = try {
                staticExtractor.fetchHtml(url, staticConfig)
            } catch (error: ExtractionError) {
                errors.add(error)
                null
            }
            if (html != null) {
                onStage(FetchStage.READING)
                val parsed = staticExtractor.parseHtml(html, platform, staticConfig)
                if (parsed != null) {
                    onStage(FetchStage.CLEANING)
                    onStage(FetchStage.BUILDING)
                    return parsed.copy(url = url)
                }
                errors.add(ExtractionError.ParseFailed("No embedded conversation JSON found"))
            }
        }

        // 2. Hidden WebView.
        val webViewConfig = platformConfig.webview
        if (webViewConfig != null) {
            onStage(FetchStage.CONNECTING)
            try {
                onStage(FetchStage.READING)
                val conversation = webviewExtractor.extract(context, url, platform, webViewConfig)
                onStage(FetchStage.CLEANING)
                onStage(FetchStage.BUILDING)
                return conversation
            } catch (error: ExtractionError) {
                errors.add(error)
            } catch (t: Throwable) {
                // A broken/absent WebView must degrade to the next strategy, never crash.
                errors.add(ExtractionError.ParseFailed("WebView extractor failed: ${t.message}"))
            }
        }

        // 3. Heuristic over the static HTML we already have (if any).
        if (html != null) {
            val heuristic = runCatching { heuristicExtractor.extract(html, url) }.getOrNull()
            if (heuristic != null) {
                onStage(FetchStage.CLEANING)
                onStage(FetchStage.BUILDING)
                return heuristic.copy(platform = platform)
            }
        }

        throw pickWorst(errors)
    }

    private fun pickWorst(errors: List<ExtractionError>): ExtractionError {
        if (errors.isEmpty()) return ExtractionError.ParseFailed("No extraction strategy ran")
        val priority = mapOf(
            ExtractionError.NotFound::class to 0,
            ExtractionError.Private::class to 1,
            ExtractionError.NetworkError::class to 2,
            ExtractionError.Timeout::class to 3,
            ExtractionError.ParseFailed::class to 4,
            ExtractionError.UnsupportedLink::class to 5,
        )
        return errors.minByOrNull { priority[it::class] ?: 99 } ?: errors.first()
    }
}
