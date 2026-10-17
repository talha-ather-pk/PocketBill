package com.example.pocketbill.domain.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MVP_SPEC 6 file names: ASCII-only, accents stripped, special letters spelled out. */
class PdfFileNameTest {

    @Test fun `spec example`() =
        assertEquals("INV-0014_Muller-Design-GmbH.pdf", PdfFileName.forInvoice("INV-0014", "Müller Design GmbH"))

    @Test fun `letters without accents are spelled out`() {
        assertEquals("Oresund-Design-ApS", PdfFileName.asciiPart("Øresund Design ApS"))
        assertEquals("Strasse", PdfFileName.asciiPart("Straße"))
        assertEquals("AEro-Lodz", PdfFileName.asciiPart("Æro Łódź"))
    }

    @Test fun `accents are stripped`() =
        assertEquals("Jose-Francois-Sao-Paulo", PdfFileName.asciiPart("José François São Paulo"))

    @Test fun `everything else becomes a single dash and edges are trimmed`() =
        assertEquals("A-B-C-D", PdfFileName.asciiPart("  A & B / C… (D) "))

    @Test fun `scripts with no Latin form drop out`() =
        assertEquals("INV-0001.pdf", PdfFileName.forInvoice("INV-0001", "محمد"))

    @Test fun `no client gives the number only`() =
        assertEquals("INV-0001.pdf", PdfFileName.forInvoice("INV-0001", null))

    @Test fun `drafts have no number yet`() =
        assertEquals("Draft_Acme.pdf", PdfFileName.forInvoice(null, "Acme"))

    @Test fun `long client names are cut without a trailing dash`() =
        assertEquals("A".repeat(59), PdfFileName.asciiPart("A".repeat(59) + " Bcd"))

    @Test fun `result is always ASCII`() {
        val name = PdfFileName.forInvoice("INV-0009", "Ünïcödé ₹ € £ ¥ 漢字 Ĳssel")
        assertTrue(name, name.all { it.code < 128 })
    }
}
