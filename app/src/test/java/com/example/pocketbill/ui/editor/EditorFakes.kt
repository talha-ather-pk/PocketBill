package com.example.pocketbill.ui.editor

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.data.repository.InvoiceInput
import com.example.pocketbill.data.repository.InvoiceStore
import com.example.pocketbill.data.repository.PresetStore
import com.example.pocketbill.data.repository.ProfileStore
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.InvoiceNumbering
import com.example.pocketbill.domain.invoice.InvoiceStatus

/** In-memory [InvoiceStore] for editor ViewModel tests. Numbering follows N-2 with a simple counter. */
class FakeInvoiceStore : InvoiceStore {
    val invoices = mutableMapOf<Long, InvoiceWithLines>()
    var saveDraftCalls = 0
    var updateIssuedCalls = 0
    var issueCalls = 0
    var nextSequence = 1L
    private var nextId = 100L

    fun put(invoice: InvoiceEntity, vararg lines: LineItemEntity) {
        invoices[invoice.id] = InvoiceWithLines(invoice, lines.toList())
    }

    override suspend fun get(id: Long): InvoiceWithLines? = invoices[id]

    override suspend fun saveDraft(invoiceId: Long, input: InvoiceInput): Outcome<Long> {
        saveDraftCalls++
        val id = if (invoiceId == 0L) nextId++ else invoiceId
        val existing = invoices[id]?.invoice
        if (existing != null && existing.status != InvoiceStatus.DRAFT) return Outcome.NotAllowed(Problem.NOT_A_DRAFT)
        invoices[id] = InvoiceWithLines(toEntity(id, input, InvoiceStatus.DRAFT, number = null), toLines(id, input))
        return Outcome.Success(id)
    }

    override suspend fun deleteDraft(invoiceId: Long): Outcome<Unit> {
        invoices.remove(invoiceId)
        return Outcome.Success(Unit)
    }

    override suspend fun issue(invoiceId: Long): Outcome<String> {
        issueCalls++
        val draft = invoices[invoiceId] ?: return Outcome.NotFound
        if (draft.invoice.clientId == null) return Outcome.ValidationFailed(setOf(Problem.NO_CLIENT))
        val number = InvoiceNumbering.format("INV-", nextSequence++)
        invoices[invoiceId] = draft.copy(invoice = draft.invoice.copy(status = InvoiceStatus.ISSUED, number = number))
        return Outcome.Success(number)
    }

    override suspend fun updateIssued(invoiceId: Long, input: InvoiceInput): Outcome<Unit> {
        updateIssuedCalls++
        val existing = invoices[invoiceId] ?: return Outcome.NotFound
        if (existing.invoice.status != InvoiceStatus.ISSUED) return Outcome.NotAllowed(Problem.READ_ONLY)
        invoices[invoiceId] = InvoiceWithLines(toEntity(invoiceId, input, InvoiceStatus.ISSUED, existing.invoice.number), toLines(invoiceId, input))
        return Outcome.Success(Unit)
    }

    override suspend fun lastInvoiceForClient(clientId: Long): InvoiceEntity? =
        invoices.values.map { it.invoice }.filter { it.clientId == clientId }.maxByOrNull { it.issueDate }

    override suspend fun previewNextNumber(): String = InvoiceNumbering.format("INV-", nextSequence)

    private fun toEntity(id: Long, input: InvoiceInput, status: InvoiceStatus, number: String?) = InvoiceEntity(
        id = id, uuid = "i$id", number = number, status = status, clientId = input.clientId, currency = input.currency,
        issueDate = input.issueDate, dueDate = input.dueDate, taxRateBp = input.taxRateBp, notes = input.notes,
        createdAt = 0, updatedAt = 0,
    )

    private fun toLines(id: Long, input: InvoiceInput) = input.lines.mapIndexed { i, l ->
        LineItemEntity(
            id = id * 100 + i, uuid = l.uuid ?: "l$i", invoiceId = id, position = i, description = l.description,
            quantityMilli = l.quantityMilli, unitPriceMinor = l.unitPriceMinor, amountMinor = 0, createdAt = 0, updatedAt = 0,
        )
    }
}

class FakeProfileStore(var profile: BusinessProfileEntity?) : ProfileStore {
    override suspend fun get(): BusinessProfileEntity? = profile
}

class FakePresetStore : PresetStore {
    val presets = mutableListOf<ItemPresetEntity>()
    val used = mutableListOf<Long>()
    val saved = mutableListOf<Triple<String, Long?, String>>()

    override suspend fun suggestions(prefix: String): List<ItemPresetEntity> =
        if (prefix.trim().length < 2) emptyList() else presets.filter { it.description.startsWith(prefix.trim(), ignoreCase = true) }.take(3)

    override suspend fun saveFromLine(description: String, unitPriceMinor: Long?, currency: String) {
        saved += Triple(description, unitPriceMinor, currency)
    }

    override suspend fun markUsed(presetId: Long) {
        used += presetId
    }
}
