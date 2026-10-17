package com.example.pocketbill.ui.editor

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.money.Discount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The editor's form rules (P4-T06): live recalculation, tax, discount, issue checks, defaults, copies. */
class EditorFormTest {

    private fun form(vararg lines: LineForm, currency: String = "EUR", tax: String = "0") =
        EditorForm(clientId = 1, currency = currency, issueDate = 100, dueDays = 14, taxRate = tax, lines = lines.toList())

    // ---- Recalculation ------------------------------------------------------------------------------

    @Test fun `usability task T2 recalculates to 570 euros as lines change`() {
        var f = form(LineForm(description = "Logo design", quantity = "1", price = "450"))
        assertEquals(45_000, f.totals().totalMinor)
        f = f.copy(lines = f.lines + LineForm(description = "Extra revisions", quantity = "2", price = "60"))
        val t = f.totals()
        assertEquals(listOf<Long?>(45_000, 12_000), t.lineAmounts)
        assertEquals(57_000, t.totalMinor)
    }

    // Symbols and codes around a number are ignored ("60 EUR" reads as 60, so "4x" reads as 4);
    // a letter inside a number can't be read (DATA_MODEL 6.3).
    @Test fun `lines that can't be read yet count as zero and show no line total`() {
        val t = form(LineForm(description = "A", price = "450"), LineForm(description = "B", price = "4x5")).totals()
        assertEquals(listOf(45_000L, null), t.lineAmounts)
        assertEquals(45_000, t.totalMinor)
    }

    @Test fun `typed decimals with a comma, fractional quantities`() {
        val t = form(LineForm(description = "Hours", quantity = "1,5", price = "85,00"), currency = "USD").totals()
        assertEquals(12_750, t.totalMinor)
    }

    @Test fun `tax is calculated once on the subtotal`() {
        val t = form(
            LineForm(description = "a", price = "33.33"), LineForm(description = "b", price = "33.33"), LineForm(description = "c", price = "33.34"),
            currency = "USD", tax = "7.5",
        ).totals()
        assertEquals(10_000, t.subtotalMinor)
        assertEquals(750, t.taxMinor)
        assertEquals(10_750, t.totalMinor)
    }

    @Test fun `percent and amount discounts come off before tax and are capped`() {
        val base = form(LineForm(description = "Work", price = "99.99"), currency = "USD", tax = "20")
        val pct = base.copy(discountType = DiscountType.PERCENT, discountValue = "10").totals()
        assertEquals(1_000, pct.discountMinor)
        assertEquals(10_799, pct.totalMinor)
        val capped = base.copy(discountType = DiscountType.AMOUNT, discountValue = "500").totals()
        assertEquals(9_999, capped.discountMinor)
        assertEquals(0, capped.totalMinor)
    }

    @Test fun `zero-decimal currency`() {
        assertEquals(4_500, form(LineForm(description = "x", quantity = "3", price = "1,500"), currency = "JPY").totals().totalMinor)
    }

    // ---- Issue checks -----------------------------------------------------------------------------

    @Test fun `a complete invoice has no issue errors`() {
        assertTrue(form(LineForm(description = "Logo design", price = "450")).issueErrors().isEmpty())
    }

    @Test fun `every problem is reported at its field`() {
        val f = EditorForm(
            clientId = null, currency = "EUR", issueDate = 100, dueDays = null, customDueDate = 90, taxRate = "abc",
            lines = listOf(LineForm(description = "", quantity = "0", price = "10.555"), LineForm(description = "Ok", price = "")),
        )
        val errors = f.issueErrors()
        assertTrue(EditorError.NoClient in errors)
        assertTrue(EditorError.LineDescription(0) in errors)
        assertTrue(EditorError.LineQuantity(0) in errors)
        assertTrue("too many decimals is named", EditorError.LinePrice(0, decimals = 2) in errors)
        assertTrue(EditorError.LinePrice(1) in errors)
        assertTrue(EditorError.TaxRate in errors)
        assertTrue(EditorError.DueBeforeIssue in errors)
    }

