package com.handoff.app.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.handoff.app.R
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Role
import java.io.OutputStream

/** Layout is pure and unit-testable; rendering happens in [PdfExporter.write]. */
object PdfLayout {

    enum class LineKind { TITLE, META, ROLE, BODY, BULLET, HEADING, CODE }

    data class LineSpec(
        val text: String,
        val kind: LineKind,
        val codeGroup: Int = -1, // consecutive lines of the same code block share a group
    )

    fun buildLines(conversation: Conversation, messages: List<Message>): List<LineSpec> {
        val lines = mutableListOf<LineSpec>()
        lines.add(LineSpec(conversation.title.ifBlank { "Conversation" }, LineKind.TITLE))
        lines.add(
            LineSpec(
                "${conversation.platform.displayName} · ${messages.size} messages · exported by Handoff",
                LineKind.META,
            ),
        )
        lines.add(LineSpec("", LineKind.META))

        messages.forEachIndexed { index, message ->
            val role = when (message.role) {
                Role.USER -> "User"
                Role.ASSISTANT -> "Assistant"
                Role.SYSTEM -> "System"
            }
            lines.add(LineSpec("$role · message ${index + 1}", LineKind.ROLE))
            var codeGroup = 0
            var inCode = false
            message.blocks.forEach { block ->
                when (block) {
                    is com.handoff.app.core.model.Block.Text ->
                        block.text.trim().takeIf { it.isNotEmpty() }?.let {
                            it.split("\n").forEach { line -> lines.add(LineSpec(line, LineKind.BODY)) }
                        }
                    is com.handoff.app.core.model.Block.Heading ->
                        lines.add(LineSpec(block.text, LineKind.HEADING))
                    is com.handoff.app.core.model.Block.ListBlock ->
                        block.items.forEachIndexed { i, item ->
                            val marker = if (block.ordered) "${i + 1}." else "•"
                            lines.add(LineSpec("$marker  $item", LineKind.BULLET))
                        }
                    is com.handoff.app.core.model.Block.Code -> {
                        block.content.trimEnd('\n').split("\n").forEach { codeLine ->
                            if (!inCode) codeGroup++
                            inCode = true
                            lines.add(LineSpec(codeLine, LineKind.CODE, codeGroup))
                        }
                        inCode = false
                    }
                    is com.handoff.app.core.model.Block.Image ->
                        lines.add(LineSpec("[image: ${block.alt}]", LineKind.BULLET))
                }
            }
            lines.add(LineSpec("", LineKind.META))
        }
        return lines
    }
}

/**
 * Renders the transcript to a paginated PDF: A4, Inter typography, monospace
 * code blocks on a light background, "page / total" numbers in the footer.
 */
class PdfExporter(private val context: Context) {

    data class Metrics(
        val pageWidth: Int = 595,
        val pageHeight: Int = 842,
        val margin: Float = 48f,
    )

