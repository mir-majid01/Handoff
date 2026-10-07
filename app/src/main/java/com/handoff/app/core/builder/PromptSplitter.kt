package com.handoff.app.core.builder

/**
 * Splits rendered content units into parts that each fit a character budget
 * (token limit × 4, minus wrapper overhead). Never splits mid-unit unless a
 * single unit alone exceeds the budget, in which case it hard-splits on
 * paragraph then line boundaries so code blocks stay intact where possible.
 */
object PromptSplitter {

    /** Fallback wrapper overhead in chars, subtracted from the budget per part. */
    const val WRAPPER_OVERHEAD_CHARS = 1_200

    fun splitUnits(
        units: List<String>,
        maxCharsPerPart: Int,
        separator: String = "\n\n",
    ): List<String> {
        if (units.isEmpty()) return listOf("")
        val budget = maxCharsPerPart.coerceAtLeast(1)
        val sepChars = separator.length

        val parts = mutableListOf<StringBuilder>()
        var current = StringBuilder()
        var currentSize = 0

        fun newPart() {
            if (current.isNotBlank()) parts.add(current)
            current = StringBuilder()
            currentSize = 0
        }

        for (unit in units) {
            val unitSize = unit.length
            when {
                unitSize > budget -> {
                    // Flush what we have, then hard-split the oversized unit.
                    newPart()
                    hardSplit(unit, budget, separator).forEach { piece ->
                        if (currentSize > 0 && currentSize + sepChars + piece.length > budget) {
                            newPart()
                        }
                        if (currentSize > 0) {
                            current.append(separator)
                            currentSize += sepChars
                        }
                        current.append(piece)
                        currentSize += piece.length
                    }
                    // If the last piece exactly filled the part, start fresh for the next unit.
                    if (currentSize >= budget) newPart()
                }
                currentSize > 0 && currentSize + sepChars + unitSize > budget -> {
                    newPart()
                    current.append(unit)
                    currentSize = unitSize
                }
                else -> {
                    if (currentSize > 0) {
                        current.append(separator)
                        currentSize += sepChars
                    }
                    current.append(unit)
                    currentSize += unitSize
                }
            }
        }
        if (current.isNotBlank()) parts.add(current)
        return parts.map { it.toString() }.ifEmpty { listOf("") }
    }

    /** Hard-split one oversized unit on paragraph boundaries, then line boundaries. */
    private fun hardSplit(unit: String, budget: Int, separator: String): List<String> {
        val paragraphs = unit.split(Regex("\n\\s*\n"))
        if (paragraphs.size > 1) {
            val pieces = mutableListOf<String>()
            var acc = StringBuilder()
            for (paragraph in paragraphs) {
                if (acc.isNotEmpty() && acc.length + separator.length + paragraph.length > budget) {
                    pieces.add(acc.toString())
                    acc = StringBuilder()
                }
                if (paragraph.length > budget) {
                    if (acc.isNotBlank()) {
                        pieces.add(acc.toString())
                        acc = StringBuilder()
                    }
                    pieces.addAll(splitByLines(paragraph, budget, separator))
                } else {
                    if (acc.isNotEmpty()) acc.append(separator)
                    acc.append(paragraph)
                }
            }
            if (acc.isNotBlank()) pieces.add(acc.toString())
            return pieces
        }
        return splitByLines(unit, budget, separator)
    }

    private fun splitByLines(unit: String, budget: Int, separator: String): List<String> {
        val pieces = mutableListOf<String>()
        var acc = StringBuilder()
        for (line in unit.lineSequence()) {
            val candidate = if (acc.isEmpty()) line else acc.toString() + "\n" + line
            if (candidate.length > budget && acc.isNotEmpty()) {
                pieces.add(acc.toString())
                acc = StringBuilder(line)
            } else {
                if (acc.isNotEmpty()) acc.append("\n")
                acc.append(line)
            }
            // A single pathological line longer than the budget gets chunked raw.
            while (acc.length > budget) {
                pieces.add(acc.substring(0, budget))
                acc = StringBuilder(acc.substring(budget))
            }
        }
        if (acc.isNotBlank()) pieces.add(acc.toString())
        return pieces
    }
}
