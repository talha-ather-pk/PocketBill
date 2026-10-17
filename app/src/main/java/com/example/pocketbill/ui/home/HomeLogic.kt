package com.example.pocketbill.ui.home

import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.isOverdue
import com.example.pocketbill.ui.clients.Owed
import com.example.pocketbill.ui.invoices.InvoiceRow
import com.example.pocketbill.ui.invoices.toRow
import com.example.pocketbill.ui.navigation.InvoiceFilter

/** One currency line of the Outstanding card, never added to another currency (UX_SPEC 2). */
data class OutstandingLine(val currency: String, val totalMinor: Long, val count: Int)

/** The three kinds of "Needs attention" rows, most urgent first (UX_SPEC 2). */
enum class AttentionKind { OVERDUE, UNSENT, DRAFT }

data class AttentionItem(
    val kind: AttentionKind,
    val row: InvoiceRow,
    /** For "Overdue 8 days". */
    val daysOverdue: Long = 0,
    /** Decides Remind/Email (email app) versus Send… (share sheet). */
    val hasEmail: Boolean = false,
)

data class HomeSummary(
    val outstanding: List<OutstandingLine> = emptyList(),
    val unpaidCount: Int = 0,
    val overdueCount: Int = 0,
    val overdueTotals: List<Owed> = emptyList(),
    /** Up to [MAX_ATTENTION] rows. */
    val attention: List<AttentionItem> = emptyList(),
    /** How many rows qualify in all; "See all (5)" when more than shown. */
    val attentionTotal: Int = 0,
    /** Where "See all" goes: the filter of the most urgent kind present. */
    val attentionFilter: InvoiceFilter = InvoiceFilter.ALL,
    val recent: List<InvoiceRow> = emptyList(),
    /** First use: no invoices at all. */
    val noInvoices: Boolean = true,
    /** Nothing owed and at least one paid invoice: "All invoices are paid." */
    val allPaid: Boolean = false,
) {
    companion object {
        const val MAX_ATTENTION = 3
        const val MAX_RECENT = 5
    }
}

/**
 * Builds Home (UX_SPEC 2): outstanding per currency (most unpaid invoices first, then by code),
 * the overdue row, "Needs attention" (overdue oldest-due first, then issued-but-unsent, then
 * drafts most recently edited) and the 5 most recently changed invoices.
 */
fun buildHome(invoices: List<InvoiceEntity>, clients: List<ClientEntity>, today: Long): HomeSummary {
    val clientsById = clients.associateBy { it.id }
    val unpaid = invoices.filter { it.status == InvoiceStatus.ISSUED }
    val overdue = unpaid.filter { isOverdue(it.status, it.dueDate, today) }

    val outstanding = unpaid.groupBy { it.currency }
        .map { (currency, list) -> OutstandingLine(currency, list.sumOf { it.totalMinor }, list.size) }
        .sortedWith(compareByDescending<OutstandingLine> { it.count }.thenBy { it.currency })
    val overdueTotals = overdue.groupBy { it.currency }
        .map { (currency, list) -> Owed(currency, list.sumOf { it.totalMinor }) }
        .sortedByDescending { it.totalMinor }

    fun hasEmail(inv: InvoiceEntity): Boolean {
        val live = inv.clientId?.let { clientsById[it]?.email }
        val frozen = inv.toSnapshot?.let(SnapshotJson::decodeTo)?.email
        return !live.isNullOrBlank() || !frozen.isNullOrBlank()
    }

    val attentionAll = buildList {
        overdue.sortedWith(compareBy<InvoiceEntity> { it.dueDate }.thenBy { it.id }).forEach {
            add(AttentionItem(AttentionKind.OVERDUE, it.toRow(clientsById, today), daysOverdue = today - it.dueDate, hasEmail = hasEmail(it)))
        }
        unpaid.filter { it.lastSentAt == null && it !in overdue }.sortedWith(compareBy<InvoiceEntity> { it.issueDate }.thenBy { it.id }).forEach {
            add(AttentionItem(AttentionKind.UNSENT, it.toRow(clientsById, today), hasEmail = hasEmail(it)))
        }
        invoices.filter { it.status == InvoiceStatus.DRAFT }.sortedByDescending { it.updatedAt }.forEach {
            add(AttentionItem(AttentionKind.DRAFT, it.toRow(clientsById, today)))
        }
    }
    val attentionFilter = when (attentionAll.firstOrNull()?.kind) {
        AttentionKind.OVERDUE -> InvoiceFilter.OVERDUE
        AttentionKind.UNSENT -> InvoiceFilter.UNPAID
        AttentionKind.DRAFT -> InvoiceFilter.DRAFTS
        null -> InvoiceFilter.ALL
    }

    return HomeSummary(
        outstanding = outstanding,
        unpaidCount = unpaid.size,
        overdueCount = overdue.size,
        overdueTotals = overdueTotals,
        attention = attentionAll.take(HomeSummary.MAX_ATTENTION),
        attentionTotal = attentionAll.size,
        attentionFilter = attentionFilter,
        recent = invoices.sortedWith(compareByDescending<InvoiceEntity> { it.updatedAt }.thenByDescending { it.id })
            .take(HomeSummary.MAX_RECENT)
            .map { it.toRow(clientsById, today) },
        noInvoices = invoices.isEmpty(),
        allPaid = unpaid.isEmpty() && invoices.any { it.status == InvoiceStatus.PAID },
    )
}
