package com.example.pocketbill.ui.invoices

import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.db.LineDescription
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.data.repository.InvoiceBrowser
import com.example.pocketbill.data.repository.InvoiceSending
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.InvoiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [InvoiceBrowser] and [InvoiceSending] that apply the same rules as the repository (DATA_MODEL 3). */
class FakeInvoiceBrowser : InvoiceBrowser, InvoiceSending {
    var nextNumber = 15
    val invoices = MutableStateFlow<List<InvoiceWithLines>>(emptyList())

    fun put(invoice: InvoiceEntity, vararg lines: LineItemEntity) {
        invoices.value = invoices.value.filterNot { it.invoice.id == invoice.id } + InvoiceWithLines(invoice, lines.toList())
    }

    fun status(id: Long) = invoices.value.first { it.invoice.id == id }.invoice

    override fun observeAll(): Flow<List<InvoiceEntity>> = invoices.map { list -> list.map { it.invoice } }
    override fun observe(id: Long): Flow<InvoiceWithLines?> = invoices.map { list -> list.firstOrNull { it.invoice.id == id } }
    override fun observeLineDescriptions(): Flow<List<LineDescription>> =
        invoices.map { list -> list.flatMap { inv -> inv.unorderedLines.map { LineDescription(inv.invoice.id, it.description) } } }

    private fun change(id: Long, from: InvoiceStatus, update: (InvoiceEntity) -> InvoiceEntity): Outcome<Unit> {
        val current = invoices.value.firstOrNull { it.invoice.id == id } ?: return Outcome.NotFound
        if (current.invoice.status != from) return Outcome.NotAllowed(Problem.READ_ONLY)
        put(update(current.invoice), *current.unorderedLines.toTypedArray())
        return Outcome.Success(Unit)
    }

    override suspend fun markPaid(invoiceId: Long, paidDate: Long) =
        change(invoiceId, InvoiceStatus.ISSUED) { it.copy(status = InvoiceStatus.PAID, paidDate = paidDate) }
    override suspend fun markUnpaid(invoiceId: Long) =
        change(invoiceId, InvoiceStatus.PAID) { it.copy(status = InvoiceStatus.ISSUED, paidDate = null) }
    override suspend fun changePaidDate(invoiceId: Long, paidDate: Long) =
        change(invoiceId, InvoiceStatus.PAID) { it.copy(paidDate = paidDate) }
    override suspend fun void(invoiceId: Long) = change(invoiceId, InvoiceStatus.ISSUED) { it.copy(status = InvoiceStatus.VOID) }
    override suspend fun undoVoid(invoiceId: Long) = change(invoiceId, InvoiceStatus.VOID) { it.copy(status = InvoiceStatus.ISSUED) }

    override suspend fun deleteDraft(invoiceId: Long): Outcome<Unit> {
        val current = invoices.value.firstOrNull { it.invoice.id == invoiceId } ?: return Outcome.NotFound
        if (current.invoice.status != InvoiceStatus.DRAFT) return Outcome.NotAllowed(Problem.NOT_A_DRAFT)
        invoices.value = invoices.value.filterNot { it.invoice.id == invoiceId }
        return Outcome.Success(Unit)
    }

    override suspend fun previewNextNumber(): String = "INV-%04d".format(nextNumber)

    /** Like the repository: a draft needs a client and at least one line to be issued. */
    override suspend fun issue(invoiceId: Long): Outcome<String> {
        val current = invoices.value.firstOrNull { it.invoice.id == invoiceId } ?: return Outcome.NotFound
        if (current.invoice.status != InvoiceStatus.DRAFT) return Outcome.NotAllowed(Problem.NOT_A_DRAFT)
        val problems = buildSet {
            if (current.invoice.clientId == null) add(Problem.NO_CLIENT)
            if (current.unorderedLines.isEmpty()) add(Problem.NO_LINES)
        }
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        val number = previewNextNumber()
        nextNumber++
        put(current.invoice.copy(status = InvoiceStatus.ISSUED, number = number), *current.unorderedLines.toTypedArray())
        return Outcome.Success(number)
    }

    override suspend fun recordSent(invoiceId: Long, via: String, at: Long): Outcome<Unit> {
        val current = invoices.value.firstOrNull { it.invoice.id == invoiceId } ?: return Outcome.NotFound
        if (current.invoice.status != InvoiceStatus.ISSUED && current.invoice.status != InvoiceStatus.PAID) {
            return Outcome.NotAllowed(Problem.READ_ONLY)
        }
        put(current.invoice.copy(lastSentAt = at, lastSentVia = via), *current.unorderedLines.toTypedArray())
        return Outcome.Success(Unit)
    }

    override suspend fun restoreSent(invoiceId: Long, lastSentAt: Long?, lastSentVia: String?): Outcome<Unit> {
        val current = invoices.value.firstOrNull { it.invoice.id == invoiceId } ?: return Outcome.NotFound
        put(current.invoice.copy(lastSentAt = lastSentAt, lastSentVia = lastSentVia), *current.unorderedLines.toTypedArray())
        return Outcome.Success(Unit)
    }
}
