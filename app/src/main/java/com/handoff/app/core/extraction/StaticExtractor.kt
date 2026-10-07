package com.handoff.app.core.extraction

import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Strategy 1: plain HTTP fetch + embedded-JSON extraction. Fast, invisible,
 * works whenever the page ships its conversation in the HTML (most share pages do).
 */
class StaticExtractor(
    private val client: OkHttpClient = defaultClient(),
) {

    suspend fun fetchHtml(url: String, staticConfig: StaticConfig): String {
        var lastError: ExtractionError = ExtractionError.ParseFailed("No attempt made")
        repeat(staticConfig.retries.coerceAtLeast(1)) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0 Mobile Safari/537.36",
                    )
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code in 200..299 -> {
                            val body = response.body?.string().orEmpty()
                            if (body.isBlank()) {
                                lastError = ExtractionError.ParseFailed("Empty response body")
                            } else {
                                return body
                            }
                        }
                        response.code == 404 || response.code == 410 ->
                            throw ExtractionError.NotFound("HTTP ${response.code}")
                        response.code == 401 || response.code == 403 || response.code == 429 ->
                            throw ExtractionError.Private("HTTP ${response.code}")
                        response.code >= 500 -> {
                            lastError = ExtractionError.NetworkError("HTTP ${response.code}")
                        }
                        else -> lastError = ExtractionError.ParseFailed("HTTP ${response.code}")
                    }
                }
            } catch (e: ExtractionError) {
                throw e
            } catch (e: IOException) {
                lastError = ExtractionError.NetworkError(e.message ?: "IO error", e)
            }
            // Simple backoff before retrying.
            if (attempt < staticConfig.retries - 1) {
                kotlinx.coroutines.delay(800L * (attempt + 1))
            }
        }
        throw lastError
    }

    /**
     * Pure function over HTML so tests can pass fixtures directly.
     * Returns null when no candidate script yields a conversation.
     */
    fun parseHtml(html: String, platform: Platform, staticConfig: StaticConfig): Conversation? {
        val document = Jsoup.parse(html, "")
        val title = guessTitle(document)
        for (candidate in staticConfig.scripts) {
            val scriptText = findScriptText(document, candidate) ?: continue
            val element = runCatching { Json.parseToJsonElement(scriptText) }.getOrNull() ?: continue
            val messages = extractMessages(element, candidate) ?: continue
            if (messages.isNotEmpty()) {
                return Conversation(
                    platform = platform,
                    title = title,
                    url = "",
                    messages = messages,
                )
            }
        }
        // Streamed RSC recovery (Next.js app-router pages ship state in __next_f chunks).
        if (staticConfig.tryStreamedRsc) {
            val payload = streamedRscPayload(html)
            val messages = payload?.let { deepSearchStream(it) }
            if (!messages.isNullOrEmpty()) {
                return Conversation(
                    platform = platform,
                    title = title,
                    url = "",
                    messages = messages,
                )
            }
        }
        return null
    }

    /**
     * Concatenates the string chunks pushed via self.__next_f.push([1,"..."])
     * into one payload of JSON fragments.
     */
    internal fun streamedRscPayload(html: String): String? {
        if (!html.contains("self.__next_f")) return null
        val regex = Regex("""self\.__next_f\.push\(\[1,\s*"((?:[^"\\]|\\.)*)"\]\)""")
        val builder = StringBuilder()
        for (match in regex.findAll(html)) {
            val literal = match.groupValues[1]
            val unescaped = runCatching {
                Json.parseToJsonElement("\"$literal\"").jsonPrimitive.content
            }.getOrNull() ?: continue
            builder.append(unescaped)
        }
        return builder.toString().takeIf { it.length > 100 }
    }

    /**
     * Scans a payload for balanced JSON objects and deep-searches each one.
     * Caps attempts so a pathological page can't burn the CPU.
     */
    internal fun deepSearchStream(payload: String): List<Message>? {
        var pos = 0
        var attempts = 0
        while (pos < payload.length - 1 && attempts < 60) {
            val start = payload.indexOf('{', pos)
            if (start == -1) break
            val end = balancedEnd(payload, start)
            attempts++
            if (end == null) {
                pos = start + 1
                continue
            }
            pos = end + 1
            val element = runCatching {
                Json.parseToJsonElement(payload.substring(start, end + 1))
            }.getOrNull() ?: continue
            val messages = JsonMessageParser.deepSearch(element)
            if (!messages.isNullOrEmpty()) return messages
        }
        return null
    }

    /** Index of the brace/bracket that closes the structure opened at [start]. */
    private fun balancedEnd(payload: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until payload.length) {
            val char = payload[index]
            if (escaped) {
                escaped = false
                continue
            }
            when {
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                !inString && (char == '{' || char == '[') -> depth++
                !inString && (char == '}' || char == ']') -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    internal fun extractMessages(element: JsonElement, candidate: ScriptCandidate): List<Message>? {
        // Explicit paths first.
        for (path in candidate.paths) {
            val node = navigate(element, path)
            if (node != null) {
                val messages = parseNode(node, candidate.parser)
                if (!messages.isNullOrEmpty()) return messages
            }
        }
        // Then the parser's own search.
        return parseNode(element, candidate.parser)
    }

    private fun parseNode(node: JsonElement, parser: String): List<Message>? =
        JsonMessageParser.parse(node, parser)

    private fun navigate(root: JsonElement, path: List<String>): JsonElement? {
        var cursor: JsonElement = root
        for (key in path) {
            cursor = when (cursor) {
                is JsonObject -> cursor[key] ?: return null
                else -> return null
            }
        }
        return cursor
    }

    internal fun findScriptText(document: Document, candidate: ScriptCandidate): String? {
        val elements: List<Element> = when {
            candidate.id != null -> listOfNotNull(document.getElementById(candidate.id))
            candidate.attrName != null && candidate.attrValue != null ->
                document.select("script[${candidate.attrName}=\"${candidate.attrValue}\"]").toList()
            else -> document.select("script").toList()
        }
        for (element in elements) {
            val text = element.data().takeIf { it.isNotBlank() } ?: element.html()
            if (text.length > 2) return text
        }
        return null
    }

    private fun guessTitle(document: Document): String =
        document.title().trim().ifEmpty { "Shared conversation" }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
