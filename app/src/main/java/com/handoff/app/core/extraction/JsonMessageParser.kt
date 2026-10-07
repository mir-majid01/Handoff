package com.handoff.app.core.extraction

import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Role
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Turns embedded page JSON into normalized messages. Three strategies:
 *  - "message_array": an array of {role|sender, content} objects.
 *  - "chatgpt_mapping": the ChatGPT share "mapping" tree walked from current_node.
 *  - "deep_search": DFS that finds either of the above anywhere in the JSON —
 *    the resilient default when a site moves its state around.
 */
object JsonMessageParser {

    /** Returns parsed messages or null if nothing conversation-shaped was found. */
    fun parse(root: JsonElement, parser: String): List<Message>? = when (parser) {
        "chatgpt_mapping" -> findMapping(root)?.let { fromMapping(it) }
        "message_array" -> findMessageArray(root)?.let { fromMessageArray(it) }
        else -> deepSearch(root)
    }

    // ---- Deep search -------------------------------------------------------

    fun deepSearch(root: JsonElement): List<Message>? {
        val seen = HashSet<JsonElement>()
        var best: List<Message>? = null

        fun visit(element: JsonElement) {
            if (best != null) return
            if (!seen.add(element)) return
            when (element) {
                is JsonObject -> {
                    if (element.containsKey("mapping")) {
                        fromMapping(element)?.let { if (it.size >= 2) { best = it; return } }
                    }
                    if (element.containsKey("chat_messages")) {
                        element["chat_messages"]?.let { arr ->
                            if (arr is JsonArray) {
                                fromMessageArray(arr)?.let { if (it.isNotEmpty()) { best = it; return } }
                            }
                        }
                    }
                    for ((_, value) in element) visit(value)
                }
                is JsonArray -> {
                    fromMessageArray(element)?.let { if (it.size >= 2) { best = it; return } }
                    element.forEach { visit(it) }
                }
                else -> Unit
            }
        }
        visit(root)
        return best
    }

    // ---- message_array -----------------------------------------------------

    fun findMessageArray(root: JsonElement): JsonArray? {
        var best: JsonArray? = null
        var bestScore = 0

        fun scoreArray(arr: JsonArray): Int {
            if (arr.isEmpty()) return 0
            var hits = 0
            for (item in arr) {
                if (item !is JsonObject) continue
                val hasRole = (item.containsKey("role") || item.containsKey("sender") ||
                    item.containsKey("author"))
                val hasContent = (item.containsKey("content") || item.containsKey("parts") ||
                    item.containsKey("text") || item.containsKey("message"))
                if (hasRole && hasContent) hits++
            }
            return hits
        }

        fun visit(element: JsonElement) {
            when (element) {
                is JsonObject -> element.values.forEach { visit(it) }
                is JsonArray -> {
                    val score = scoreArray(element)
                    if (score > bestScore) {
                        bestScore = score
                        best = element
                    }
                    element.forEach { visit(it) }
                }
                else -> Unit
            }
        }
        visit(root)
        return if (bestScore >= 2) best else null
    }

    fun fromMessageArray(array: JsonArray): List<Message> {
        val messages = mutableListOf<Message>()
        for (item in array) {
            if (item !is JsonObject) continue
            val role = roleOf(item) ?: continue
            val blocks = contentBlocks(item["content"] ?: item["parts"] ?: item["text"])
            if (blocks.isEmpty()) continue
            messages.add(Message(role = role, blocks = blocks))
        }
        return messages
    }

    private fun roleOf(item: JsonObject): Role? {
        val raw = (item["role"] ?: item["sender"])?.asSafeString() ?: return when {
            item.containsKey("author") -> authorRole(item["author"])
            else -> null
        }
        return normalizeRole(raw) ?: when {
            item.containsKey("author") -> authorRole(item["author"])
            else -> null
        }
    }

    private fun authorRole(author: JsonElement?): Role? {
        val obj = author as? JsonObject ?: return null
        val raw = obj["role"]?.asSafeString() ?: return null
        return normalizeRole(raw)
    }

    fun normalizeRole(raw: String): Role? = when (raw.lowercase().trim()) {
        "user", "human" -> Role.USER
        "assistant", "model", "ai" -> Role.ASSISTANT
        "system" -> Role.SYSTEM
        else -> null
    }

    // ---- content → blocks ----------------------------------------------------

