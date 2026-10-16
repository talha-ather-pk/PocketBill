package com.example.pocketbill.domain.money

/**
 * Formats money the same way everywhere (UX_SPEC.md section 7.2): English digit grouping and
 * decimal point, the currency's own decimal places, and the symbol from [CurrencyCode].
 *
 * It deliberately avoids java.text.NumberFormat: Android (ICU) and the desktop JVM format some
 * currencies differently (spacing after codes, symbol choice), and the phone's locale must never
 * change how an invoice looks.
 */
object MoneyFormatter {
    /** Separates a letter code from the number and keeps them on one line in PDFs and emails. */
    private const val NO_BREAK_SPACE = ' '

    /** "€1,200.00", "¥4,500", "CA$1,200.00", "KWD 2.500" (the space is a no-break space). */
    fun format(minor: Long, currency: CurrencyCode): String {
        require(minor >= 0) { "Amount must not be negative: $minor" }
        val symbol = currency.symbol
        val separator = if (symbol.last().isLetter()) NO_BREAK_SPACE.toString() else ""
        return symbol + separator + groupedNumber(minor, currency.minorDigits)
    }

    fun format(money: Money): String = format(money.minor, money.currency)

    /** Plain number for an input field, with no symbol or grouping: "1200.50", "4500". */
    fun formatPlain(minor: Long, currency: CurrencyCode): String {
        require(minor >= 0) { "Amount must not be negative: $minor" }
        val (whole, fraction) = split(minor, currency.minorDigits)
        return if (fraction.isEmpty()) whole else "$whole.$fraction"
    }

    private fun groupedNumber(minor: Long, digits: Int): String {
        val (whole, fraction) = split(minor, digits)
        val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
        return if (fraction.isEmpty()) grouped else "$grouped.$fraction"
    }

    /** 120050 with 2 digits gives ("1200", "50"). */
    private fun split(minor: Long, digits: Int): Pair<String, String> {
        if (digits == 0) return minor.toString() to ""
        val text = minor.toString().padStart(digits + 1, '0')
        return text.dropLast(digits) to text.takeLast(digits)
    }
}
