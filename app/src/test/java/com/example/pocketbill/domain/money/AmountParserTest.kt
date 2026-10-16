package com.example.pocketbill.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

/** Every row of the DATA_MODEL.md section 6.3 table, plus each currency decimal group. */
class AmountParserTest {

    private fun eur(text: String) = AmountParser.parseAmount(text, CurrencyCode.EUR)
    private fun ok(value: Long) = ParseResult.Ok(value)

    @Test fun `whole number`() = assertEquals(ok(45_000), eur("450"))

    @Test fun `one dot or comma with 1-2 digits is the decimal point`() {
        assertEquals(ok(45_050), eur("450.5"))
        assertEquals(ok(45_050), eur("450,5"))
        assertEquals(ok(45_055), eur("450,55"))
    }

    @Test fun `comma followed by exactly 3 digits is a thousands separator`() {
        assertEquals(ok(120_000), eur("1,200"))
        assertEquals(ok(120_000_000), eur("1,200,000"))
    }

    @Test fun `with both separators the last one is the decimal point`() {
        assertEquals(ok(120_050), eur("1,200.50"))
        assertEquals(ok(120_050), eur("1.200,50"))
    }

    @Test fun `a single dot is always the decimal point`() = assertEquals(ok(120), eur("1.200"))

    @Test fun `several dots are thousands separators`() = assertEquals(ok(120_000_000), eur("1.200.000"))

    @Test fun `symbols codes and spaces around the number are ignored`() {
        assertEquals(ok(6_000), eur("€ 60"))
        assertEquals(ok(6_000), eur("60 EUR"))
        assertEquals(ok(120_000), eur(" 1 200 "))
    }

    @Test fun `leading or trailing decimal point`() {
        assertEquals(ok(50), eur(".5"))
        assertEquals(ok(45_000), eur("450."))
    }

    @Test fun `too many decimals is refused, never rounded`() {
        assertEquals(ParseResult.TooManyDecimals(2), eur("10.555"))
        assertEquals(ParseResult.TooManyDecimals(0), AmountParser.parseAmount("10.5", CurrencyCode.JPY))
    }

    @Test fun `extra trailing zeros are fine`() {
        assertEquals(ok(4_500), AmountParser.parseAmount("4500.00", CurrencyCode.JPY))
        assertEquals(ok(1_050), eur("10.5000"))
    }

    @Test fun `each decimal group`() {
        assertEquals(ok(4_500), AmountParser.parseAmount("4,500", CurrencyCode.JPY))
        assertEquals(ok(1_250), AmountParser.parseAmount("1.25", CurrencyCode.KWD))
        assertEquals(ok(1_250), AmountParser.parseAmount("1.250", CurrencyCode.KWD))
        assertEquals(ParseResult.TooManyDecimals(3), AmountParser.parseAmount("1.2505", CurrencyCode.KWD))
    }

    @Test fun `errors`() {
        assertEquals(ParseResult.Negative, eur("-5"))
        assertEquals(ParseResult.Invalid, eur("abc"))
        assertEquals(ParseResult.Empty, eur(""))
        assertEquals(ParseResult.Empty, eur("   "))
        assertEquals(ParseResult.Invalid, eur("1e5"))
        assertEquals(ParseResult.Invalid, eur("1,2,3"))
        assertEquals(ParseResult.Invalid, eur("1,200.5.0"))
        assertEquals(ParseResult.Invalid, eur("€"))
    }

    @Test fun `zero price is allowed`() = assertEquals(ok(0), eur("0"))

    @Test fun `amounts over the limit`() {
        assertEquals(ok(MoneyLimits.MAX_AMOUNT_MINOR), eur("9,999,999,999,999.99"))
        assertEquals(ParseResult.TooLarge, eur("10000000000000"))
        assertEquals(ParseResult.TooLarge, eur("99999999999999999999999"))
    }

    @Test fun `quantities`() {
        assertEquals(ok(1_500), AmountParser.parseQuantity("1.5"))
        assertEquals(ok(250), AmountParser.parseQuantity("0.25"))
        assertEquals(ok(2_000), AmountParser.parseQuantity("2"))
        assertEquals(ParseResult.TooManyDecimals(3), AmountParser.parseQuantity("0.0005"))
        assertEquals(ParseResult.NotPositive, AmountParser.parseQuantity("0"))
        assertEquals(ParseResult.TooLarge, AmountParser.parseQuantity("1000000"))
    }

    @Test fun `percentages`() {
        assertEquals(ok(750), AmountParser.parsePercent("7.5"))
        assertEquals(ok(750), AmountParser.parsePercent("7.5%"))
        assertEquals(ok(2_000), AmountParser.parsePercent("20 %"))
        assertEquals(ok(0), AmountParser.parsePercent("0"))
        assertEquals(ParseResult.TooManyDecimals(2), AmountParser.parsePercent("7.125"))
        assertEquals(ParseResult.TooLarge, AmountParser.parsePercent("1000.01"))
    }
}
