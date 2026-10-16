package com.example.pocketbill.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Currency

/** Checks the currency table (DATA_MODEL.md section 6.2). */
class CurrencyCodeTest {

    @Test fun `decimal places match ISO 4217 as published in the JDK's currency data`() {
        for (code in CurrencyCode.entries) {
            assertEquals(code.name, Currency.getInstance(code.name).defaultFractionDigits, code.minorDigits)
        }
    }

    @Test fun `the MVP list has 48 currencies`() = assertEquals(48, CurrencyCode.entries.size)

    @Test fun `every currency has a symbol with no digits or separators`() {
        for (code in CurrencyCode.entries) {
            assertTrue(code.name, code.symbol.isNotBlank())
            assertTrue(code.name, code.symbol.none { it.isDigit() || it == '.' || it == ',' || it == '-' })
        }
    }

    @Test fun `codes are looked up exactly`() {
        assertEquals(CurrencyCode.EUR, CurrencyCode.fromCode("EUR"))
        assertNull(CurrencyCode.fromCode("eur"))
        assertNull(CurrencyCode.fromCode("XYZ"))
    }
}
