package com.example.pocketbill.data.export

import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import java.time.LocalDate

/**
 * Invoices as a spreadsheet for an accountant (F-19). One row per issued, paid or void invoice
 * (drafts aren't invoices yet), oldest first. Dates are ISO (2026-10-30) and amounts plain
 * numbers (1200.50) so every spreadsheet reads them, whatever its language. Client names come
 * from the issued invoice, as on the PDF.
 */
object InvoiceCsv {
    val HEADER = listOf(
        "Number", "Status", "Client", "Client email", "Issue date", "Due date", "Paid date",
        "Currency", "Subtotal", "Discount", "Tax", "Total", "Last sent",
    )

    /** "PocketBill-invoices-2026-10-17.csv" */
    fun fileName(date: LocalDate): String = "PocketBill-invoices-$date.csv"

    fun build(invoices: List<InvoiceEntity>, clients: List<ClientEntity>): String {
        val byId = clients.associateBy { it.id }
        val rows = invoices
            .filter { it.status != InvoiceStatus.DRAFT }
            .sortedWith(compareBy<InvoiceEntity> { it.issueDate }.thenBy { it.id })
            .map { inv ->
                val frozen = inv.toSnapshot?.let(SnapshotJson::decodeTo)
                val live = inv.clientId?.let(byId::get)
                val currency = CurrencyCode.fromCode(inv.currency)
                fun money(minor: Long) = currency?.let { MoneyFormatter.formatPlain(minor, it) } ?: minor.toString()
                listOf(
                    inv.number.orEmpty(),
                    status(inv.status),
                    frozen?.name ?: live?.name.orEmpty(),
                    frozen?.email ?: live?.email.orEmpty(),
                    iso(inv.issueDate),
                    iso(inv.dueDate),
                    inv.paidDate?.let(::iso).orEmpty(),
                    inv.currency,
                    money(inv.subtotalMinor),
                    money(inv.discountMinor),
                    money(inv.taxMinor),
                    money(inv.totalMinor),
                    inv.lastSentAt?.let { iso(it / DAY_MS) }.orEmpty(),
                )
            }
        // A byte-order mark makes Excel read accents (Müller, José) correctly.
        return BOM + (listOf(HEADER) + rows).joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",") { cell(it) } }
    }

    private fun status(s: InvoiceStatus) = when (s) {
        InvoiceStatus.ISSUED -> "Unpaid"
        InvoiceStatus.PAID -> "Paid"
        InvoiceStatus.VOID -> "Void"
        InvoiceStatus.DRAFT -> "Draft"
    }

    private fun iso(epochDay: Long) = LocalDate.ofEpochDay(epochDay).toString()

    /**
     * RFC 4180 quoting, plus a leading apostrophe on text that starts like a formula
     * (=, +, -, @) so a spreadsheet never runs a client name as a formula (CSV injection).
     */
    private fun cell(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r" && value.toDoubleOrNull() == null) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private const val BOM = "\uFEFF"
    private const val DAY_MS = 86_400_000L
}
