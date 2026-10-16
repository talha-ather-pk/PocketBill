package com.example.pocketbill.domain.money

import java.math.BigInteger

/** One invoice line: quantity in thousandths (1500 = 1.5) and unit price in minor units. */
data class LineInput(val quantityMilli: Long, val unitPriceMinor: Long)

/** An invoice-level discount (F-15). */
sealed interface Discount {
    /** A percentage of the subtotal, in basis points (1000 = 10%). */
    data class Percent(val bp: Int) : Discount

    /** A fixed amount in minor units of the invoice currency. */
    data class Amount(val minor: Long) : Discount
}

/** Results of [InvoiceCalculator.calculate], all in minor units of [currency]. */
data class InvoiceTotals(
    val currency: CurrencyCode,
    val lineAmountsMinor: List<Long>,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
)

sealed interface CalculationResult {
    data class Ok(val totals: InvoiceTotals) : CalculationResult

    /** A line, the subtotal or the total would exceed [MoneyLimits.MAX_AMOUNT_MINOR] (M-6). */
    data object TooLarge : CalculationResult
}

/**
 * Invoice arithmetic from DATA_MODEL.md section 6.4. Every step rounds half-up to the
 * currency's minor unit, using exact integer maths, so no amount is ever approximated.
 */
object InvoiceCalculator {
    private val THOUSAND = BigInteger.valueOf(1_000)
    private val TEN_THOUSAND = BigInteger.valueOf(10_000)
    private val MAX = BigInteger.valueOf(MoneyLimits.MAX_AMOUNT_MINOR)

    fun calculate(
        currency: CurrencyCode,
        lines: List<LineInput>,
        taxBp: Int = 0,
        discount: Discount? = null,
    ): CalculationResult {
        require(taxBp in 0..MoneyLimits.MAX_TAX_BP) { "Tax rate out of range: $taxBp" }
        for (line in lines) {
            require(line.quantityMilli in 1..MoneyLimits.MAX_QUANTITY_MILLI) { "Quantity out of range: ${line.quantityMilli}" }
            require(line.unitPriceMinor in 0..MoneyLimits.MAX_AMOUNT_MINOR) { "Price out of range: ${line.unitPriceMinor}" }
        }

        // 1. Line amounts, each rounded. 2. Subtotal is their sum.
        val lineAmounts = ArrayList<Long>(lines.size)
        var subtotal = BigInteger.ZERO
        for (line in lines) {
            val amount = roundHalfUp(big(line.quantityMilli) * big(line.unitPriceMinor), THOUSAND)
            if (amount > MAX) return CalculationResult.TooLarge
            lineAmounts += amount.toLong()
            subtotal += amount
        }
        if (subtotal > MAX) return CalculationResult.TooLarge

        // 3. Discount, capped at the subtotal.
        val discountAmount = when (discount) {
            null -> BigInteger.ZERO
            is Discount.Percent -> {
                require(discount.bp in 0..MoneyLimits.MAX_DISCOUNT_BP) { "Discount out of range: ${discount.bp}" }
                roundHalfUp(subtotal * big(discount.bp), TEN_THOUSAND)
            }
            is Discount.Amount -> {
                require(discount.minor in 0..MoneyLimits.MAX_AMOUNT_MINOR) { "Discount out of range: ${discount.minor}" }
                big(discount.minor)
            }
        }.min(subtotal)

        // 4. Tax, rounded once on the whole invoice. 5. Total.
        val taxable = subtotal - discountAmount
        val tax = roundHalfUp(taxable * big(taxBp), TEN_THOUSAND)
        val total = taxable + tax
        if (total > MAX) return CalculationResult.TooLarge

        return CalculationResult.Ok(
            InvoiceTotals(
                currency = currency,
                lineAmountsMinor = lineAmounts,
                subtotalMinor = subtotal.toLong(),
                discountMinor = discountAmount.toLong(),
                taxMinor = tax.toLong(),
                totalMinor = total.toLong(),
            ),
        )
    }

    /** Divides [numerator] by [denominator], rounding half-up. Both must be non-negative. */
    internal fun roundHalfUp(numerator: BigInteger, denominator: BigInteger): BigInteger =
        (numerator.shiftLeft(1) + denominator) / denominator.shiftLeft(1)

    private fun big(value: Long): BigInteger = BigInteger.valueOf(value)

    private fun big(value: Int): BigInteger = BigInteger.valueOf(value.toLong())
}
