package com.example.pocketbill.pdf

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.FromSnapshot
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.PaperSize
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.ToSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** What the PDF shows (MVP_SPEC 6), and frozen vs live details (DATA_MODEL 5.2). */
class InvoiceDocumentTest {
    private val issue = LocalDate.of(2026, 10, 16).toEpochDay()
    private val due = LocalDate.of(2026, 10, 30).toEpochDay()

    private val profile = BusinessProfileEntity(
        uuid = "p", name = "Live Studio", contactLines = "hi@live.example\n+1 555 0100", address = "1 Live St",
        taxId = "DE123", paymentInstructions = "IBAN LIVE", defaultCurrency = "EUR", paperSize = PaperSize.LETTER,
        createdAt = 0, updatedAt = 0,
    )
    private val client = ClientEntity(uuid = "c", name = "Live Client", email = "live@client.example", country = "Germany", createdAt = 0, updatedAt = 0)

    private fun invoice(status: InvoiceStatus, change: InvoiceEntity.() -> InvoiceEntity = { this }) = InvoiceWithLines(
        InvoiceEntity(
            uuid = "i", number = if (status == InvoiceStatus.DRAFT) null else "INV-0014", status = status, clientId = 1,
            currency = "EUR", issueDate = issue, dueDate = due, subtotalMinor = 57000, totalMinor = 57000,
            fromSnapshot = SnapshotJson.encode(FromSnapshot(name = "Frozen Studio", paymentInstructions = "IBAN FROZEN")),
            toSnapshot = SnapshotJson.encode(ToSnapshot(name = "Müller Design GmbH", email = "billing@mueller.example")),
            createdAt = 0, updatedAt = 0,
        ).change(),
        listOf(
            LineItemEntity(uuid = "b", invoiceId = 1, position = 1, description = " Extra hours ", quantityMilli = 2_000, unitPriceMinor = 6000, amountMinor = 12000, createdAt = 0, updatedAt = 0),
            LineItemEntity(uuid = "a", invoiceId = 1, position = 0, description = "Logo design", quantityMilli = 1_500, unitPriceMinor = 30000, amountMinor = 45000, createdAt = 0, updatedAt = 0),
        ),
    )

    @Test fun `issued invoice uses frozen details`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.ISSUED), profile, client)
        assertEquals("Frozen Studio", doc.fromName)
        assertEquals("Müller Design GmbH", doc.toName)
        assertEquals(listOf("billing@mueller.example"), doc.toLines)
        assertEquals("IBAN FROZEN", doc.paymentInstructions)
        assertEquals("INV-0014_Muller-Design-GmbH.pdf", doc.fileName)
        assertEquals(PdfMark.NONE, doc.mark)
    }

    @Test fun `draft uses live details and has no number`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.DRAFT), profile, client)
        assertEquals("Live Studio", doc.fromName)
        assertEquals(listOf("hi@live.example", "+1 555 0100", "1 Live St", "Tax ID: DE123"), doc.fromLines)
        assertEquals("Live Client", doc.toName)
        assertEquals(listOf("Germany", "live@client.example"), doc.toLines)
        assertNull(doc.number)
        assertEquals(PdfMark.DRAFT, doc.mark)
        assertEquals("Draft_Live-Client.pdf", doc.fileName)
    }

    @Test fun `unreadable snapshot falls back to live details`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.ISSUED) { copy(fromSnapshot = "{broken") }, profile, client)
        assertEquals("Live Studio", doc.fromName)
    }

    @Test fun `draft without a client`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.DRAFT) { copy(clientId = null) }, profile, null)
        assertNull(doc.toName)
        assertEquals("Draft.pdf", doc.fileName)
    }

    @Test fun `paper size comes from the live profile, A4 without one`() {
        assertEquals(PaperSize.LETTER, InvoiceDocument.from(invoice(InvoiceStatus.ISSUED), profile, client).paperSize)
        assertEquals(PaperSize.A4, InvoiceDocument.from(invoice(InvoiceStatus.ISSUED), null, null).paperSize)
    }

    @Test fun `lines are in position order and formatted`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.ISSUED), profile, client)
        assertEquals(PdfLine("Logo design", "1.5", "€300.00", "€450.00"), doc.lines[0])
        assertEquals(PdfLine("Extra hours", "2", "€60.00", "€120.00"), doc.lines[1])
    }

    @Test fun `facts use unambiguous dates`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.ISSUED), profile, client)
        assertEquals(listOf(PdfRow("Issue date", "16 Oct 2026"), PdfRow("Due date", "30 Oct 2026"), PdfRow("Currency", "EUR")), doc.facts)
    }

    @Test fun `totals show discount and tax only when present`() {
        assertEquals(
            listOf(PdfRow("Subtotal", "€570.00"), PdfRow("Total due", "€570.00")),
            InvoiceDocument.from(invoice(InvoiceStatus.ISSUED), profile, client).totals,
        )
        val withBoth = invoice(InvoiceStatus.ISSUED) {
            copy(discountType = DiscountType.PERCENT, discountValue = 1_000, discountMinor = 5700, taxRateBp = 750, taxMinor = 3848, totalMinor = 55148)
        }
        assertEquals(
            listOf(PdfRow("Subtotal", "€570.00"), PdfRow("Discount (10%)", "−€57.00"), PdfRow("Tax (7.5%)", "€38.48"), PdfRow("Total due", "€551.48")),
            InvoiceDocument.from(withBoth, profile, client).totals,
        )
    }

    @Test fun `paid invoice gets the PAID mark with its date`() {
        val paid = LocalDate.of(2026, 10, 23).toEpochDay()
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.PAID) { copy(paidDate = paid) }, profile, client)
        assertEquals(PdfMark.PAID, doc.mark)
        assertEquals("23 Oct 2026", doc.markDetail)
        assertEquals(PdfRow("Paid on", "23 Oct 2026"), doc.facts.last())
        assertEquals("Total paid", doc.totals.last().label)
    }

    @Test fun `void invoice gets the VOID mark`() {
        val doc = InvoiceDocument.from(invoice(InvoiceStatus.VOID), profile, client)
        assertEquals(PdfMark.VOID, doc.mark)
        assertNull(doc.markDetail)
    }
}
