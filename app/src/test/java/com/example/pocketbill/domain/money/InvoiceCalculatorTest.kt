package com.example.pocketbill.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Calculation cases C-1 to C-14 from DATA_MODEL.md section 6.5. Expected values were computed independently. */
class InvoiceCalculatorTest {

    private fun totals(
        currency: CurrencyCode,
        vararg lines: LineInput,
        taxBp: Int = 0,
        discount: Discount? = null,
    ): InvoiceTotals {
        val result = InvoiceCalculator.calculate(currency, lines.toList(), taxBp, discount)
        return (result as CalculationResult.Ok).totals
    }

    private fun line(quantityMilli: Long, unitPriceMinor: Long) = LineInput(quantityMilli, unitPriceMinor)

    @Test fun `C-1 usability task T2 totals 570 euros`() {
        val t = totals(CurrencyCode.EUR, line(1_000, 45_000), line(2_000, 6_000))
        assertEquals(listOf(45_000L, 12_000L), t.lineAmountsMinor)
        assertEquals(57_000, t.totalMinor)
    }

    @Test fun `C-2 fractional hours`() {
        assertEquals(12_750, totals(CurrencyCode.USD, line(1_500, 8_500)).totalMinor)
    }

    @Test fun `C-3 line rounds half up`() {
        assertEquals(50, totals(CurrencyCode.USD, line(1_500, 33)).totalMinor)
    }

    @Test fun `C-4 exactly half rounds up`() {
        assertEquals(2, totals(CurrencyCode.USD, line(1_500, 1)).totalMinor)
    }

    @Test fun `C-5 below half rounds down`() {
        assertEquals(0, totals(CurrencyCode.USD, line(125, 1)).totalMinor)
    }

    @Test fun `C-6 zero-decimal currency`() {
        assertEquals(4_500, totals(CurrencyCode.JPY, line(3_000, 1_500)).totalMinor)
    }

    @Test fun `C-7 three-decimal currency`() {
        assertEquals(2_500, totals(CurrencyCode.KWD, line(2_000, 1_250)).totalMinor)
    }

    @Test fun `C-8 tax is rounded once on the subtotal`() {
        val t = totals(CurrencyCode.USD, line(1_000, 3_333), line(1_000, 3_333), line(1_000, 3_334), taxBp = 750)
        assertEquals(10_000, t.subtotalMinor)
        assertEquals(750, t.taxMinor)
        assertEquals(10_750, t.totalMinor)
    }

    @Test fun `C-9 tax rounds half up`() {
        val t = totals(CurrencyCode.USD, line(1_000, 10), taxBp = 500)
        assertEquals(1, t.taxMinor)
        assertEquals(11, t.totalMinor)
    }

    @Test fun `C-10 fractional tax rate`() {
        val t = totals(CurrencyCode.USD, line(1_000, 100), taxBp = 1_250)
        assertEquals(13, t.taxMinor)
        assertEquals(113, t.totalMinor)
    }

    @Test fun `C-11 percent discount is applied before tax`() {
        val t = totals(CurrencyCode.USD, line(1_000, 9_999), taxBp = 2_000, discount = Discount.Percent(1_000))
        assertEquals(1_000, t.discountMinor)
        assertEquals(1_800, t.taxMinor)
        assertEquals(10_799, t.totalMinor)
    }

    @Test fun `C-12 discount is capped at the subtotal`() {
        val t = totals(CurrencyCode.USD, line(1_000, 5_000), discount = Discount.Amount(8_000))
        assertEquals(5_000, t.discountMinor)
        assertEquals(0, t.totalMinor)
    }

    @Test fun `C-13 amounts over the limit are refused without overflow`() {
        val result = InvoiceCalculator.calculate(
            CurrencyCode.USD,
            listOf(LineInput(MoneyLimits.MAX_QUANTITY_MILLI, 99_999_999_999L)),
        )
        assertEquals(CalculationResult.TooLarge, result)
    }

    @Test fun `C-13 a total pushed over the limit by tax is refused`() {
        val result = InvoiceCalculator.calculate(
            CurrencyCode.USD,
            listOf(LineInput(1_000, MoneyLimits.MAX_AMOUNT_MINOR)),
            taxBp = 1,
        )
        assertEquals(CalculationResult.TooLarge, result)
    }

    @Test fun `C-14 adding different currencies throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100, CurrencyCode.EUR) + Money(100, CurrencyCode.USD)
        }
    }

    @Test fun `adding the same currency works`() {
        assertEquals(Money(300, CurrencyCode.EUR), Money(100, CurrencyCode.EUR) + Money(200, CurrencyCode.EUR))
    }

    @Test fun `an invoice with no lines totals zero`() {
        assertEquals(0, totals(CurrencyCode.USD).totalMinor)
    }

    @Test fun `negative or zero inputs are rejected as programming errors`() {
        assertThrows(IllegalArgumentException::class.java) {
            InvoiceCalculator.calculate(CurrencyCode.USD, listOf(LineInput(0, 100)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            InvoiceCalculator.calculate(CurrencyCode.USD, listOf(LineInput(1_000, -1)))
        }
        assertThrows(IllegalArgumentException::class.java) { Money(-1, CurrencyCode.USD) }
    }
}