    fun contentBlocks(element: JsonElement?): List<Block> {
        val blocks = mutableListOf<Block>()
        when (element) {
            null, is JsonNull -> Unit
            is JsonPrimitive -> {
                val text = element.content.trim()
                if (text.isNotEmpty()) blocks.add(Block.Text(text))
            }
            is JsonArray -> element.forEach { blocks.addAll(contentBlocks(it)) }
            is JsonObject -> {
                val type = (element["type"] ?: element["content_type"] ?: element["mimeType"])
                    ?.asSafeString()
                    ?.lowercase()
                val text = element["text"]?.asSafeString()?.trim().orEmpty()
                when {
                    type == null && text.isNotEmpty() -> blocks.add(Block.Text(text))
                    type in setOf("text", "output_text", "markdown") -> {
                        if (text.isNotEmpty()) {
                            blocks.add(Block.Text(text))
                        } else {
                            element["parts"]?.let { blocks.addAll(contentBlocks(it)) }
                        }
                    }
                    type in setOf("code", "code_block") -> {
                        val lang = element["language"]?.asSafeString().orEmpty()
                        val content = element["code"]?.asSafeString()
                            ?: element["content"]?.asSafeString()
                            ?: text
                        if (content.isNotBlank()) blocks.add(Block.Code(lang, content))
                    }
                    type in setOf("image", "image_url") -> blocks.add(
                        Block.Image(
                            alt = text.ifEmpty { "image" },
                            url = element["url"]?.asSafeString() ?: element["image_url"]?.asSafeString(),
                        ),
                    )
                    type in setOf("tool_use", "tool_result", "function_call", "function") -> {
                        // Tool chatter is UI plumbing; keep only textual payload if present.
                        if (text.isNotEmpty()) blocks.add(Block.Text(text))
                        element["content"]?.let { blocks.addAll(contentBlocks(it)) }
                    }
                    element.containsKey("parts") -> blocks.addAll(contentBlocks(element["parts"]))
                    element.containsKey("content") -> blocks.addAll(contentBlocks(element["content"]))
                    text.isNotEmpty() -> blocks.add(Block.Text(text))
                }
            }
        }
        return blocks
    }

    // ---- chatgpt_mapping -----------------------------------------------------

    /**
     * ChatGPT shares embed a node tree: {mapping: {id: {message, parent, children}},
     * current_node}. Walk parent links from current_node, then reverse.
     */
    fun findMapping(root: JsonElement): JsonObject? {
        var found: JsonObject? = null

        fun visit(element: JsonElement) {
            if (found != null) return
            when (element) {
                is JsonObject -> {
                    val mapping = element["mapping"]
                    if (mapping is JsonObject && mapping.isNotEmpty()) {
                        found = mapping
                        return
                    }
                    element.values.forEach { visit(it) }
                }
                is JsonArray -> element.forEach { visit(it) }
                else -> Unit
            }
        }
        visit(root)
        return found
    }

    fun fromMapping(mappingRoot: JsonObject): List<Message> {
        val mapping = mappingRoot["mapping"] as? JsonObject ?: mappingRoot
        if (mapping.isEmpty()) return emptyList()

        val currentNode = mappingRoot["current_node"]?.asSafeString()
        var cursor: String? = currentNode ?: mapping.keys.firstOrNull { mapping[it] is JsonObject &&
            ((mapping[it] as JsonObject)["children"] as? JsonArray)?.isEmpty() == true }

        if (cursor == null) {
            // Fallback: pick the last node id that has no children.
            for ((id, node) in mapping) {
                val children = (node as? JsonObject)?.get("children") as? JsonArray
                if (children.isNullOrEmpty()) cursor = id
            }
        }
        val chain = ArrayList<JsonObject>()
        var guard = 0
        while (cursor != null && guard++ < 10_000) {
            val node = mapping[cursor] as? JsonObject ?: break
            chain.add(node)
            cursor = node["parent"]?.asSafeString()
        }
        chain.reverse()

        var messages = messagesFromChain(chain)
        if (messages.size < 2) {
            // Fallback when parent links are missing: keep JSON insertion order.
            messages = mapping.values
                .mapNotNull { it as? JsonObject }
                .mapNotNull { node ->
                    val message = node["message"] as? JsonObject ?: return@mapNotNull null
                    val role = (message["author"] as? JsonObject)?.get("role")
                        ?.asSafeString()?.let { normalizeRole(it) } ?: return@mapNotNull null
                    val content = message["content"] as? JsonObject ?: return@mapNotNull null
                    if (content["content_type"]?.asSafeString() in setOf("hidden", "system_error")) {
                        return@mapNotNull null
                    }
                    val blocks = contentBlocks(content)
                    if (blocks.isEmpty()) null else Message(role = role, blocks = blocks)
                }
        }
        return messages
    }

    private fun messagesFromChain(chain: List<JsonObject>): List<Message> {
        val messages = mutableListOf<Message>()
        for (node in chain) {
            val message = node["message"] as? JsonObject ?: continue
            val author = message["author"] as? JsonObject
            val rawRole = author?.get("role")?.asSafeString() ?: continue
            val role = normalizeRole(rawRole) ?: continue
            val content = message["content"] as? JsonObject ?: continue
            val contentType = content["content_type"]?.asSafeString()
            if (contentType == "hidden" || contentType == "system_error") continue
            val blocks = contentBlocks(content)
            if (blocks.isEmpty()) continue
            messages.add(Message(role = role, blocks = blocks))
        }
        return messages
    }

    private fun JsonElement?.asSafeString(): String? = when (this) {
        null, is JsonNull -> null
        is JsonPrimitive -> content
        else -> null
    }
}