    @Test fun `empty extra lines are ignored but an invoice needs at least one item`() {
        assertTrue(form(LineForm(description = "Logo", price = "450"), LineForm()).issueErrors().isEmpty())
        assertEquals(listOf(EditorError.NoLines), form(LineForm()).issueErrors())
    }

    @Test fun `saving a draft drops blank lines and never refuses half-typed numbers`() {
        val input = form(LineForm(description = "Logo", price = "4x5"), LineForm()).copy(discountType = DiscountType.PERCENT, discountValue = "10").toInput()
        assertEquals(1, input.lines.size)
        assertEquals(0L, input.lines.single().unitPriceMinor)
        assertEquals(Discount.Percent(1_000), input.discount)
        assertEquals(114, input.dueDate)
    }

    // ---- Defaults (D-015) ---------------------------------------------------------------------------

    private val profile = BusinessProfileEntity(uuid = "p", name = "Studio", defaultCurrency = "USD", defaultTermsDays = 30, defaultTaxRateBp = 500, createdAt = 0, updatedAt = 0)
    private fun client(currency: String? = null, terms: Int? = null) =
        ClientEntity(id = 1, uuid = "c", name = "Acme", preferredCurrency = currency, preferredTermsDays = terms, createdAt = 0, updatedAt = 0)
    private val lastInvoice = InvoiceEntity(id = 9, uuid = "i", clientId = 1, currency = "GBP", issueDate = 10, dueDate = 17, taxRateBp = 2_000, createdAt = 0, updatedAt = 0)

    @Test fun `client preference wins, then last invoice, then business defaults`() {
        assertEquals(ClientDefaults("EUR", 14, 2_000), resolveDefaults(client("EUR", 14), lastInvoice, profile))
        assertEquals(ClientDefaults("GBP", 7, 2_000), resolveDefaults(client(), lastInvoice, profile))
        assertEquals(ClientDefaults("USD", 30, 500), resolveDefaults(client(), null, profile))
        assertEquals(ClientDefaults("USD", 14, 0), resolveDefaults(null, null, null))
    }

    // ---- Saved invoices and copies -----------------------------------------------------------------

    private val saved = InvoiceWithLines(
        InvoiceEntity(
            id = 5, uuid = "i5", number = "INV-0005", status = InvoiceStatus.ISSUED, clientId = 1, currency = "EUR",
            issueDate = 100, dueDate = 121, taxRateBp = 750, discountType = DiscountType.AMOUNT, discountValue = 1_000,
            notes = "Thanks", createdAt = 0, updatedAt = 0,
        ),
        listOf(
            LineItemEntity(id = 2, uuid = "l2", invoiceId = 5, position = 1, description = "Second", quantityMilli = 2_000, unitPriceMinor = 6_000, amountMinor = 12_000, createdAt = 0, updatedAt = 0),
            LineItemEntity(id = 1, uuid = "l1", invoiceId = 5, position = 0, description = "First", quantityMilli = 1_500, unitPriceMinor = 45_000, amountMinor = 67_500, createdAt = 0, updatedAt = 0),
        ),
    )

    @Test fun `a saved invoice loads back as typed text in line order`() {
        val f = saved.toForm()
        assertEquals(listOf("First", "Second"), f.lines.map { it.description })
        assertEquals(listOf("1.5", "2"), f.lines.map { it.quantity })
        assertEquals(listOf("450.00", "60.00"), f.lines.map { it.price })
        assertEquals("7.5", f.taxRate)
        assertEquals("10.00", f.discountValue)
        assertNull("21 days isn't a chip, so it's a custom date", f.dueDays)
        assertEquals(121, f.dueDate)
        assertEquals(saved.lines.map { it.uuid }, f.lines.map { it.uuid })
    }

    @Test fun `duplicate keeps content, resets dates by the same terms, and gets new line identities`() {
        val copy = saved.toDuplicateForm(today = 200)
        assertEquals(200, copy.issueDate)
        assertEquals(221, copy.dueDate)
        assertEquals(listOf("First", "Second"), copy.lines.map { it.description })
        assertEquals("Thanks", copy.notes)
        copy.lines.zip(saved.lines).forEach { (a, b) -> assertNotEquals(b.uuid, a.uuid) }
    }
}
