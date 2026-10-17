package com.example.pocketbill.domain.format

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DateFormatterTest {
    private lateinit var savedLocale: Locale

    @Before fun saveLocale() { savedLocale = Locale.getDefault() }

    @After fun restoreLocale() = Locale.setDefault(savedLocale)

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test fun `full date is day, short English month, year`() {
        assertEquals("30 Oct 2026", DateFormatter.full(day(2026, 10, 30)))
        assertEquals("3 Apr 2026", DateFormatter.full(day(2026, 4, 3)))
    }

    @Test fun `lists drop the year only in the current year`() {
        val today = day(2026, 10, 17)
        assertEquals("30 Oct", DateFormatter.inList(day(2026, 10, 30), today))
        assertEquals("30 Dec 2025", DateFormatter.inList(day(2025, 12, 30), today))
    }

    @Test fun `phone language does not change dates`() {
        for (locale in listOf(Locale.GERMANY, Locale.FRANCE, Locale.JAPAN)) {
            Locale.setDefault(locale)
            assertEquals("$locale", "3 Mar 2026", DateFormatter.full(day(2026, 3, 3)))
        }
    }
}
