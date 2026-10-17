package com.example.pocketbill.domain.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dates the way UX_SPEC.md section 7.2 sets them: day, short English month, year ("30 Oct 2026"),
 * never all-number dates, whatever the phone's language. Inputs are epoch days (DATA_MODEL 1).
 */
object DateFormatter {
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val short = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** "30 Oct 2026": used on invoices, PDFs and emails. */
    fun full(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(full)

    /** "30 Oct" in the current year, else "30 Oct 2026": used in lists inside the app. */
    fun inList(epochDay: Long, todayEpochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return if (date.year == LocalDate.ofEpochDay(todayEpochDay).year) date.format(short) else date.format(full)
    }
}
