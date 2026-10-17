package com.example.pocketbill.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.pocketbill.domain.invoice.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** PDF colours (UX_SPEC 8.1): fixed, so PDFs are always white paper and print well whatever the app theme. */
object PdfColors {
    const val INK = 0xFF1B222B.toInt()
    const val MUTED = 0xFF5F6B78.toInt()
    const val RULE = 0xFFDFE4EA.toInt()
    const val ACCENT = 0xFF1F4E79.toInt()
    const val STAMP_PAID = 0xFF1A7F5A.toInt()
    const val STAMP_VOID = 0xFF5F6B78.toInt()
}

/**
 * Draws an [InvoiceDocument] with Android's PdfDocument (MVP_SPEC 6): business block, title and
 * number, Bill to, dates, the items table (continued across pages with its header repeated),
 * totals, payment details, notes, "Page 1 of N", and the DRAFT / PAID / VOID marks.
 * Units are PDF points (1/72 inch). Uses the system font (Roboto) with Android's fallback chain.
 */
class InvoicePdfRenderer(private val outputDir: File) {

    /** Renders off the main thread and returns the file (in [outputDir], named [InvoiceDocument.fileName]). */
    suspend fun render(doc: InvoiceDocument): File = withContext(Dispatchers.Default) {
        outputDir.mkdirs()
        // Only the latest PDF is kept in the cache; earlier ones are regenerated when needed.
        outputDir.listFiles()?.forEach { if (it.name != doc.fileName) it.delete() }
        val file = File(outputDir, doc.fileName)
        val partial = File(outputDir, doc.fileName + ".part")
        val pdf = PdfDocument()
        try {
            Drawing(doc).drawAll(pdf)
            partial.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        if (!partial.renameTo(file)) {
            partial.copyTo(file, overwrite = true)
            partial.delete()
        }
        file
    }

    /** The layout of one document. */
    private class Drawing(private val doc: InvoiceDocument) {
        private val pageWidth = if (doc.paperSize == PaperSize.LETTER) 612 else 595
        private val pageHeight = if (doc.paperSize == PaperSize.LETTER) 792 else 842
        private val left = MARGIN
        private val right = pageWidth - MARGIN
        private val contentWidth = right - left
        private val bottom = pageHeight - MARGIN - FOOTER_SPACE

        // Table columns, right to left.
        private val amountRight = right
        private val unitRight = amountRight - AMOUNT_WIDTH
        private val qtyRight = unitRight - UNIT_WIDTH
        private val descriptionWidth = (qtyRight - QTY_WIDTH - left - CELL_GAP).toInt()

        private val body = paint(BODY_SIZE)
        private val bodyNumbers = paint(BODY_SIZE, numbers = true)
        private val muted = paint(SMALL_SIZE, color = PdfColors.MUTED)

        // Paints used by StaticLayouts stay left-aligned: their alignment would shift every line.
        private val mutedText = paint(SMALL_SIZE, color = PdfColors.MUTED)
        private val label = paint(LABEL_SIZE, bold = true, color = PdfColors.MUTED).apply { letterSpacing = 0.06f }
        private val strong = paint(BODY_SIZE, bold = true)

        private val descriptionLayouts = doc.lines.map { layout(it.description.ifEmpty { "—" }, body, descriptionWidth) }
        private val rowHeights = descriptionLayouts.map { it.height + 2 * CELL_PADDING }
        private val headerHeight = LABEL_SIZE + 2 * CELL_PADDING + 2

        private val tails: List<Tail> = buildList {
            add(TotalsTail())
            if (doc.paymentInstructions.isNotEmpty()) add(TextTail("PAYMENT DETAILS", doc.paymentInstructions))
            if (doc.notes.isNotEmpty()) add(TextTail("NOTES", doc.notes))
        }

        fun drawAll(pdf: PdfDocument) {
            val firstTop = measureFirstPageHeader()
            val pages = PdfPagination.paginate(
                firstPageTop = firstTop,
                otherPagesTop = MARGIN + CONTINUED_SPACE,
                bottom = bottom,
                headerHeight = headerHeight,
                rowHeights = rowHeights,
                tailHeights = tails.map { it.height },
                tailGap = TAIL_GAP,
            )
            pages.forEachIndexed { index, placed ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                val canvas = page.canvas
                if (doc.mark == PdfMark.DRAFT) drawWatermark(canvas)
                if (index == 0) drawFirstPageHeader(canvas) else drawContinuedHeader(canvas)
                placed.forEach { item ->
                    when (item) {
                        is Placed.TableHeader -> drawTableHeader(canvas, item.top)
                        is Placed.Row -> drawRow(canvas, item.index, item.top)
                        is Placed.Tail -> tails[item.index].draw(canvas, item.top)
                    }
                }
                drawFooter(canvas, index + 1, pages.size)
                if (index == 0 && (doc.mark == PdfMark.PAID || doc.mark == PdfMark.VOID)) drawStamp(canvas)
                pdf.finishPage(page)
            }
        }

        // ---- Page 1 header -----------------------------------------------------------------------

        private val nameLayout = layout(doc.fromName, paint(NAME_SIZE, bold = true), (contentWidth * 0.58f).toInt())
        private val fromLayout = layout(doc.fromLines.joinToString("\n"), mutedText, (contentWidth * 0.58f).toInt())
        private val title = paint(TITLE_SIZE, bold = true, color = PdfColors.ACCENT)
        private val numberPaint = paint(NUMBER_SIZE, bold = true)
        private val billToWidth = (contentWidth * 0.5f).toInt()
        private val toNameLayout = layout(doc.toName ?: "No client yet", strong, billToWidth)
        private val toLayout = layout(doc.toLines.joinToString("\n"), mutedText, billToWidth)
        private var billToTop = 0f

        private fun fromBlockHeight() = nameLayout.height + (if (doc.fromLines.isEmpty()) 0 else 4 + fromLayout.height)
        private fun titleBlockHeight() = TITLE_SIZE + 6 + NUMBER_SIZE + 4
        private fun billToHeight() = LABEL_SIZE + 6 + toNameLayout.height + (if (doc.toLines.isEmpty()) 0 else 3 + toLayout.height)
        private fun factsHeight() = doc.facts.size * FACT_LINE

        private fun measureFirstPageHeader(): Float {
            billToTop = MARGIN + maxOf(fromBlockHeight().toFloat(), titleBlockHeight()) + SECTION_GAP
            return billToTop + maxOf(billToHeight().toFloat(), factsHeight()) + SECTION_GAP
        }

        private fun drawFirstPageHeader(canvas: Canvas) {
            draw(canvas, nameLayout, left, MARGIN)
            if (doc.fromLines.isNotEmpty()) draw(canvas, fromLayout, left, MARGIN + nameLayout.height + 4)

            title.textAlign = Paint.Align.RIGHT
            canvas.drawText("INVOICE", right, MARGIN + TITLE_SIZE * 0.85f, title)
            numberPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(doc.number ?: "Draft", right, MARGIN + TITLE_SIZE + 6 + NUMBER_SIZE * 0.85f, numberPaint)

            canvas.drawText("BILL TO", left, billToTop + LABEL_SIZE * 0.85f, label)
            draw(canvas, toNameLayout, left, billToTop + LABEL_SIZE + 6)
            if (doc.toLines.isNotEmpty()) draw(canvas, toLayout, left, billToTop + LABEL_SIZE + 6 + toNameLayout.height + 3)

            val labelX = right - FACTS_WIDTH
            doc.facts.forEachIndexed { i, fact ->
                val baseline = billToTop + i * FACT_LINE + BODY_SIZE * 0.85f
                muted.textAlign = Paint.Align.LEFT
                canvas.drawText(fact.label, labelX, baseline, muted)
                bodyNumbers.textAlign = Paint.Align.RIGHT
                canvas.drawText(fact.value, right, baseline, bodyNumbers)
            }
        }

        private fun drawContinuedHeader(canvas: Canvas) {
            muted.textAlign = Paint.Align.LEFT
            val text = listOfNotNull(doc.number ?: "Draft", doc.fromName.ifBlank { null }).joinToString(" · ") + " (continued)"
            canvas.drawText(text, left, MARGIN + SMALL_SIZE * 0.85f, muted)
        }

        // ---- Items table --------------------------------------------------------------------------

        private fun drawTableHeader(canvas: Canvas, top: Float) {
            val baseline = top + CELL_PADDING + LABEL_SIZE * 0.85f
            label.textAlign = Paint.Align.LEFT
            canvas.drawText("DESCRIPTION", left, baseline, label)
            label.textAlign = Paint.Align.RIGHT
            canvas.drawText("QTY", qtyRight, baseline, label)
            canvas.drawText("UNIT PRICE", unitRight, baseline, label)
            canvas.drawText("AMOUNT", amountRight, baseline, label)
            label.textAlign = Paint.Align.LEFT
            rule(canvas, top + headerHeight - 1, strong = true)
        }

        private fun drawRow(canvas: Canvas, index: Int, top: Float) {
            val line = doc.lines[index]
            draw(canvas, descriptionLayouts[index], left, top + CELL_PADDING)
            val baseline = top + CELL_PADDING + firstBaseline(descriptionLayouts[index])
            bodyNumbers.textAlign = Paint.Align.RIGHT
            canvas.drawText(line.quantity, qtyRight, baseline, bodyNumbers)
            canvas.drawText(line.unitPrice, unitRight, baseline, bodyNumbers)
            canvas.drawText(line.amount, amountRight, baseline, bodyNumbers)
            rule(canvas, top + rowHeights[index] - 0.5f)
        }

        // ---- Blocks after the table ----------------------------------------------------------------

        private interface Tail {
            val height: Float
            fun draw(canvas: Canvas, top: Float)
        }

        /** Subtotal, discount, tax and the total, right-aligned; kept together on one page. */
        private inner class TotalsTail : Tail {
            private val totalPaint = paint(TOTAL_SIZE, bold = true, numbers = true)
            override val height = (doc.totals.size - 1) * FACT_LINE + 8 + TOTAL_SIZE + 6

            override fun draw(canvas: Canvas, top: Float) {
                val labelX = right - TOTALS_WIDTH
                val plain = doc.totals.dropLast(1)
                plain.forEachIndexed { i, row ->
                    val baseline = top + i * FACT_LINE + BODY_SIZE * 0.85f
                    muted.textAlign = Paint.Align.LEFT
                    canvas.drawText(row.label, labelX, baseline, muted)
                    bodyNumbers.textAlign = Paint.Align.RIGHT
                    canvas.drawText(row.value, right, baseline, bodyNumbers)
                }
                val totalTop = top + plain.size * FACT_LINE + 4
                val line = Paint().apply { color = PdfColors.INK; strokeWidth = 1f }
                canvas.drawLine(labelX, totalTop, right, totalTop, line)
                val total = doc.totals.last()
                val baseline = totalTop + 4 + TOTAL_SIZE * 0.9f
                totalPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(total.label, labelX, baseline, totalPaint)
                totalPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(total.value, right, baseline, totalPaint)
            }
        }

        private inner class TextTail(private val heading: String, text: String) : Tail {
            private val textLayout = layout(text, body, contentWidth.toInt())
            override val height = LABEL_SIZE + 6 + textLayout.height.toFloat()

            override fun draw(canvas: Canvas, top: Float) {
                label.textAlign = Paint.Align.LEFT
                canvas.drawText(heading, left, top + LABEL_SIZE * 0.85f, label)
                draw(canvas, textLayout, left, top + LABEL_SIZE + 6)
            }
        }

        // ---- Marks and footer ----------------------------------------------------------------------

        private fun drawFooter(canvas: Canvas, page: Int, of: Int) {
            muted.textAlign = Paint.Align.CENTER
            canvas.drawText("Page $page of $of", pageWidth / 2f, pageHeight - MARGIN + SMALL_SIZE, muted)
            muted.textAlign = Paint.Align.LEFT
        }

        /** Large faint diagonal "DRAFT" on every page, under the content. */
        private fun drawWatermark(canvas: Canvas) {
            val p = paint(WATERMARK_SIZE, bold = true, color = PdfColors.STAMP_VOID).apply {
                alpha = 30
                textAlign = Paint.Align.CENTER
            }
            canvas.save()
            canvas.rotate(-35f, pageWidth / 2f, pageHeight / 2f)
            canvas.drawText("DRAFT", pageWidth / 2f, pageHeight / 2f + WATERMARK_SIZE / 3f, p)
            canvas.restore()
        }

        /** A rotated, outlined "PAID" (with its date) or "VOID" stamp near the top of page 1. */
        private fun drawStamp(canvas: Canvas) {
            val paid = doc.mark == PdfMark.PAID
            val color = if (paid) PdfColors.STAMP_PAID else PdfColors.STAMP_VOID
            val word = if (paid) "PAID" else "VOID"
            val big = paint(STAMP_SIZE, bold = true, color = color).apply { textAlign = Paint.Align.CENTER; letterSpacing = 0.1f }
            val small = paint(SMALL_SIZE, bold = true, color = color).apply { textAlign = Paint.Align.CENTER }
            val detail = doc.markDetail
            val width = maxOf(big.measureText(word), detail?.let(small::measureText) ?: 0f) + 28
            val height = STAMP_SIZE + 16 + (if (detail != null) SMALL_SIZE + 4 else 0f)
            val cx = left + contentWidth * 0.62f
            val cy = billToTop - SECTION_GAP / 2

            canvas.save()
            canvas.rotate(-12f, cx, cy)
            val box = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 2.5f; isAntiAlias = true }
            canvas.drawRoundRect(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2, 6f, 6f, box)
            val wordBaseline = cy - height / 2 + 8 + STAMP_SIZE * 0.85f
            canvas.drawText(word, cx, wordBaseline, big)
            if (detail != null) canvas.drawText(detail, cx, wordBaseline + 4 + SMALL_SIZE, small)
            canvas.restore()
        }

        // ---- Helpers -------------------------------------------------------------------------------

        private fun rule(canvas: Canvas, y: Float, strong: Boolean = false) {
            val p = Paint().apply { color = if (strong) PdfColors.MUTED else PdfColors.RULE; strokeWidth = if (strong) 0.8f else 0.5f }
            canvas.drawLine(left, y, right, y, p)
        }

        private fun draw(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }

        private fun firstBaseline(layout: StaticLayout): Float = layout.getLineBaseline(0).toFloat()
    }

