package com.example.pocketbill.domain.invoice

import com.example.pocketbill.domain.text.normalizeForMatching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure rules behind P4-T03: numbering (DATA_MODEL 5.1), snapshots (5.3), overdue, name matching. */
class InvoiceRulesTest {

    @Test fun `numbers are padded to four digits, then grow`() {
        assertEquals("INV-0001", InvoiceNumbering.format("INV-", 1))
        assertEquals("INV-0058", InvoiceNumbering.format("INV-", 58))
        assertEquals("INV-9999", InvoiceNumbering.format("INV-", 9_999))
        assertEquals("INV-10000", InvoiceNumbering.format("INV-", 10_000))
        assertEquals("2026-0007", InvoiceNumbering.format("2026-", 7))
    }

    @Test fun `first free number skips taken ones (N-7)`() {
        val taken = setOf("INV-0042", "INV-0043")
        assertEquals(AssignedNumber("INV-0044", 44), InvoiceNumbering.firstFree("INV-", 42) { it in taken })
        assertEquals(AssignedNumber("INV-0041", 41), InvoiceNumbering.firstFree("INV-", 41) { it in taken })
    }

    @Test fun `snapshots round-trip and default to version 1`() {
        val from = FromSnapshot(name = "Your Studio", paymentInstructions = "IBAN DE89 3704")
        val to = ToSnapshot(name = "Müller Design GmbH", email = "billing@mueller.example", country = "Germany")
        assertEquals(from, SnapshotJson.decodeFrom(SnapshotJson.encode(from)))
        assertEquals(to, SnapshotJson.decodeTo(SnapshotJson.encode(to)))
        assertTrue(SnapshotJson.encode(to).contains("\"v\":1"))
        assertTrue("accents kept in JSON", SnapshotJson.encode(to).contains("Müller"))
    }

    @Test fun `unknown snapshot fields are ignored (5_3)`() {
        val later = """{"v":2,"name":"Acme","email":null,"vatScheme":"reverse-charge"}"""
        assertEquals(ToSnapshot(v = 2, name = "Acme"), SnapshotJson.decodeTo(later))
    }

    @Test fun `unreadable snapshots return null instead of throwing`() {
        assertNull(SnapshotJson.decodeTo("not json"))
        assertNull(SnapshotJson.decodeFrom("""{"v":1}""")) // name missing
    }

    @Test fun `overdue means issued and past the due date`() {
        assertTrue(isOverdue(InvoiceStatus.ISSUED, dueDateEpochDay = 100, todayEpochDay = 101))
        assertFalse("due today is not overdue", isOverdue(InvoiceStatus.ISSUED, 100, 100))
        assertFalse(isOverdue(InvoiceStatus.PAID, 100, 200))
        assertFalse(isOverdue(InvoiceStatus.DRAFT, 100, 200))
        assertFalse(isOverdue(InvoiceStatus.VOID, 100, 200))
    }

    @Test fun `names match ignoring case, accents and extra spaces`() {
        assertEquals(normalizeForMatching("muller design gmbh"), normalizeForMatching("  Müller   Design GmbH "))
        assertEquals("francois", normalizeForMatching("François"))
    }
}
