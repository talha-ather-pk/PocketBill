package com.example.pocketbill.domain.money

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Formatting examples from UX_SPEC.md section 7.2. */
class MoneyFormatterTest {
    private lateinit var savedLocale: Locale

    @Before fun saveLocale() { savedLocale = Locale.getDefault() }

    @After fun restoreLocale() = Locale.setDefault(savedLocale)

    private val expected = listOf(
        Triple(120_000L, CurrencyCode.EUR, "€1,200.00"),
        Triple(120_000L, CurrencyCode.USD, "$1,200.00"),
        Triple(120_000L, CurrencyCode.CAD, "CA$1,200.00"),
        Triple(120_000L, CurrencyCode.AUD, "A$1,200.00"),
        Triple(4_500L, CurrencyCode.JPY, "¥4,500"),
        Triple(2_500L, CurrencyCode.KWD, "KWD 2.500"),
        Triple(120_000L, CurrencyCode.PKR, "PKR 1,200.00"),
    )

    @Test fun `formats with English rules and unambiguous symbols`() {
        for ((minor, currency, text) in expected) {
            assertEquals(text, MoneyFormatter.format(minor, currency))
        }
    }

    @Test fun `output does not change with the phone's locale`() {
        for (locale in listOf(Locale.ENGLISH, Locale.GERMANY, Locale.FRANCE, Locale.JAPAN)) {
            Locale.setDefault(locale)
            for ((minor, currency, text) in expected) {
                assertEquals("$currency under $locale", text, MoneyFormatter.format(minor, currency))
            }
        }
    }

    @Test fun `plain format for input fields`() {
        assertEquals("1200.50", MoneyFormatter.formatPlain(120_050, CurrencyCode.EUR))
        assertEquals("4500", MoneyFormatter.formatPlain(4_500, CurrencyCode.JPY))
        assertEquals("2.500", MoneyFormatter.formatPlain(2_500, CurrencyCode.KWD))
        assertEquals("0.05", MoneyFormatter.formatPlain(5, CurrencyCode.USD))
    }
}
