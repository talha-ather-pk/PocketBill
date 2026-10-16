package com.example.pocketbill.domain.money

/**
 * Currencies PocketBill supports (DATA_MODEL.md section 6.2).
 *
 * Decimal places (ISO 4217) and display symbols (Unicode CLDR, US English) are written out here
 * instead of being read from the phone's Java or ICU data, which differs between Android
 * versions and from the computer the tests run on. That keeps every amount formatted the same
 * in the app, the PDF and the email. The enum name is the ISO code and is what the database
 * stores, so renaming an entry needs a migration.
 */
enum class CurrencyCode(val minorDigits: Int, val symbol: String) {
    // 2 decimal places
    USD(2, "$"), EUR(2, "€"), GBP(2, "£"), CAD(2, "CA$"), AUD(2, "A$"), NZD(2, "NZ$"),
    CHF(2, "CHF"), SEK(2, "SEK"), NOK(2, "NOK"), DKK(2, "DKK"), PLN(2, "PLN"), CZK(2, "CZK"),
    INR(2, "₹"), PKR(2, "PKR"), BDT(2, "BDT"), LKR(2, "LKR"), NPR(2, "NPR"), NGN(2, "NGN"),
    KES(2, "KES"), GHS(2, "GHS"), ZAR(2, "ZAR"), EGP(2, "EGP"), AED(2, "AED"), SAR(2, "SAR"),
    QAR(2, "QAR"), PHP(2, "₱"), IDR(2, "IDR"), MYR(2, "MYR"), SGD(2, "SGD"), HKD(2, "HK$"),
    CNY(2, "CN¥"), THB(2, "THB"), TRY(2, "TRY"), BRL(2, "R$"), MXN(2, "MX$"), ARS(2, "ARS"),
    COP(2, "COP"),

    // No decimal places
    JPY(0, "¥"), KRW(0, "₩"), VND(0, "₫"), CLP(0, "CLP"), UGX(0, "UGX"), ISK(0, "ISK"),

    // 3 decimal places
    KWD(3, "KWD"), BHD(3, "BHD"), OMR(3, "OMR"), JOD(3, "JOD"), TND(3, "TND");

    companion object {
        /** Returns the currency for an ISO 4217 code such as "EUR", or null if it isn't supported. */
        fun fromCode(code: String): CurrencyCode? = entries.firstOrNull { it.name == code }
    }
}