    fun write(conversation: Conversation, messages: List<Message>, output: OutputStream) {
        val specs = PdfLayout.buildLines(conversation, messages)
        val metrics = Metrics()
        val paints = Paints(ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.SANS_SERIF)

        val contentWidth = metrics.pageWidth - metrics.margin * 2
        val wrapped = wrap(specs, paints, contentWidth)

        val lineHeight = paints.body.fontSpacing
        val titleHeight = paints.title.fontSpacing
        val codeLineHeight = paints.code.fontSpacing
        val usableHeight = metrics.pageHeight - metrics.margin * 2

        // First pass: assign lines to pages to know the total.
        val pages = mutableListOf<MutableList<Pair<PdfLayout.LineSpec, Float>>>() // spec + advance
        var currentPage = mutableListOf<Pair<PdfLayout.LineSpec, Float>>()
        var y = 0f
        for ((spec, advance) in wrapped) {
            if (y + advance > usableHeight && currentPage.isNotEmpty()) {
                pages.add(currentPage)
                currentPage = mutableListOf()
                y = 0f
            }
            currentPage.add(spec to advance)
            y += advance
        }
        if (currentPage.isNotEmpty()) pages.add(currentPage)
        if (pages.isEmpty()) pages.add(mutableListOf())

        val document = PdfDocument()
        pages.forEachIndexed { pageIndex, pageLines ->
            val pageInfo = PdfDocument.PageInfo.Builder(metrics.pageWidth, metrics.pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var cursorY = metrics.margin

            // Draw code backgrounds first (behind text).
            drawCodeBackgrounds(canvas, pageLines, metrics, cursorY)

            for ((spec, advance) in pageLines) {
                when (spec.kind) {
                    PdfLayout.LineKind.TITLE -> drawText(canvas, spec.text, paints.title, metrics, cursorY + titleHeight)
                    PdfLayout.LineKind.META -> drawText(canvas, spec.text, paints.meta, metrics, cursorY + lineHeight)
                    PdfLayout.LineKind.ROLE -> {
                        if (spec.text.isNotEmpty()) {
                            drawText(canvas, spec.text.uppercase(), paints.role, metrics, cursorY + paints.role.fontSpacing)
                        }
                    }
                    PdfLayout.LineKind.HEADING -> drawText(canvas, spec.text, paints.heading, metrics, cursorY + paints.heading.fontSpacing)
                    PdfLayout.LineKind.BODY -> drawText(canvas, spec.text, paints.body, metrics, cursorY + lineHeight)
                    PdfLayout.LineKind.BULLET -> drawText(canvas, spec.text, paints.body, metrics, cursorY + lineHeight, indent = 14f)
                    PdfLayout.LineKind.CODE -> drawText(canvas, spec.text, paints.code, metrics, cursorY + codeLineHeight, indent = 8f)
                }
                cursorY += advance
            }

            // Footer page number.
            val label = "${pageIndex + 1} / ${pages.size}"
            val width = paints.meta.measureText(label)
            canvas.drawText(label, (metrics.pageWidth - width) / 2f, metrics.pageHeight - metrics.margin / 2f, paints.meta)

            document.finishPage(page)
        }

        output.use { stream ->
            document.writeTo(stream)
        }
        document.close()
    }

    private fun drawCodeBackgrounds(
        canvas: Canvas,
        pageLines: List<Pair<PdfLayout.LineSpec, Float>>,
        metrics: Metrics,
        topOffset: Float,
    ) {
        val codePaint = Paint().apply { color = android.graphics.Color.rgb(0xF3, 0xED, 0xE4) }
        var groupStart: Int? = null
        pageLines.forEachIndexed { index, (spec, _) ->
            if (spec.kind == PdfLayout.LineKind.CODE && groupStart == null) groupStart = index
            val next = pageLines.getOrNull(index + 1)
            val groupEnds = spec.kind == PdfLayout.LineKind.CODE &&
                (next == null || next.first.codeGroup != spec.codeGroup)
            val start = if (groupEnds) groupStart else null
            if (start != null) {
                val startY = topOffset + pageLines.subList(0, start).sumOf { it.second.toDouble() }.toFloat()
                val height = pageLines.subList(start, index + 1).sumOf { it.second.toDouble() }.toFloat()
                canvas.drawRect(
                    metrics.margin - 6f,
                    startY,
                    metrics.pageWidth - metrics.margin + 6f,
                    startY + height,
                    codePaint,
                )
                groupStart = null
            }
        }
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        metrics: Metrics,
        baseline: Float,
        indent: Float = 0f,
    ) {
        if (text.isEmpty()) return
        canvas.drawText(text, metrics.margin + indent, baseline, paint)
    }

    private fun wrap(
        specs: List<PdfLayout.LineSpec>,
        paints: Paints,
        contentWidth: Float,
    ): List<Pair<PdfLayout.LineSpec, Float>> {
        val out = mutableListOf<Pair<PdfLayout.LineSpec, Float>>()
        for (spec in specs) {
            val paint = when (spec.kind) {
                PdfLayout.LineKind.TITLE -> paints.title
                PdfLayout.LineKind.META -> paints.meta
                PdfLayout.LineKind.ROLE -> paints.role
                PdfLayout.LineKind.HEADING -> paints.heading
                PdfLayout.LineKind.CODE -> paints.code
                else -> paints.body
            }
            val advance = when (spec.kind) {
                PdfLayout.LineKind.TITLE -> paint.fontSpacing + 6f
                PdfLayout.LineKind.ROLE -> paint.fontSpacing + 6f
                PdfLayout.LineKind.HEADING -> paint.fontSpacing + 4f
                PdfLayout.LineKind.CODE -> paint.fontSpacing
                else -> paint.fontSpacing + 2f
            }
            if (spec.text.isEmpty()) {
                out.add(spec to advance)
                continue
            }
            if (spec.kind == PdfLayout.LineKind.CODE) {
                // Code wraps at character level so whitespace stays meaningful.
                var remaining = spec.text
                do {
                    val fits = paint.breakText(remaining, true, contentWidth - 8f, null)
                    if (fits == 0) break
                    out.add(spec.copy(text = remaining.substring(0, fits)) to advance)
                    remaining = remaining.substring(fits)
                } while (remaining.isNotEmpty())
            } else {
                var remaining = spec.text
                do {
                    val fits = paint.breakText(remaining, true, contentWidth, null)
                    if (fits == 0) break
                    out.add(spec.copy(text = remaining.substring(0, fits)) to advance)
                    remaining = remaining.substring(fits)
                } while (remaining.isNotEmpty())
            }
        }
        return out
    }

    private class Paints(baseTypeface: Typeface) {
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = 22f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(0x2B, 0x26, 0x23)
        }
        val meta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = 10f
            color = android.graphics.Color.rgb(0x7A, 0x6F, 0x66)
        }
        val role = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = 9f
            isFakeBoldText = true
            letterSpacing = 0.08f
            color = android.graphics.Color.rgb(0x6B, 0x8F, 0x7B)
        }
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(0x2B, 0x26, 0x23)
        }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = 11f
            color = android.graphics.Color.rgb(0x2B, 0x26, 0x23)
        }
        val code = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textSize = 9.5f
            color = android.graphics.Color.rgb(0x3A, 0x34, 0x2E)
        }
    }
}
