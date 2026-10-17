package com.example.pocketbill.domain.format

import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.isOverdue
import com.example.pocketbill.domain.time.DeviceClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Date edge cases from P4-T07, finished in P5-T01: year ends, leap days, time zones, due-date boundaries. */
class DateEdgeCasesTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test fun `leap day and year ends format plainly`() {
        assertEquals("29 Feb 2028", DateFormatter.full(day(2028, 2, 29)))
        assertEquals("31 Dec 2026", DateFormatter.full(day(2026, 12, 31)))
        assertEquals("1 Jan 2027", DateFormatter.full(day(2027, 1, 1)))
    }

    @Test fun `lists show the year as soon as it differs, even a day apart`() {
        assertEquals("31 Dec 2026", DateFormatter.inList(day(2026, 12, 31), todayEpochDay = day(2027, 1, 1)))
        assertEquals("1 Jan", DateFormatter.inList(day(2027, 1, 1), todayEpochDay = day(2027, 1, 1)))
    }

    @Test fun `an invoice is overdue only from the day after its due date`() {
        val due = day(2026, 12, 31)
        assertFalse(isOverdue(InvoiceStatus.ISSUED, due, todayEpochDay = due))
        assertTrue(isOverdue(InvoiceStatus.ISSUED, due, todayEpochDay = day(2027, 1, 1)))
        assertFalse("paid invoices are never overdue", isOverdue(InvoiceStatus.PAID, due, todayEpochDay = due + 30))
    }

    @Test fun `14-day terms across February and the year end`() {
        assertEquals("14 Mar 2028", DateFormatter.full(day(2028, 2, 29) + 14))
        assertEquals("7 Jan 2027", DateFormatter.full(day(2026, 12, 24) + 14))
    }

    @Test fun `timestamps become the user's local day, not the UTC day`() {
        // Found on the phone: a draft edited at 1:54 am in Pakistan showed "edited 17 Oct" / "Started yesterday".
        val instant = java.time.Instant.parse("2026-10-17T20:54:00Z").toEpochMilli()
        assertEquals(day(2026, 10, 18), DeviceClock { ZoneId.of("Asia/Karachi") }.dayOf(instant))
        assertEquals(day(2026, 10, 17), DeviceClock { ZoneId.of("America/New_York") }.dayOf(instant))
        assertEquals(day(2026, 10, 17), DeviceClock { ZoneId.of("UTC") }.dayOf(instant))
    }

    @Test fun `today follows the phone's own time zone`() {
        // Same instant, different calendar days: Tonga is already on the next day.
        val tonga = DeviceClock { ZoneId.of("Pacific/Tongatapu") }.todayEpochDay()
        val hawaii = DeviceClock { ZoneId.of("Pacific/Honolulu") }.todayEpochDay()
        assertTrue(tonga - hawaii in 0..2)
        assertEquals(LocalDate.now(ZoneId.of("Pacific/Tongatapu")).toEpochDay(), tonga)
    }
}
