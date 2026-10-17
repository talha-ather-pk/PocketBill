package com.example.pocketbill.pdf

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pocketbill.domain.invoice.PaperSize
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * MVP_SPEC 6 and 9.4 on the phone: the D-007 characters have glyphs in the PDF font, a 50-item
 * invoice paginates and renders in under 2 seconds, and A4 and Letter have the right page sizes.
 * Writes only to a test folder in the cache; the app's data is not touched.
 */
@RunWith(AndroidJUnit4::class)
class InvoicePdfRendererTest {
    private val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "pdf-test")
    private val renderer = InvoicePdfRenderer(dir)

    @After fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun doc(lines: Int, paper: PaperSize = PaperSize.A4, mark: PdfMark = PdfMark.NONE) = InvoiceDocument(
        paperSize = paper,
        number = "INV-0014",
        mark = mark,
        markDetail = if (mark == PdfMark.PAID) "23 Oct 2026" else null,
        fromName = "Øresund Design ApS",
        fromLines = listOf("hej@oresund.example", "Strøget 1, København"),
        toName = "Müller Design GmbH",
        toLines = listOf("Straße 5, München", "Germany"),
        toEmail = null,
        facts = listOf(PdfRow("Issue date", "16 Oct 2026"), PdfRow("Due date", "30 Oct 2026"), PdfRow("Currency", "EUR")),
        lines = List(lines) { PdfLine("Item ${it + 1} for José and François: design work with a longer description that wraps", "2", "€60.00", "€120.00") },
        totals = listOf(PdfRow("Subtotal", "€6,000.00"), PdfRow("Total due", "€6,000.00")),
        paymentInstructions = "IBAN DE00 0000 0000 0000\nPayPal: pay@oresund.example",
        notes = "Thank you!",
        fileName = "INV-0014_Muller-Design-GmbH.pdf",
    )

    private fun pages(file: File): List<Pair<Int, Int>> =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r -> (0 until r.pageCount).map { i -> r.openPage(i).use { it.width to it.height } } }
        }

    @Test fun d007CharactersHaveGlyphsInThePdfFont() {
        val regular = Paint().apply { typeface = Typeface.DEFAULT }
        val bold = Paint().apply { typeface = Typeface.DEFAULT_BOLD }
        // hasGlyph checks one character at a time, so the words are split into characters.
        val characters = "José Müller Øresund François €£¥₹₦₱₨−…·—".filterNot { it == ' ' }.map { it.toString() }.distinct()
        val missing = characters.filterNot { regular.hasGlyph(it) && bold.hasGlyph(it) }
        assertTrue("No glyph for: $missing", missing.isEmpty())
    }

    @Test fun shortInvoiceIsOneA4Page() = runBlocking {
        val file = renderer.render(doc(lines = 3))
        assertEquals("INV-0014_Muller-Design-GmbH.pdf", file.name)
        assertEquals(listOf(595 to 842), pages(file))
    }

    @Test fun letterPagesHaveLetterSize() = runBlocking {
        val file = renderer.render(doc(lines = 3, paper = PaperSize.LETTER))
        assertEquals(listOf(612 to 792), pages(file))
    }

    @Test fun fiftyItemsPaginateInUnderTwoSeconds() = runBlocking {
        renderer.render(doc(lines = 1)) // warm up fonts and classes
        val start = System.nanoTime()
        val file = renderer.render(doc(lines = 50))
        val ms = (System.nanoTime() - start) / 1_000_000
        val count = pages(file).size
        assertTrue("Expected several pages, got $count", count in 2..10)
        assertTrue("Took $ms ms", ms < 2_000)
    }

    @Test fun marksRenderWithoutErrors() = runBlocking {
        listOf(PdfMark.DRAFT, PdfMark.PAID, PdfMark.VOID).forEach { mark ->
            assertEquals(1, pages(renderer.render(doc(lines = 2, mark = mark))).size)
        }
    }

    @Test fun logoIsDrawnAndAMissingLogoIsSkipped() = runBlocking {
        val logoDir = File(dir.parentFile, "logo-test").apply { mkdirs() }
        try {
            val png = java.io.ByteArrayOutputStream().also {
                android.graphics.Bitmap.createBitmap(300, 90, android.graphics.Bitmap.Config.ARGB_8888)
                    .apply { eraseColor(android.graphics.Color.rgb(31, 78, 121)) }
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }.toByteArray()
            File(logoDir, "logo.png").writeBytes(png)
            val withLogo = InvoicePdfRenderer(dir, logoDir)
            assertEquals(1, pages(withLogo.render(doc(lines = 3).copy(logoFile = "logo.png"))).size)
            assertEquals(1, pages(withLogo.render(doc(lines = 3).copy(logoFile = "missing.png"))).size)
        } finally {
            logoDir.deleteRecursively()
        }
    }

    @Test fun onlyTheLatestPdfStaysInTheFolder() = runBlocking {
        renderer.render(doc(lines = 1).copy(fileName = "A.pdf"))
        renderer.render(doc(lines = 1).copy(fileName = "B.pdf"))
        assertEquals(listOf("B.pdf"), dir.list()!!.toList())
    }
}
