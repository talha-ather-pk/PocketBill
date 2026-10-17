package com.example.pocketbill.ui.invoices

import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.LineDescription
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.text.normalizeForMatching
import com.example.pocketbill.ui.clients.Owed
import com.example.pocketbill.ui.components.DisplayStatus
import com.example.pocketbill.ui.components.displayStatus
import com.example.pocketbill.ui.navigation.InvoiceFilter
import java.time.LocalDate

/** One invoice row (UX_SPEC 5 row design). */
data class InvoiceRow(
    val id: Long,
    val number: String?,
    /** Frozen name for issued invoices (what the client sees), live name for drafts (DATA_MODEL 5.6). */
    val clientName: String,
    val status: DisplayStatus,
    val currency: String,
    val totalMinor: Long,
    val issueDate: Long,
    val dueDate: Long,
    val paidDate: Long?,
    val lastSentAt: Long?,
    val updatedAt: Long,
)

/** A month heading ("October 2026") followed by its rows, for the All and Paid filters. */
data class InvoiceGroup(val yearMonth: Pair<Int, Int>?, val rows: List<InvoiceRow>)

data class InvoiceListResult(
    val groups: List<InvoiceGroup>,
    /** Count per filter, for the chip labels ("Unpaid 4"). */
    val counts: Map<InvoiceFilter, Int>,
    /** Sum of the listed invoices per currency, largest first; empty for All (mixed statuses). */
    val totals: List<Owed>,
    /** True when a search is active: it covers all invoices, whatever the filter (UX_SPEC 5). */
    val searching: Boolean,
)

/** Which client name to show and search: frozen for issued invoices, current otherwise. */
fun clientNameFor(invoice: InvoiceEntity, clientsById: Map<Long, ClientEntity>): String {
    val frozen = invoice.toSnapshot?.let { SnapshotJson.decodeTo(it)?.name }
    return frozen ?: invoice.clientId?.let { clientsById[it]?.name }.orEmpty()
}

fun InvoiceEntity.matchesFilter(filter: InvoiceFilter, today: Long): Boolean {
    val shown = displayStatus(status, dueDate, today)
    return when (filter) {
        InvoiceFilter.ALL -> true
        InvoiceFilter.DRAFTS -> shown == DisplayStatus.DRAFT
        // Unpaid includes overdue: both are still owed.
        InvoiceFilter.UNPAID -> shown == DisplayStatus.UNPAID || shown == DisplayStatus.OVERDUE
        InvoiceFilter.OVERDUE -> shown == DisplayStatus.OVERDUE
        InvoiceFilter.PAID -> shown == DisplayStatus.PAID
        InvoiceFilter.VOID -> shown == DisplayStatus.VOID
    }
}

/**
 * Builds the Invoices tab (UX_SPEC 5): filter, search (number, client name current and frozen,
 * item descriptions; case- and accent-insensitive, across all invoices), sort per filter, month
 * groups for All and Paid, and totals per currency.
 */
fun buildInvoiceList(
    invoices: List<InvoiceEntity>,
    clients: List<ClientEntity>,
    lines: List<LineDescription>,
    filter: InvoiceFilter,
    query: String,
    today: Long,
): InvoiceListResult {
    val clientsById = clients.associateBy { it.id }
    val counts = InvoiceFilter.entries.associateWith { f -> invoices.count { it.matchesFilter(f, today) } }
    val wanted = normalizeForMatching(query)
    val searching = wanted.isNotEmpty()

    val selected = if (searching) {
        val descriptions = lines.groupBy({ it.invoiceId }, { normalizeForMatching(it.description) })
        invoices.filter { inv ->
            val names = listOfNotNull(
                inv.number,
                clientNameFor(inv, clientsById),
                inv.clientId?.let { clientsById[it]?.name },
            ).map(::normalizeForMatching)
            names.any { it.contains(wanted) } || descriptions[inv.id].orEmpty().any { it.contains(wanted) }
        }
    } else {
        invoices.filter { it.matchesFilter(filter, today) }
    }

    val effective = if (searching) InvoiceFilter.ALL else filter
    val sorted = when (effective) {
        InvoiceFilter.UNPAID, InvoiceFilter.OVERDUE -> selected.sortedWith(compareBy<InvoiceEntity> { it.dueDate }.thenBy { it.id })
        InvoiceFilter.PAID -> selected.sortedWith(compareByDescending<InvoiceEntity> { it.paidDate ?: 0 }.thenByDescending { it.id })
        InvoiceFilter.DRAFTS -> selected.sortedWith(compareByDescending<InvoiceEntity> { it.updatedAt }.thenByDescending { it.id })
        InvoiceFilter.ALL, InvoiceFilter.VOID -> selected.sortedWith(compareByDescending<InvoiceEntity> { it.issueDate }.thenByDescending { it.id })
    }
    val rows = sorted.map { inv ->
        InvoiceRow(
            id = inv.id, number = inv.number, clientName = clientNameFor(inv, clientsById),
            status = displayStatus(inv.status, inv.dueDate, today), currency = inv.currency, totalMinor = inv.totalMinor,
            issueDate = inv.issueDate, dueDate = inv.dueDate, paidDate = inv.paidDate, lastSentAt = inv.lastSentAt, updatedAt = inv.updatedAt,
        )
    }

    val groups = when (effective) {
        InvoiceFilter.ALL -> groupByMonth(rows) { it.issueDate }
        InvoiceFilter.PAID -> groupByMonth(rows) { it.paidDate ?: it.issueDate }
        else -> if (rows.isEmpty()) emptyList() else listOf(InvoiceGroup(null, rows))
    }
    val totals = if (effective == InvoiceFilter.ALL) {
        emptyList()
    } else {
        rows.groupBy { it.currency }.map { (currency, list) -> Owed(currency, list.sumOf { it.totalMinor }) }.sortedByDescending { it.totalMinor }
    }
    return InvoiceListResult(groups, counts, totals, searching)
}

private fun groupByMonth(rows: List<InvoiceRow>, date: (InvoiceRow) -> Long): List<InvoiceGroup> =
    rows.groupBy { LocalDate.ofEpochDay(date(it)).let { d -> d.year to d.monthValue } }
        .map { (month, list) -> InvoiceGroup(month, list) }

/** Drafts don't count as issued, so their numbers and status are shown as "Draft". */
val InvoiceRow.isDraft: Boolean get() = status == DisplayStatus.DRAFT

/** For the row's second line (UX_SPEC 5): which date to show. */
fun InvoiceRow.datePart(): Pair<DateKind, Long> = when (status) {
    DisplayStatus.PAID -> DateKind.PAID to (paidDate ?: issueDate)
    DisplayStatus.OVERDUE -> DateKind.OVERDUE to dueDate
    // updatedAt is a timestamp (ms); shown as the day it was last edited.
    DisplayStatus.DRAFT -> DateKind.EDITED to updatedAt / 86_400_000L
    DisplayStatus.VOID -> DateKind.ISSUED to issueDate
    DisplayStatus.UNPAID -> DateKind.DUE to dueDate
}

enum class DateKind { DUE, OVERDUE, PAID, EDITED, ISSUED }