    companion object {
        private const val MARGIN = 40f
        private const val FOOTER_SPACE = 16f
        private const val CONTINUED_SPACE = 22f
        private const val SECTION_GAP = 24f
        private const val TAIL_GAP = 18f
        private const val CELL_PADDING = 6f
        private const val CELL_GAP = 12f
        private const val QTY_WIDTH = 50f
        private const val UNIT_WIDTH = 100f
        private const val AMOUNT_WIDTH = 105f
        private const val FACTS_WIDTH = 190f
        private const val TOTALS_WIDTH = 230f
        private const val FACT_LINE = 15f

        private const val BODY_SIZE = 9.5f
        private const val SMALL_SIZE = 8.5f
        private const val LABEL_SIZE = 7.5f
        private const val NAME_SIZE = 14f
        private const val TITLE_SIZE = 22f
        private const val NUMBER_SIZE = 11f
        private const val TOTAL_SIZE = 12f
        private const val STAMP_SIZE = 24f
        private const val WATERMARK_SIZE = 110f

        private fun paint(size: Float, bold: Boolean = false, color: Int = PdfColors.INK, numbers: Boolean = false) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                this.color = color
                // Same-width digits so amounts line up (UX_SPEC 8.3).
                if (numbers) fontFeatureSettings = "tnum"
            }

        private fun layout(text: String, paint: TextPaint, width: Int): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
    }
}
