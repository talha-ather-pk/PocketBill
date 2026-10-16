package com.example.pocketbill.domain.money

import java.math.BigInteger

/** Outcome of reading a typed number. Values are scaled integers: minor units, thousandths or basis points. */
sealed interface ParseResult {
    data class Ok(val value: Long) : ParseResult

    sealed interface Error : ParseResult

    /** Nothing was typed. */
    data object Empty : Error

    /** Not a number, e.g. "abc" or "1,2,3". */
    data object Invalid : Error

    /** A minus sign was typed (M-5). */
    data object Negative : Error

    /** More decimal places than allowed. Refused, never rounded. */
    data class TooManyDecimals(val allowed: Int) : Error

    /** Above the limit for this kind of value (M-6). */
    data object TooLarge : Error

    /** Zero where only a positive value makes sense (quantities). */
    data object NotPositive : Error
}

/**
 * Reads amounts, quantities and percentages as people type them (DATA_MODEL.md section 6.3).
 *
 * - Spaces, currency symbols and codes around the number are ignored.
 * - With both `.` and `,`, the last one is the decimal point.
 * - A single `,` followed by exactly 3 digits is a thousands separator; otherwise it is the decimal point.
 * - A single `.` is always the decimal point.
 * - Extra decimal places are an error unless they are only trailing zeros ("4500.00" in JPY is fine).
 */
object AmountParser {
    private const val MAX_INTEGER_DIGITS = 18

    fun parseAmount(text: String, currency: CurrencyCode): ParseResult =
        parseScaled(text, currency.minorDigits, MoneyLimits.MAX_AMOUNT_MINOR, allowZero = true)

    /** Quantity in thousandths: "1.5" gives 1500. Must be above 0. */
    fun parseQuantity(text: String): ParseResult =
        parseScaled(text, scale = 3, max = MoneyLimits.MAX_QUANTITY_MILLI, allowZero = false)

    /** Percentage in basis points: "7.5" or "7.5%" gives 750. Up to [MoneyLimits.MAX_TAX_BP]. */
    fun parsePercent(text: String): ParseResult =
        parseScaled(text, scale = 2, max = MoneyLimits.MAX_TAX_BP.toLong(), allowZero = true)

    private fun parseScaled(text: String, scale: Int, max: Long, allowZero: Boolean): ParseResult {
        val compact = text.filterNot { it.isWhitespace() }
        if (compact.isEmpty()) return ParseResult.Empty
        if ('-' in compact || '−' in compact) return ParseResult.Negative

        // Drop symbols and codes before and after the number ("€", "USD", "%"), but not inside it.
        val core = compact.trim { !it.isDigit() && it != '.' && it != ',' }
        if (core.none { it.isDigit() } || core.any { !it.isDigit() && it != '.' && it != ',' }) {
            return ParseResult.Invalid
        }

        val (integerDigits, fractionDigits) = splitNumber(core) ?: return ParseResult.Invalid
        val fraction = fractionDigits.trimEnd('0')
        if (fraction.length > scale) return ParseResult.TooManyDecimals(scale)

        val integer = integerDigits.trimStart('0').ifEmpty { "0" }
        if (integer.length > MAX_INTEGER_DIGITS) return ParseResult.TooLarge

        val value = BigInteger(integer) * BigInteger.TEN.pow(scale) +
            BigInteger(fraction.padEnd(scale, '0').ifEmpty { "0" })
        if (value > BigInteger.valueOf(max)) return ParseResult.TooLarge
        if (!allowZero && value.signum() == 0) return ParseResult.NotPositive
        return ParseResult.Ok(value.toLong())
    }

    /** Splits digits-and-separators into (integer digits, fraction digits), or null if the grouping is malformed. */
    private fun splitNumber(s: String): Pair<String, String>? {
        val dots = s.count { it == '.' }
        val commas = s.count { it == ',' }
        if (dots == 0 && commas == 0) return s to ""

        if (dots > 0 && commas > 0) {
            val decimal = if (s.lastIndexOf('.') > s.lastIndexOf(',')) '.' else ','
            val group = if (decimal == '.') ',' else '.'
            if (s.count { it == decimal } != 1) return null
            val integerPart = s.substringBefore(decimal)
            val fraction = s.substringAfter(decimal)
            if (fraction.any { !it.isDigit() }) return null
            return (ungroup(integerPart, group) ?: return null) to fraction
        }

        val separator = if (dots > 0) '.' else ','
        if (maxOf(dots, commas) > 1) return (ungroup(s, separator) ?: return null) to ""

        val left = s.substringBefore(separator)
        val right = s.substringAfter(separator)
        if (separator == ',' && right.length == 3 && left.isNotEmpty()) {
            return (ungroup(s, ',') ?: return null) to ""
        }
        return left to right
    }

    /** "1,200,000" with group ',' gives "1200000"; returns null unless groups are 1–3 digits, then exactly 3. */
    private fun ungroup(s: String, group: Char): String? {
        val parts = s.split(group)
        if (parts.first().length !in 1..3) return null
        if (parts.drop(1).any { it.length != 3 }) return null
        if (parts.any { part -> part.any { !it.isDigit() } }) return null
        return parts.joinToString("")
    }
}
