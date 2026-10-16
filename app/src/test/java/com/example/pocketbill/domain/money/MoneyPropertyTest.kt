package com.example.pocketbill.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Property tests from DATA_MODEL.md section 6.5, over many random inputs with a fixed seed. */
class MoneyPropertyTest {
    private val random = Random(20261016)

    @Test fun `formatting then parsing returns the same amount in every currency`() {
        for (currency in CurrencyCode.entries) {
            repeat(2_000) {
                val minor = when (it % 3) {
                    0 -> random.nextLong(0, 1_000)
                    1 -> random.nextLong(0, 100_000_000)
                    else -> random.nextLong(0, MoneyLimits.MAX_AMOUNT_MINOR + 1)
                }
                for (text in listOf(MoneyFormatter.format(minor, currency), MoneyFormatter.formatPlain(minor, currency))) {
                    assertEquals("$text in $currency", ParseResult.Ok(minor), AmountParser.parseAmount(text, currency))
                }
            }
        }
    }

    @Test fun `totals always add up and are never negative`() {
        repeat(20_000) {
            val lines = List(random.nextInt(0, 8)) {
                LineInput(random.nextLong(1, 100_000), random.nextLong(0, 10_000_000))
            }
            val discount = when (random.nextInt(3)) {
                0 -> null
                1 -> Discount.Percent(random.nextInt(0, MoneyLimits.MAX_DISCOUNT_BP + 1))
                else -> Discount.Amount(random.nextLong(0, 1_000_000_000))
            }
            val result = InvoiceCalculator.calculate(CurrencyCode.USD, lines, random.nextInt(0, 3_001), discount)
            val t = (result as CalculationResult.Ok).totals
            assertEquals(t.subtotalMinor, t.lineAmountsMinor.sum())
            assertEquals(t.totalMinor, t.subtotalMinor - t.discountMinor + t.taxMinor)
            assertTrue(t.discountMinor in 0..t.subtotalMinor)
            assertTrue(t.totalMinor >= 0)
        }
    }

    @Test fun `half-up rounding matches BigDecimal HALF_UP`() {
        repeat(20_000) {
            val numerator = java.math.BigInteger.valueOf(random.nextLong(0, Long.MAX_VALUE))
            val denominator = java.math.BigInteger.valueOf(random.nextLong(1, 1_000_000))
            val expected = java.math.BigDecimal(numerator).divide(java.math.BigDecimal(denominator), 0, java.math.RoundingMode.HALF_UP).toBigInteger()
            assertEquals(expected, InvoiceCalculator.roundHalfUp(numerator, denominator))
        }
    }
}
