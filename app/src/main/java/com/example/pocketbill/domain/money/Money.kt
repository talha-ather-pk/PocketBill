package com.example.pocketbill.domain.money

/** Limits from DATA_MODEL.md section 6.1 (M-3, M-4, M-6). */
object MoneyLimits {
    /** Largest amount in minor units. Below 2^53, so it stays exact in JSON backups. */
    const val MAX_AMOUNT_MINOR: Long = 999_999_999_999_999L

    /** Largest quantity in thousandths (999,999.999). */
    const val MAX_QUANTITY_MILLI: Long = 999_999_999L

    /** Largest tax rate in basis points (1000%). */
    const val MAX_TAX_BP: Int = 100_000

    /** Largest percentage discount in basis points (100%). */
    const val MAX_DISCOUNT_BP: Int = 10_000
}

/**
 * An amount of money in whole minor units (cents for USD) together with its currency.
 * Amounts are never negative (M-5) and never above [MoneyLimits.MAX_AMOUNT_MINOR] (M-6).
 */
data class Money(val minor: Long, val currency: CurrencyCode) {
    init {
        require(minor in 0..MoneyLimits.MAX_AMOUNT_MINOR) { "Amount out of range: $minor" }
    }

    /** Adds two amounts in the same currency. Different currencies are refused, never converted (M-2, M-7). */
    operator fun plus(other: Money): Money {
        require(other.currency == currency) { "Cannot add ${other.currency} to $currency" }
        return Money(Math.addExact(minor, other.minor), currency)
    }
}
